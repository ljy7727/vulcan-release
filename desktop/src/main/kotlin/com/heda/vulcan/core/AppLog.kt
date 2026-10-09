package com.heda.vulcan.core

import com.heda.vulcan.data.DesktopDb
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 应用日志（等价于电脑版 app.log）。
 * 写入 %USERPROFILE%\.vulcan-desktop\app.log，同时打印到控制台。
 */
object AppLog {

    private val file: File = File(DesktopDb.dataDir(), "app.log")
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun line(msg: String) {
        val text = "[${LocalDateTime.now().format(fmt)}] $msg"
        println(text)
        try {
            file.parentFile?.mkdirs()
            file.appendText(text + System.lineSeparator())
        } catch (_: Exception) {
            // 日志失败不影响主流程
        }
    }

    fun startup() {
        line("=== 应用启动 ===")
        line("java=${System.getProperty("java.version")} os=${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
        val tray = java.awt.SystemTray.isSupported()
        line("托盘支持=$tray")
    }
}
