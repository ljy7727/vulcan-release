package com.heda.vulcan.core

import java.net.BindException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.swing.SwingUtilities

/**
 * 单实例保护。
 *
 * 第一个实例在本机回环端口上监听；后续再次启动的实例会：
 * 1. 绑定端口失败 → 通过端口发送 "SHOW" 指令，通知旧实例把窗口从托盘唤出；
 * 2. 自己直接退出。
 *
 * 避免两个实例同时读写同一个 SQLite（锁冲突/数据错乱）。
 */
object SingleInstance {

    private const val PORT = 47801
    private const val MAGIC = "VULCAN_SHOW"

    /** 旧实例收到唤醒指令时执行（Main 里接线到 windowVisible = true） */
    @Volatile
    var onShowRequest: (() -> Unit)? = null

    /**
     * @return true = 本进程是主实例，正常启动；
     *         false = 已有实例在运行（已通知其显示窗口），本进程应立即退出。
     */
    fun activateOrNotify(): Boolean = try {
        val server = ServerSocket(PORT, 5, InetAddress.getLoopbackAddress())
        // 后台线程：监听后续实例的唤醒请求
        Thread {
            while (!server.isClosed) {
                try {
                    server.accept().use { sock ->
                        val msg = sock.getInputStream().readNBytes(64).decodeToString()
                        if (msg.contains(MAGIC)) {
                            AppLog.line("单实例：收到唤醒请求，显示主窗口")
                            SwingUtilities.invokeLater { onShowRequest?.invoke() }
                        }
                    }
                } catch (_: Throwable) {
                    // 单次 accept 失败不影响监听
                }
            }
        }.apply { isDaemon = true; name = "single-instance-listener" }.start()
        true
    } catch (_: BindException) {
        // 端口被占 = 已有实例 → 通知它显示窗口
        try {
            Socket("127.0.0.1", PORT).use { s ->
                s.getOutputStream().write(MAGIC.toByteArray())
                s.getOutputStream().flush()
            }
        } catch (_: Throwable) {
            // 通知失败也照样退出，避免双实例
        }
        false
    } catch (_: Throwable) {
        // 探测本身失败（极少数环境），不阻塞启动
        true
    }
}
