package com.droid.dolphy

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/** Which root implementation is installed, if any. */
enum class RootFlavor {
    NONE, MAGISK, KERNEL_SU, APATCH, SUPERSU, UNKNOWN;

    val displayName: String
        get() = when (this) {
            NONE -> "—"
            MAGISK -> "Magisk"
            KERNEL_SU -> "KernelSU"
            APATCH -> "APatch"
            SUPERSU -> "SuperSU"
            UNKNOWN -> "su"
        }
}

data class RootStatus(
    val rooted: Boolean = false,
    val flavor: RootFlavor = RootFlavor.NONE,
    val suPath: String? = null,
    /** Output of `su -v`, useful to tell builds apart. */
    val version: String? = null,
    /** True when the probe timed out — usually means the su prompt was denied. */
    val timedOut: Boolean = false,
) {
    val label: String
        get() = when {
            !rooted -> "—"
            flavor == RootFlavor.UNKNOWN -> "su"
            else -> flavor.displayName
        }
}

/**
 * Root detection with caching.
 *
 * The old implementation ran `su -c id` on every call, which spawns a process on
 * a non-rooted phone (slow) and can pop a Magisk prompt over and over on a
 * rooted one. Results are cached for a minute and the probe is bounded by a
 * timeout, so a hanging prompt cannot block the caller forever.
 */
object RootDetector {

    private const val TAG = "RootDetector"
    private const val CACHE_MS = 60_000L
    private const val TIMEOUT_MS = 2_500L

    private val suPaths = arrayOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
        "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/vendor/bin/su",
    )

    @Volatile private var cached: RootStatus? = null
    @Volatile private var cachedAt: Long = 0L

    /** Cached result; refreshes automatically after [CACHE_MS]. */
    fun status(context: Context? = null, force: Boolean = false): RootStatus {
        val now = System.currentTimeMillis()
        val current = cached
        if (!force && current != null && now - cachedAt < CACHE_MS) return current

        val result = detect(context)
        cached = result
        cachedAt = now
        return result
    }

    fun invalidate() {
        cached = null
        cachedAt = 0L
    }

    private fun detect(context: Context?): RootStatus {
        val suPath = suPaths.firstOrNull { runCatching { File(it).exists() }.getOrDefault(false) }

        // 1. Try to actually get uid 0 — the only check that really matters.
        val probe = runSu(arrayOf("su", "-c", "id"))
        val rooted = probe?.first?.contains("uid=0") == true

        if (!rooted) {
            // 2. No uid 0: a root manager app is only a weak hint.
            return RootStatus(
                rooted = false,
                flavor = if (suPath != null) detectManager(context) else RootFlavor.NONE,
                suPath = suPath,
                timedOut = probe == null && suPath != null,
            )
        }

        val flavor = if (suPath?.contains("magisk") == true) {
            RootFlavor.MAGISK
        } else {
            detectManager(context).takeIf { it != RootFlavor.NONE } ?: flavorFromVersion()
        }
        val version = runSu(arrayOf("su", "-v"))?.first?.trim()?.take(48)

        Log.d(TAG, "root=$rooted flavor=$flavor su=$suPath")
        return RootStatus(rooted = true, flavor = flavor, suPath = suPath, version = version)
    }

    private fun detectManager(context: Context?): RootFlavor {
        if (context == null) return RootFlavor.NONE
        val packages = mapOf(
            "com.topjohnwu.magisk" to RootFlavor.MAGISK,
            "me.weishu.kernelsu" to RootFlavor.KERNEL_SU,
            "me.bmax.apatch" to RootFlavor.APATCH,
            "eu.chainfire.supersu" to RootFlavor.SUPERSU,
        )
        for ((pkg, flavor) in packages) {
            val installed = runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
            if (installed) return flavor
        }
        return RootFlavor.NONE
    }

    private fun flavorFromVersion(): RootFlavor {
        val out = runSu(arrayOf("su", "-v"))?.first?.lowercase() ?: return RootFlavor.UNKNOWN
        return when {
            "magisk" in out -> RootFlavor.MAGISK
            "kernelsu" in out || "kernel su" in out -> RootFlavor.KERNEL_SU
            "apatch" in out -> RootFlavor.APATCH
            "supersu" in out -> RootFlavor.SUPERSU
            else -> RootFlavor.UNKNOWN
        }
    }

    /** Runs a command, returns (output, exitCode) or null on failure/timeout. */
    private fun runSu(command: Array<String>): Pair<String, Int>? = try {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            null
        } else {
            process.inputStream.bufferedReader().readText() to process.exitValue()
        }
    } catch (e: Exception) {
        Log.w(TAG, "su probe failed: ${e.message}")
        null
    }
}

/**
 * Backwards-compatible façade: existing modules call `RootUtils.isRooted()` and
 * `RootUtils.executeRootCommand(...)`. Both now go through [RootDetector].
 */
object RootUtils {
    private const val TAG = "RootUtils"

    fun isRooted(): Boolean = RootDetector.status().rooted

    fun status(): RootStatus = RootDetector.status()

    fun executeRootCommand(command: String): Pair<Int, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            val result = if (error.isNotEmpty()) "$output\n$error" else output
            Pair(exitCode, result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root command: $command", e)
            Pair(-1, e.message ?: "Unknown error")
        }
    }
}
