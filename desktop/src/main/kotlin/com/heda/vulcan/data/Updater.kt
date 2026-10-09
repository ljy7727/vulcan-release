package com.heda.vulcan.data

import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.zip.ZipInputStream

/**
 * 在线更新（GitHub 公开仓库分发）。
 *
 * manifest 约定（公开仓库根目录 manifest.json）：
 * {
 *   "version": "1.9.0",
 *   "notes": "更新说明",
 *   "desktop": {
 *     "rebuild":  { "version":"1.9.0","url":"https://...zip","size":123,"sha256":"..." },
 *     "original": { "version":"2.0","url":"https://...zip","size":123,"sha256":"..." }
 *   },
 *   "apk": { "version":"1.5.0","url":"https://..." },
 *   "web": { "version":"1.5.0","url":"https://..." }
 * }
 *
 * 两条桌面通道：rebuild = 重建编译版（有源码），original = 原版程序包（无源码，作为原始版本基线）。
 */
object Updater {

    const val DEFAULT_MANIFEST =
        "https://raw.githubusercontent.com/ljy7727/vulcan-release/main/manifest.json"

    data class Asset(
        val version: String,
        val url: String,
        val size: Long,
        val sha256: String,
        val notes: String
    ) {
        val available: Boolean get() = version.isNotEmpty() && url.isNotEmpty()
    }

    data class Manifest(
        val version: String,
        val notes: String,
        val desktopRebuild: Asset,
        val desktopOriginal: Asset,
        val apk: Asset,
        val web: Asset
    )

    private fun assetOf(v: Any?): Asset {
        val m = v.asMap()
        return Asset(
            version = m["version"].asStr(),
            url = m["url"].asStr(),
            size = (m["size"] as? Number)?.toLong() ?: 0L,
            sha256 = m["sha256"].asStr(),
            notes = m["notes"].asStr()
        )
    }

    /** 拉取 manifest；失败返回 null（离线/网络异常静默处理）。 */
    fun fetch(manifestUrl: String = DEFAULT_MANIFEST): Manifest? = try {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        val req = HttpRequest.newBuilder(URI.create(manifestUrl))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) null
        else {
            val root = Json.parse(resp.body()).asMap()
            val d = root["desktop"].asMap()
            Manifest(
                version = root["version"].asStr(),
                notes = root["notes"].asStr(),
                desktopRebuild = assetOf(d["rebuild"]),
                desktopOriginal = assetOf(d["original"]),
                apk = assetOf(root["apk"]),
                web = assetOf(root["web"])
            )
        }
    } catch (e: Exception) {
        AppLogLine("更新检查失败: ${e.message}")
        null
    }

    /** 重建版是否有新版本（按版本号字符串比较）。 */
    fun hasUpdate(m: Manifest): Boolean =
        m.desktopRebuild.available && m.desktopRebuild.version != AppVersion.VERSION

    /** 下载文件到目标路径（带进度回调，0..100）。返回是否成功。 */
    fun download(asset: Asset, dest: File, onProgress: (Int) -> Unit = {}): Boolean = try {
        dest.parentFile?.mkdirs()
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build()
        val req = HttpRequest.newBuilder(URI.create(asset.url)).GET().build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream())
        if (resp.statusCode() !in 200..299) {
            AppLogLine("下载失败 HTTP ${resp.statusCode()}")
            false
        } else {
            val total = asset.size.takeIf { it > 0 }
                ?: resp.headers().firstValueAsLong("content-length").orElse(0L)
            var read = 0L
            resp.body().use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress((read * 100 / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            onProgress(100)
            true
        }
    } catch (e: Exception) {
        AppLogLine("下载异常: ${e.message}")
        false
    }

    /** sha256 校验（manifest 提供时）。 */
    fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isEmpty()) return true
        val bytes = file.readBytes()
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) } == expected.lowercase()
    }

    /** 解压 zip 到目录。 */
    fun unzip(zip: File, targetDir: File): Boolean = try {
        targetDir.mkdirs()
        ZipInputStream(zip.inputStream()).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                val out = File(targetDir, e.name)
                if (e.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
            }
        }
        true
    } catch (e: Exception) {
        AppLogLine("解压失败: ${e.message}")
        false
    }

    /** 新版落地目录：%USERPROFILE%\.vulcan-desktop\update\<version> */
    fun updateDir(version: String): File =
        File(DesktopDb.dataDir(), "update" + File.separator + version)

    private fun AppLogLine(msg: String) {
        try {
            com.heda.vulcan.core.AppLog.line(msg)
        } catch (_: Throwable) {
            // 日志不可用时忽略
        }
    }
}
