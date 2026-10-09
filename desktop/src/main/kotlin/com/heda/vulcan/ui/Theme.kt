package com.heda.vulcan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 主题（对应电脑版 5 套主题 + 深色模式）。
 */
data class ThemeDef(val id: String, val label: String, val primary: Color, val tertiary: Color)

object Themes {
    val all = listOf(
        ThemeDef("blue", "经典蓝", Color(0xFF1565C0), Color(0xFF7E57C2)),
        ThemeDef("green", "翡翠绿", Color(0xFF2E7D32), Color(0xFF00897B)),
        ThemeDef("orange", "暖阳橙", Color(0xFFE65100), Color(0xFFF9A825)),
        ThemeDef("rose", "玫瑰红", Color(0xFFAD1457), Color(0xFFD81B60)),
        ThemeDef("purple", "星空紫", Color(0xFF6A1B9A), Color(0xFF4527A0))
    )

    fun byId(id: String): ThemeDef = all.firstOrNull { it.id == id } ?: all[2]
}

@Composable
fun VulcanTheme(themeId: String, dark: Boolean, content: @Composable () -> Unit) {
    val t = Themes.byId(themeId)
    val scheme = if (dark) {
        darkColorScheme(
            primary = t.primary,
            tertiary = t.tertiary,
            surface = Color(0xFF1E1E1E),
            background = Color(0xFF121212)
        )
    } else {
        lightColorScheme(
            primary = t.primary,
            tertiary = t.tertiary,
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFEDF4FB)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
