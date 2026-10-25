#!/usr/bin/env python3
"""Machine-translate the missing keys of every locale, then write the XML.

Why this exists
---------------
`values/strings.xml` (English) is the source of truth with ~1155 keys. Twelve
locales only had the 123 "core UI" keys and fell back to English for the rest.
Translating a thousand strings by hand across a dozen languages is not a sane
afternoon, so this script fills the gaps with a machine translation and keeps
the result reviewable:

  * Android format specifiers (%1$s, %2$d, %1$04X, %%) are swapped for [[n]]
    tokens before the request and restored afterwards, so they never move or
    get translated. `check_coverage.py` still verifies them.
  * Newlines, apostrophes, quotes and XML entities are unescaped for the
    translator and re-escaped for aapt2 (`'` -> `\\'`, `&` -> `&amp;`…).
  * Results are cached in `tools/i18n/.mt_cache.json`, so a re-run only asks
    for keys it has never seen. Delete the cache to force a retranslation.
  * Existing translations are never overwritten: only missing keys are added.

Usage (from the repository root):
    python tools/i18n/machine_translate.py                # every locale
    python tools/i18n/machine_translate.py de it pt       # a few locales
    python tools/i18n/machine_translate.py --limit 20 de  # smoke test

The endpoint is the public Google Translate web endpoint (no API key). It is
fine for a one-off fill-in; the cache keeps the request count down. Treat the
output as a first pass and review strings that matter.
"""
from __future__ import annotations

import json
import random
import re
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
CACHE_PATH = Path(__file__).resolve().parent / ".mt_cache.json"

# Google uses different codes than Android for a couple of languages.
GOOGLE_CODE = {"zh": "zh-CN", "pt": "pt", "he": "iw", "jv": "jw"}

FORMAT_RE = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
ENTITY_RE = re.compile(r"&(#\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);")

WORKERS = 3
BATCH = 20
TIMEOUT = 30
MIN_INTERVAL = 0.30  # seconds between requests, per process

# Keys the translator must not touch: either they carry no prose, or the value
# has to match the locale's country rather than its language.
OVERRIDES: dict[str, dict[str, str]] = {
    "nfc_phone_placeholder": {
        "en": "+1 …", "ru": "+7 …", "fr": "+33 …", "es": "+34 …", "de": "+49 …",
        "it": "+39 …", "pt": "+351 …", "tr": "+90 …", "uk": "+380 …",
        "pl": "+48 …", "ar": "+20 …", "hi": "+91 …", "id": "+62 …",
        "zh": "+86 …", "ja": "+81 …", "ko": "+82 …",
    },
}


# --------------------------------------------------------------------------- #
# XML <-> plain text
# --------------------------------------------------------------------------- #
def parse(path: Path) -> dict[str, str]:
    """Ordered key -> raw body, exactly as stored in the XML."""
    tree = ET.parse(path)
    out: dict[str, str] = {}
    for node in tree.getroot():
        if node.tag == "string":
            out[node.get("name", "")] = node.text or ""
    return out


def unescape(raw: str) -> str:
    """Turn an Android resource body into plain text for the translator."""
    s = raw.replace("\\n", "\n").replace("\\t", "\t")
    s = s.replace("\\'", "'").replace('\\"', '"')
    s = s.replace("\\@", "@").replace("\\?", "?")

    def entity(m: re.Match[str]) -> str:
        token = m.group(1)
        if token.startswith("#x") or token.startswith("#X"):
            return chr(int(token[2:], 16))
        if token.startswith("#"):
            return chr(int(token[1:]))
        return {"amp": "&", "lt": "<", "gt": ">", "quot": '"', "apos": "'"}.get(token, m.group(0))

    return ENTITY_RE.sub(entity, s)


def escape(text: str) -> str:
    """Turn plain text back into something aapt2 accepts verbatim."""
    s = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("'", "\\'").replace('"', '\\"')
    s = s.replace("\n", "\\n").replace("\t", "\\t")
    if s.startswith(("?", "@")):
        s = "\\" + s
    return s


def protect(text: str) -> tuple[str, list[str]]:
    tokens: list[str] = []

    def repl(m: re.Match[str]) -> str:
        tokens.append(m.group(0))
        return f"[[{len(tokens) - 1}]]"

    return FORMAT_RE.sub(repl, text), tokens


def restore(text: str, tokens: list[str]) -> str:
    for index, token in enumerate(tokens):
        # The translator sometimes spaces the brackets out.
        pattern = re.compile(r"\[\s*\[\s*" + str(index) + r"\s*\]\s*\]")
        text = pattern.sub(lambda _m, t=token: t, text)
    return text


