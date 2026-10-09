package com.heda.vulcan.core

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 全局热键（等价于电脑版的 Hotkeys / JNativeHook）：
 *  - 截图识别：Alt + Z
 *  - 全局搜索：连续敲击空格 4 次
 * 若原生库不可用（如精简系统），仅记录日志，不影响主程序。
 */
object Hotkeys {

    private var started = false
    private var lastSpaceAt = 0L
    private var spaceCount = 0

    fun start(
        screenshotHotkey: String,
        searchTrigger: String,
        onScreenshot: () -> Unit,
        onSearch: () -> Unit
    ) {
        if (started) return
        try {
            // 关闭 JNativeHook 的冗余日志
            Logger.getLogger("com.github.kwhat.jnativehook").level = Level.OFF

            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
                override fun nativeKeyPressed(e: NativeKeyEvent) {
                    // Alt + Z
                    if (e.keyCode == NativeKeyEvent.VC_Z &&
                        (e.modifiers and NativeKeyEvent.ALT_MASK) != 0
                    ) {
                        AppLog.line("触发框选识别（热键 $screenshotHotkey）")
                        onScreenshot()
                    }
                    // 四击空格
                    if (e.keyCode == NativeKeyEvent.VC_SPACE) {
                        val now = System.currentTimeMillis()
                        spaceCount = if (now - lastSpaceAt < 500) spaceCount + 1 else 1
                        lastSpaceAt = now
                        if (spaceCount >= 4) {
                            spaceCount = 0
                            AppLog.line("触发全局搜索（$searchTrigger）")
                            onSearch()
                        }
                    }
                }

                override fun nativeKeyReleased(e: NativeKeyEvent) {}
                override fun nativeKeyTyped(e: NativeKeyEvent) {}
            })
            started = true
            AppLog.line("全局热键已注册：截图=$screenshotHotkey 搜索=$searchTrigger")
        } catch (e: Throwable) {
            AppLog.line("全局热键初始化失败: ${e.message}")
        }
    }
}
