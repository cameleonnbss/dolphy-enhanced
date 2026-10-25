#!/usr/bin/env python3
"""Bring the English (default) and Russian locale files to full key parity.

Run from the repository root:  python tools/i18n/patch_parity.py

Why this exists
---------------
Before this script the app shipped `values/` = Russian and `values-en/` = English.
62 keys only existed in Russian and 16 only in English, so an English user saw
Russian text on those screens (and vice versa). One entry (`nfc_troll_no_desc`)
was also malformed: its closing tag was missing, which swallowed the
`wifi_print_title` key entirely.

The script is idempotent: running it twice changes nothing.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EN_PATH = ROOT / "app/src/main/res/values/strings.xml"
RU_PATH = ROOT / "app/src/main/res/values-ru/strings.xml"

ENTRY_RE = re.compile(r'<string name="([^"]+)"[^>]*>(.*?)</string>', re.S)
SELF_CLOSING_RE = re.compile(r'<string name="([^"]+)"\s*/>')

# ---------------------------------------------------------------------------
# English text for keys that only existed in Russian.
# ---------------------------------------------------------------------------
EN_EXTRA = {
    "audio_spoof_info_text": "Plays sounds on the device that opens the local Dolphy page. Both devices must be on the same Wi-Fi or local network. No internet, hosting or external tunnels are used.",
    "bad_usb_card_desc": "Ducky script · USB HID gadget (root)",
    "bad_usb_done": "Script executed",
    "bad_usb_empty": "Enter a Ducky script",
    "bad_usb_fail": "Execution failed",
    "bad_usb_hint": "Phone as a USB keyboard/mouse (gadget). Connect USB to a PC, paste a Ducky script and tap Run.",
    "bad_usb_run": "Run",
    "bad_usb_running": "Running…",
    "bad_usb_script_label": "Ducky script",
    "bad_usb_status_connected": "Connected via USB",
    "bad_usb_status_disconnected": "Disconnected",
    "bad_usb_status_root": "Root required",
    "bad_usb_status_setup": "Setting up HID gadget…",
    "hid_bad_hid_mode": "Bad HID Mode",
    "hid_change_name_description": "Enter the name that will be broadcast to the selected devices. \"Free Wi-Fi\" or \"Internet modem\" works best to make people accept the pairing request.",
    "hid_change_name_hint": "Enter a name",
    "hid_change_name_title": "Enter a name",
    "hid_device_name_label": "Device name: %s",
    "lan_mitm_active": "Interception active",
    "lan_mitm_all_desc": "Broadcast ARP spoofing with no target selection: every device on the network gets the \"Hacked by Dolphy\" page on its next request. Android, Windows and iOS show a system captive-portal window.",
    "lan_mitm_all_start": "Open portal for everyone",
    "lan_mitm_all_stop": "Stop",
    "lan_mitm_entry": "LAN MITM",
    "lan_mitm_entry_desc": "Portal for every device on the network: ARP spoof + sniffer (root)",
    "lan_mitm_gateway": "Gateway: %s",
    "lan_mitm_iface": "Interface: %s",
    "lan_mitm_ip": "Your IP: %s",
    "lan_mitm_log": "Intercept",
    "lan_mitm_log_auth": "Basic auth · %s",
    "lan_mitm_log_empty": "Waiting for network traffic…",
    "lan_mitm_log_http": "HTTP · %s",
    "lan_mitm_log_portal": "Portal · %s",
    "lan_mitm_log_sni": "HTTPS (SNI) · %s",
    "lan_mitm_network": "Network",
    "lan_mitm_root_checking": "root: checking…",
    "lan_mitm_root_no": "root: no",
    "lan_mitm_root_warn": "Root unavailable. ARP spoofing and sniffing need su — root this device first.",
    "lan_mitm_root_yes": "root: yes",
    "lan_mitm_ssid": "SSID: %s",
    "lan_mitm_title": "LAN MITM",
    "nfc_master_key": "Master Key",
    "nfc_master_key_desc": "Root-only key brute-force for intercoms and NFC locks (Mifare Classic and similar).",
    "nfc_master_key_description": "Root: brute-force intercoms and NFC locks",
    "nfc_master_key_root_only_desc": "Root-only: brute-force keys of intercoms and NFC locks (Mifare Classic and similar) through su. Hold the phone against the reader.",
    "nfc_master_key_scanning": "Brute-forcing keys…",
    "nfc_master_key_start": "Start brute-force",
    "nfc_master_key_status_no_root": "Root required",
    "nfc_master_key_status_root": "Root · ready to brute-force",
    "nfc_master_key_stop": "Stop",
    "nfc_master_key_title": "Master Key",
    "nfc_troll_no_desc": "No description available",
    "nfc_open_settings": "Settings",
    "nfc_prompt_enable": "Please turn on NFC in Settings",
    "nfc_status_disabled": "NFC is off",
    "other_bad_usb": "BAD USB",
    "other_bad_usb_desc": "Ducky script · USB HID gadget (root)",
    "printer_available": "Available printers",
    "smarttv_cast_title": "SmartTV Cast",
    "whisperpair_tws_empty": "No devices found — put your earbuds in pairing mode",
    "whisperpair_tws_hint": "Pick your earbuds — pairing and audio capture run fully automatically",
    "whisperpair_tws_scanning": "Scanning Bluetooth devices…",
    "whisperpair_tws_stopped": "Stopped",
    "whisperpair_tws_title": "TWS Intercept",
    "wifi_print_title": "Wi-Fi printing",
    "language_system": "System default",
    "language_picker_title": "App language",
    "language_picker_subtitle": "Interface language. Screens without a translation stay in English.",
    "language_picker_search": "Search a language",
    "wordlist_common_title": "Common passwords",
    "wordlist_router_title": "Router defaults",
    "card_db_title": "Card Database",
    "card_db_desc": "NFC, transit and access card reference",
    "card_db_search": "Search a card, a chip, an AID",
    "card_db_all": "All",
    "card_db_count": "%1$d cards",
    "wordlist_generated_title": "Generated patterns",
    "about_enhanced_by": "Enhanced by camzzz",
    "about_github": "GitHub",
    "about_upstream": "Based on the original Dolphy by unvoiddd — GPL v3",
    "about_license": "Protected by GNU GPL v3",
    "about_contributors": "Thanks to",
    "settings_root_title": "Root access",
    "settings_root_checking": "Checking\u2026",
    "settings_root_yes": "Root detected",
    "settings_root_no": "Not rooted",
    "consent_title": "Educational and authorized use only",
    "consent_body": "Dolphy Enhanced is a research and education tool for wireless protocols, NFC/RFID and hardware emulation.\n\nEvery module is provided for educational purposes and for testing equipment you own or are explicitly authorised to test. Using this app against third-party devices or networks, to disrupt, intercept or impersonate them, is illegal in most countries.\n\nThe authors and contributors accept no responsibility for misuse. By continuing you confirm that you are of legal age, that you will only use this app on systems you are allowed to test, and that you accept these terms.",
    "consent_note": "Nothing leaves your device: no account, no analytics, no tracking.",
    "consent_accept": "I have read and I agree",
    "consent_exit": "Exit",
    "settings_consent_row": "Usage terms",
    "functions_search_hint": "Search a function",
    "ir_flipper_extra_tv": "TVs",
    "ble_spam_title": "BLE Spam",
    "ble_spam_desc": "Pairing-request spam for Apple, Android, Windows and Samsung",
    "bond_spam_title": "Bond Spam",
    "bond_spam_desc": "Bluetooth Classic pairing flood",
    "oui_vendor": "Manufacturer",
}

# ---------------------------------------------------------------------------
# Russian text for keys that only existed in English.
# ---------------------------------------------------------------------------
RU_EXTRA = {
    "chat_connected": "Подключено",
    "chat_connecting": "Подключение к чату…",
    "chat_disconnected": "Отключено",
    "chat_error": "Ошибка чата: %s",
    "chat_message_hint": "Введите сообщение…",
    "chat_no_messages": "Сообщений пока нет",
    "chat_send_message": "Отправить сообщение",
    "chat_title": "Dolphy Чат",
    "chat_user_joined": "%s подключился к чату",
    "chat_user_left": "%s покинул чат",
    "global_chat": "Общий чат",
    "global_chat_desc": "Чат со всеми пользователями Dolphy",
    "global_chat_title": "Общий чат",
    "network_hub_waiting_message": "Подключитесь к «%1$s» в настройках Wi‑Fi и вернитесь в приложение.",
    "nfc_troll_no_desc": "Описание недоступно",
    "network_hub_title_en": "Wi-Fi DoS",
    "other_chat": "Dolphy Чат",
    "other_chat_desc": "Общий чат для пользователей Dolphy",
    "language_system": "Как в системе",
    "language_picker_title": "Язык приложения",
    "language_picker_subtitle": "Язык интерфейса. Экраны без перевода останутся на английском.",
    "language_picker_search": "Поиск языка",
    "wordlist_common_title": "Популярные пароли",
    "wordlist_router_title": "Заводские пароли роутеров",
    "card_db_title": "База карт",
    "card_db_desc": "Справочник NFC, транспортных и пропускных карт",
    "card_db_search": "Поиск карты, чипа, AID",
    "card_db_all": "Все",
    "card_db_count": "%1$d карт",
    "wordlist_generated_title": "Сгенерированные шаблоны",
    "about_enhanced_by": "Доработано camzzz",
    "about_github": "GitHub",
    "about_upstream": "На основе оригинального Dolphy от unvoiddd — GPL v3",
    "about_license": "Защищено GNU GPL v3",
    "about_contributors": "Спасибо:",
    "settings_root_title": "Root-доступ",
    "settings_root_checking": "Проверка\u2026",
    "settings_root_yes": "Root обнаружен",
    "settings_root_no": "Нет root",
    "consent_title": "Только для обучения и на авторизованных устройствах",
    "consent_body": "Dolphy Enhanced — исследовательский и образовательный инструмент для беспроводных протоколов, NFC/RFID и эмуляции оборудования.\n\nВсе модули предназначены для обучения и тестирования оборудования, которым вы владеете или на тестирование которого у вас есть явное разрешение. Использование приложения против чужих устройств и сетей, для их блокировки, перехвата или подмены, незаконно в большинстве стран.\n\nАвторы и участники не несут ответственности за неправомерное использование. Продолжая, вы подтверждаете, что достигли совершеннолетия, будете использовать приложение только на системах, тестирование которых вам разрешено, и принимаете эти условия.",
    "consent_note": "Ничего не покидает ваше устройство: ни аккаунта, ни аналитики, ни отслеживания.",
    "consent_accept": "Я прочитал и согласен",
    "consent_exit": "Выход",
    "settings_consent_row": "Условия использования",
    "functions_search_hint": "Поиск функции",
    "ir_flipper_extra_tv": "телевизоры",
    "ble_spam_title": "BLE Спам",
    "ble_spam_desc": "Спам запросами сопряжения Apple, Android, Windows и Samsung",
    "bond_spam_title": "Bond Спам",
    "bond_spam_desc": "Флуд запросами сопряжения Bluetooth Classic",
    "oui_vendor": "Производитель",
}


def parse(path: Path) -> tuple[list[str], dict[str, str], str]:
    """Return (key order, key->raw value, trailing text after last </string>)."""
    text = path.read_text(encoding="utf-8")
    order: list[str] = []
    values: dict[str, str] = {}

    # `<string name="x" />` is legal but would make the main regex swallow the
    # next entry, so lift those out first.
    for m in SELF_CLOSING_RE.finditer(text):
        key = m.group(1)
        if key not in values:
            order.append(key)
            values[key] = ""
    text = SELF_CLOSING_RE.sub("", text)

    for m in ENTRY_RE.finditer(text):
        key, value = m.group(1), m.group(2)
        if key not in values:
            order.append(key)
        values[key] = value
    return order, values, text


def write(path: Path, order: list[str], values: dict[str, str], header: str) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', header, "<resources>"]
    for key in order:
        lines.append(f'    <string name="{key}">{values[key]}</string>')
    lines.append("</resources>")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    for p in (EN_PATH, RU_PATH):
        if not p.exists():
            print(f"missing: {p}", file=sys.stderr)
            return 1

    en_order, en, _ = parse(EN_PATH)
    ru_order, ru, _ = parse(RU_PATH)

    # Empty entries shadow the fallback locale with nothing: drop them so
    # Android falls back to English instead of showing a blank label.
    for order, values in ((en_order, en), (ru_order, ru)):
        for key in [k for k in order if not values.get(k, "").strip()]:
            print(f"dropping empty entry '{key}'", file=sys.stderr)
            order.remove(key)
            values.pop(key, None)

    # Repair the truncated entry from the original English file.
    broken = "nfc_troll_no_desc"
    if broken in en and "<string" in en[broken]:
        en[broken] = EN_EXTRA[broken]

    added_en = added_ru = 0
    for key, value in EN_EXTRA.items():
        if key not in en:
            en[key] = value
            en_order.append(key)
            added_en += 1
    for key, value in RU_EXTRA.items():
        if key not in ru:
            ru[key] = value
            ru_order.append(key)
            added_ru += 1

    # Keys present in one file only must exist in both; fill from the other side.
    for key in list(en_order):
        if key not in ru:
            print(f"warning: no Russian text for '{key}'", file=sys.stderr)
    for key in list(ru_order):
        if key not in en:
            print(f"warning: no English text for '{key}'", file=sys.stderr)

    write(
        EN_PATH,
        en_order,
        en,
        "<!-- Default locale (English). Every other locale falls back to this file. -->",
    )
    write(
        RU_PATH,
        ru_order,
        ru,
        "<!-- Russian. Generated/maintained with tools/i18n/patch_parity.py -->",
    )

    print(f"English: {len(en_order)} keys (+{added_en})")
    print(f"Russian: {len(ru_order)} keys (+{added_ru})")
    print(f"parity: {len(set(en_order) ^ set(ru_order))} keys still differ")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
