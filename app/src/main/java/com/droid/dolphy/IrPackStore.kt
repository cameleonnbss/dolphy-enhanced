package com.droid.dolphy

import android.content.Context
import android.util.Log
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Storage for the IR database.
 *
 * Historically the whole Flipper-IRDB tree lived in `assets/`, which put 12 MB of
 * remote files inside every APK download. It now lives in `filesDir/irpack` and is
 * fetched on demand; the bundled assets remain as a fallback so a device without
 * the pack keeps whatever ships in the APK.
 *
 * Every read goes through here, so the rest of the IR code never has to know
 * whether a given remote came from the pack or from the APK.
 */
object IrPackStore {

    private const val TAG = "IrPackStore"
    private const val DIR = "irpack"
    private const val MARKER = ".installed"

    /** Release asset holding the zipped database. */
    const val PACK_URL =
        "https://github.com/cameleonnbss/dolphy-enhanced/releases/download/ir-pack-v1/ir-pack.zip"

    fun root(context: Context): File = File(context.filesDir, DIR)

    fun isInstalled(context: Context): Boolean = File(root(context), MARKER).isFile

    /** Size of the installed pack in bytes, 0 when absent. */
    fun installedSize(context: Context): Long {
        val dir = root(context)
        if (!dir.isDirectory) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun open(context: Context, path: String): InputStream? {
        val file = File(root(context), path)
        if (file.isFile) {
            return try {
                file.inputStream()
            } catch (e: Exception) {
                Log.w(TAG, "pack open failed for $path: ${e.message}")
                null
            }
        }
        return try {
            context.assets.open(path)
        } catch (e: Exception) {
            null
        }
    }

    fun exists(context: Context, path: String): Boolean {
        if (File(root(context), path).isFile) return true
        return try {
            context.assets.open(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Directory listing, pack first then APK assets, de-duplicated. */
    fun list(context: Context, dir: String): List<String> {
        val out = LinkedHashSet<String>()
        val target = if (dir.isBlank()) root(context) else File(root(context), dir)
        try {
            target.listFiles()?.forEach { out.add(it.name) }
        } catch (_: Exception) {
        }
        try {
            context.assets.list(dir)?.forEach { out.add(it) }
        } catch (_: Exception) {
        }
        return out.toList()
    }

    /**
     * Downloads and unpacks the database. Returns the number of files written.
     * Throws on network or archive errors so the caller can show a message.
     */
    fun install(context: Context, onProgress: (Int) -> Unit = {}): Int {
        val tmp = File(context.cacheDir, "ir-pack.zip")
        download(PACK_URL, tmp, onProgress)
        val written = unzip(tmp, root(context))
        tmp.delete()
        File(root(context), MARKER).writeText(PACK_URL)
        Log.i(TAG, "IR pack installed: $written files")
        return written
    }

    fun uninstall(context: Context) {
        root(context).deleteRecursively()
    }

    private fun download(url: String, target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Dolphy-Enhanced")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress(((copied * 100) / total).toInt())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(zip: File, destination: File): Int {
        var count = 0
        val rootPath = destination.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                // Reject absolute paths and ".." traversal before writing anything.
                val outFile = File(destination, entry.name)
                if (!outFile.canonicalPath.startsWith(rootPath)) {
                    Log.w(TAG, "skipping suspicious entry ${entry.name}")
                    input.closeEntry()
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                    count++
                }
                input.closeEntry()
            }
        }
        return count
    }
}