# --------------------------------------------------------------------------- #
# Translation
# --------------------------------------------------------------------------- #
class Translator:
    def __init__(self) -> None:
        self.cache: dict[str, dict[str, str]] = {}
        if CACHE_PATH.exists():
            try:
                self.cache = json.loads(CACHE_PATH.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                self.cache = {}
        self._lock = threading.Lock()
        self._rate_lock = threading.Lock()
        self._last_request = 0.0
        self.requests = 0

    def _throttle(self) -> None:
        """Keep the request rate low enough that Google does not answer 429."""
        with self._rate_lock:
            wait = MIN_INTERVAL - (time.monotonic() - self._last_request)
            if wait > 0:
                time.sleep(wait)
            self._last_request = time.monotonic()

    def _fetch_batch(self, texts: list[str], lang: str) -> list[str]:
        """Translate several strings in one request; the endpoint keeps the order.

        `clients5.google.com` accepts repeated `q` parameters and answers with a
        flat JSON array, which is far kinder to the quota than one call per
        string. If the response shape does not match, the batch is halved until
        single strings can be checked one by one.
        """
        code = GOOGLE_CODE.get(lang, lang)
        query = "&".join("q=" + urllib.parse.quote(t) for t in texts)
        url = (
            "https://clients5.google.com/translate_a/t"
            "?client=dict-chrome-ex&sl=en&dt=t&tl=" + urllib.parse.quote(code)
            + "&" + query
        )
        last: Exception | None = None
        for attempt in range(5):
            self._throttle()
            try:
                with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
                    payload = json.loads(response.read().decode("utf-8"))
                with self._lock:
                    self.requests += 1
                if isinstance(payload, str):
                    payload = [payload]
                if isinstance(payload, list) and len(payload) == len(texts):
                    out = []
                    for item in payload:
                        if isinstance(item, list):
                            out.append("".join(str(part) for part in item))
                        else:
                            out.append(str(item))
                    return out
                if len(texts) == 1:
                    raise RuntimeError(f"unexpected payload for one string: {payload!r}")
                middle = len(texts) // 2
                return (self._fetch_batch(texts[:middle], lang)
                        + self._fetch_batch(texts[middle:], lang))
            except urllib.error.HTTPError as exc:
                last = exc
                if exc.code == 429:
                    time.sleep(15 * (attempt + 1) + random.random() * 5)
                else:
                    time.sleep(2 ** attempt + random.random())
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
                last = exc
                time.sleep(2 ** attempt + random.random())
        raise RuntimeError(f"translation failed for {lang}: {last}")

    def translate_many(self, texts: list[str], lang: str) -> None:
        """Translate and cache a list of plain strings, skipping known ones."""
        cache = self.cache.setdefault(lang, {})
        pending = [t for t in dict.fromkeys(texts) if t not in cache]
        for start in range(0, len(pending), BATCH):
            chunk = pending[start:start + BATCH]
            guarded, tokens_per_item = [], []
            for text in chunk:
                guarded_text, tokens = protect(text)
                guarded.append(guarded_text)
                tokens_per_item.append(tokens)
            results = self._fetch_batch(guarded, lang)
            with self._lock:
                for text, result, tokens in zip(chunk, results, tokens_per_item):
                    cache[text] = restore(result, tokens)

    def save(self) -> None:
        tmp = CACHE_PATH.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.cache, ensure_ascii=False, indent=0), encoding="utf-8")
        tmp.replace(CACHE_PATH)


def worth_translating(text: str) -> bool:
    stripped = text.strip()
    if len(stripped) < 2:
        return False
    # Only placeholders / punctuation / numbers: nothing to translate.
    if not re.search(r"[A-Za-z]", stripped):
        return False
    return True


def write_locale(tag: str, source: dict[str, str], existing: dict[str, str],
                 translated: dict[str, str]) -> int:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- {tag}: filled by tools/i18n/machine_translate.py (Google Translate).",
        "     Machine first pass — re-run after editing values/strings.xml. -->",
        "<resources>",
    ]
    for key, raw in source.items():
        if key in existing:
            value = existing[key]
        elif key in translated:
            value = escape(translated[key])
        elif key in OVERRIDES and tag in OVERRIDES[key]:
            value = escape(OVERRIDES[key][tag])
        elif not worth_translating(unescape(raw)):
            value = raw  # numbers, symbols: identical in every language
        else:
            continue  # untranslatable: English fallback
        lines.append(f'    <string name="{key}">{value}</string>')
    lines.append("</resources>")
    lines.append("")

    target = RES / f"values-{tag}" / "strings.xml"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(lines), encoding="utf-8")
    return len(lines) - 6


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #
def main(argv: list[str]) -> int:
    limit = 0
    tags: list[str] = []
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg == "--limit":
            index += 1
            limit = int(argv[index])
        elif not arg.startswith("-"):
            tags.append(arg)
        index += 1

    source = parse(RES / "values/strings.xml")
    if not source:
        print("no English strings found", file=sys.stderr)
        return 1

    folders = sorted(p for p in RES.glob("values-*") if (p / "strings.xml").exists())
    available = [p.name.removeprefix("values-") for p in folders]
    if tags:
        unknown = [t for t in tags if t not in available]
        if unknown:
            print(f"unknown locale(s): {', '.join(unknown)}", file=sys.stderr)
            return 1
    else:
        tags = available

    translator = Translator()
    print(f"source: {len(source)} English keys · {len(tags)} locale(s)")

    for tag in tags:
        path = RES / f"values-{tag}" / "strings.xml"
        existing = {k: v for k, v in parse(path).items() if k in source}
        missing = [k for k in source if k not in existing]
        todo = [k for k in missing if worth_translating(unescape(source[k]))]
        if limit:
            todo = todo[:limit]

        done = 0
        if todo:
            plain = [unescape(source[key]) for key in todo]
            batches = [plain[i:i + BATCH] for i in range(0, len(plain), BATCH)]
            with ThreadPoolExecutor(max_workers=WORKERS) as pool:
                futures = {
                    pool.submit(translator.translate_many, batch, tag): batch
                    for batch in batches
                }
                for future in as_completed(futures):
                    batch = futures[future]
                    try:
                        future.result()
                        done += len(batch)
                    except Exception as exc:  # keep going, these fall back
                        print(f"  ! {tag}: batch of {len(batch)} failed: {exc}")
                    translator.save()
                    print(f"  {tag}: {done}/{len(todo)}")
            translator.save()

        cache_for_tag = translator.cache.get(tag, {})
        translated = {
            key: cache_for_tag[plain]
            for key in source
            if (plain := unescape(source[key])) in cache_for_tag
        }
        written = write_locale(tag, source, existing, translated)
        print(f"{tag}: {len(existing)} kept + {len(todo)} translated = {written} keys")

    print(f"requests this run: {translator.requests}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
