package com.heda.vulcan

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.heda.vulcan.core.AppLog
import com.heda.vulcan.core.Hotkeys
import com.heda.vulcan.core.notifyTray
import com.heda.vulcan.core.setupTray
import com.heda.vulcan.data.DesktopDb
import com.heda.vulcan.data.DesktopPrefs
import com.heda.vulcan.ui.App
import com.heda.vulcan.ui.AppState

/**
 * 硫化工艺助手 · 桌面版 v1.9 —— 程序入口。
 *
 * 复刻自电脑版（vulcan-desktop）的反编译逻辑：Kotlin + Compose for Desktop，
 * SQLite(JDBC) 存储、全局热键（JNativeHook）、系统托盘（AWT）。
 */
fun main() = application {
    AppLog.startup()

    val prefs = remember { DesktopPrefs() }
    val db = remember { DesktopDb() }
    val state = remember { AppState(db, prefs) }

    // 激活门：版本未激活时拦截，不初始化热键/托盘
    var activated by remember { mutableStateOf(com.heda.vulcan.data.License.isActivated(prefs)) }

    LaunchedEffect(Unit) {
        state.reloadAll()
        AppLog.line("数据库: ${state.stats}")
        state.setStatus("就绪 · 数据目录 ${DesktopDb.dataDir().absolutePath}")
    }

    val windowState = rememberWindowState(
        size = DpSize(1100.dp, 760.dp),
        position = WindowPosition(Alignment.Center)
    )
    // 窗口可见性：关闭到托盘时隐藏（托盘菜单/双击可恢复）
    var windowVisible by remember { mutableStateOf(true) }

    Window(
        onCloseRequest = {
            if (state.closeToTray && state.trayEnabled) {
                AppLog.line("窗口已最小化到托盘")
                windowVisible = false
                notifyTray("硫化工艺助手", "已最小化到托盘；右键托盘图标可退出程序")
            } else {
                AppLog.line("用户退出")
                exitApplication()
            }
        },
        visible = windowVisible,
        state = windowState,
        title = "硫化工艺助手 v1.9"
    ) {
        if (!activated) {
            com.heda.vulcan.ui.ActivationGate(prefs) { activated = true }
            return@Window
        }

        // 系统托盘 + 全局热键（只初始化一次）
        LaunchedEffect(Unit) {
            setupTray(
                window = window,
                enabled = state.trayEnabled,
                onExit = { exitApplication() },
                onShow = { windowVisible = true }
            )
            Hotkeys.start(
                screenshotHotkey = prefs.screenshotHotkey,
                searchTrigger = prefs.searchTrigger,
                onScreenshot = { state.setStatus("触发框选识别（OCR 将在后续版本接入 RapidOCR）") },
                onSearch = {
                    window.isVisible = true
                    window.toFront()
                    state.setStatus("全局搜索：可直接粘贴物料后点【识别】")
                }
            )
        }

        App(state)
    }
}
