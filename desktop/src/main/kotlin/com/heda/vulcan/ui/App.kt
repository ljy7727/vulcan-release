package com.heda.vulcan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.awt.FileDialog
import java.io.File

/** 模块定义 */
private val TABS = listOf(
    "查询", "首缸", "不良", "备忘", "数据", "备份", "设置", "更新"
)

@Composable
fun App(state: AppState) {
    VulcanTheme(state.themeId, state.darkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧导航
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "硫化工艺助手",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text("桌面版 v1.9", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Box(Modifier.padding(top = 6.dp)) {}
                    TABS.forEachIndexed { i, name ->
                        val selected = state.tab == i
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { state.tab = i }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                name,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 内容区
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                                .padding(16.dp)
                        ) {
                            when (state.tab) {
                                0 -> QueryScreen(state)
                                1 -> FirstCureScreen(state)
                                2 -> DefectScreen(state, scroll)
                                3 -> MemoScreen(state)
                                4 -> ImportScreen(state)
                                5 -> BackupScreen(state)
                                6 -> SettingsScreen(state)
                                else -> UpdateScreen(state)
                            }
                        }
                    }
                    // 状态栏
                    if (state.statusText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text(state.statusText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/** 文件选择（AWT FileDialog，桌面原生）。 */
fun pickFile(mode: Int, title: String, filterExt: String? = null): File? {
    val dlg = FileDialog(null as java.awt.Frame?, title, mode)
    if (filterExt != null) dlg.file = "*.$filterExt"
    dlg.isVisible = true
    val dir = dlg.directory ?: return null
    val name = dlg.file ?: return null
    return File(dir, name)
}

fun pickOpenFile(title: String, ext: String? = null): File? = pickFile(FileDialog.LOAD, title, ext)

fun pickSaveFile(title: String, defaultName: String): File? {
    val dlg = FileDialog(null as java.awt.Frame?, title, FileDialog.SAVE)
    dlg.file = defaultName
    dlg.isVisible = true
    val dir = dlg.directory ?: return null
    val name = dlg.file ?: return null
    return File(dir, name)
}
