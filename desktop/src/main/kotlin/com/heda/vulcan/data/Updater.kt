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

    /**
     * manifest 源（按顺序回退，任一可达即可）。
     * ① GitHub API：在内网/代理环境下最常被放行；
     * ② raw：直连最快；
     * ③ jsDelivr CDN 镜像：GitHub 被墙时的兜底。
     */
    val MANIFEST_SOURCES: List<String> = listOf(
        "https://api.github.com/repos/ljy7727/vulcan-release/contents/manifest.json",
        "https://raw.githubusercontent.com/ljy7727/vulcan-release/main/manifest.json",
        "https://cdn.jsdelivr.net/gh/ljy7727/vulcan-release@main/manifest.json"
    )

    val DEFAULT_MANIFEST: String get() = MANIFEST_SOURCES.first()

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

    /** 依次尝试各 manifest 源，任一成功即返回；全部失败返回 null（离线静默）。 */
    fun fetch(): Manifest? {
        for (src in MANIFEST_SOURCES) {
            fetchOne(src)?.let { return it }
        }
        AppLogLine("更新检查失败：所有源均不可达")
        return null
    }

    /** 拉取单个 manifest 源。GitHub API 返回 base64 包裹，自动解码。 */
    fun fetchOne(manifestUrl: String): Manifest? = try {
        val client = newClient()
        val req = HttpRequest.newBuilder(URI.create(manifestUrl))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) null
        else {
            val root0 = Json.parse(resp.body()).asMap()
            // GitHub Contents API：{"encoding":"base64","content":"..."}
            val body = if (root0["encoding"].asStr() == "base64") {
                String(java.util.Base64.getMimeDecoder().decode(root0["content"].asStr()))
            } else resp.body()
            val root = Json.parse(body).asMap()
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
        AppLogLine("manifest 源不可达: $manifestUrl (${e.message})")
        null
    }

    /** 构建 HttpClient（支持 http(s)_proxy 环境变量，企业内网必需）。 */
    private fun newClient(): HttpClient {
        val b = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
        proxyAddress()?.let { (host, port) ->
            try {
                b.proxy(java.net.ProxySelector.of(java.net.InetSocketAddress(host, port)))
            } catch (_: Exception) {
            }
        }
        return b.build()
    }

    private fun proxyAddress(): Pair<String, Int>? {
        val raw = System.getenv("https_proxy") ?: System.getenv("HTTPS_PROXY")
            ?: System.getenv("http_proxy") ?: System.getenv("HTTP_PROXY")
            ?: return null
        return try {
            val u = URI.create(if (raw.contains("://")) raw else "http://$raw")
            Pair(u.host, if (u.port > 0) u.port else 80)
        } catch (_: Exception) {
            null
        }
    }

    /** 重建版是否有新版本（按版本号字符串比较）。 */
    fun hasUpdate(m: Manifest): Boolean =
        m.desktopRebuild.available && m.desktopRebuild.version != AppVersion.VERSION

    /** 下载文件到目标路径（带进度回调，0..100）。返回是否成功。 */
    fun download(asset: Asset, dest: File, onProgress: (Int) -> Unit = {}): Boolean = try {
        dest.parentFile?.mkdirs()
        val client = newClient()
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
