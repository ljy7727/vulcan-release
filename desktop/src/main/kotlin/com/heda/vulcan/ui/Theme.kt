package com.heda.vulcan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 主题色板 —— 与 APK 端 `Palettes` 完全一致（含每个主题的背景色与深色变体）。
 * 切换主题时整体背景同步变化，而非仅强调色。
 */
data class ThemeDef(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkOnBackground: Color
)

object Themes {

    val blue = ThemeDef(
        id = "blue", label = "经典蓝",
        primary = Color(0xFF1565C0), secondary = Color(0xFF42A5F5), tertiary = Color(0xFF7E57C2),
        background = Color(0xFFF3F7FC), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF16202E),
        darkPrimary = Color(0xFF90CAF9), darkSecondary = Color(0xFF64B5F6), darkTertiary = Color(0xFFB39DDB),
        darkBackground = Color(0xFF0F1A28), darkSurface = Color(0xFF17263A), darkOnBackground = Color(0xFFDCE6F2)
    )
    val green = ThemeDef(
        id = "green", label = "翡翠绿",
        primary = Color(0xFF00796B), secondary = Color(0xFF26A69A), tertiary = Color(0xFF66BB6A),
        background = Color(0xFFF1F8F6), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF10221E),
        darkPrimary = Color(0xFF80CBC4), darkSecondary = Color(0xFF4DB6AC), darkTertiary = Color(0xFFA5D6A7),
        darkBackground = Color(0xFF0D1F1B), darkSurface = Color(0xFF15302A), darkOnBackground = Color(0xFFD9EDE8)
    )
    val orange = ThemeDef(
        id = "orange", label = "暖阳橙",
        primary = Color(0xFFE65100), secondary = Color(0xFFFB8C00), tertiary = Color(0xFFD81B60),
        background = Color(0xFFFFF6EF), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF2B1D12),
        darkPrimary = Color(0xFFFFB74D), darkSecondary = Color(0xFFFFA726), darkTertiary = Color(0xFFF48FB1),
        darkBackground = Color(0xFF241811), darkSurface = Color(0xFF35251B), darkOnBackground = Color(0xFFFFE9D6)
    )
    val rose = ThemeDef(
        id = "rose", label = "玫瑰红",
        primary = Color(0xFFAD1457), secondary = Color(0xFFEC407A), tertiary = Color(0xFF5E35B1),
        background = Color(0xFFFDF2F6), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF2B1620),
        darkPrimary = Color(0xFFF48FB1), darkSecondary = Color(0xFFF06292), darkTertiary = Color(0xFFB39DDB),
        darkBackground = Color(0xFF241019), darkSurface = Color(0xFF351A27), darkOnBackground = Color(0xFFFBE3EC)
    )
    val purple = ThemeDef(
        id = "purple", label = "星空紫",
        primary = Color(0xFF5E35B1), secondary = Color(0xFF7E57C2), tertiary = Color(0xFF26A69A),
        background = Color(0xFFF6F3FB), surface = Color(0xFFFFFFFF), onBackground = Color(0xFF1E1830),
        darkPrimary = Color(0xFFB39DDB), darkSecondary = Color(0xFF9575CD), darkTertiary = Color(0xFF80CBC4),
        darkBackground = Color(0xFF171226), darkSurface = Color(0xFF231C3A), darkOnBackground = Color(0xFFEAE2FA)
    )

    val all = listOf(blue, green, orange, rose, purple)

    fun byId(id: String): ThemeDef = all.firstOrNull { it.id == id } ?: orange
}

@Composable
fun VulcanTheme(themeId: String, dark: Boolean, content: @Composable () -> Unit) {
    val p = Themes.byId(themeId)
    val scheme = if (!dark) {
        lightColorScheme(
            primary = p.primary,
            onPrimary = Color.White,
            primaryContainer = p.primary.copy(alpha = 0.12f),
            onPrimaryContainer = p.primary,
            secondary = p.secondary,
            onSecondary = Color.White,
            secondaryContainer = p.secondary.copy(alpha = 0.14f),
            onSecondaryContainer = p.secondary,
            tertiary = p.tertiary,
            background = p.background,
            onBackground = p.onBackground,
            surface = p.surface,
            onSurface = p.onBackground,
            surfaceVariant = p.background,
            onSurfaceVariant = p.onBackground.copy(alpha = 0.75f),
            outline = p.primary.copy(alpha = 0.25f),
            error = Color(0xFFB3261E)
        )
    } else {
        darkColorScheme(
            primary = p.darkPrimary,
            onPrimary = Color(0xFF10202E),
            primaryContainer = p.darkPrimary.copy(alpha = 0.16f),
            onPrimaryContainer = p.darkPrimary,
            secondary = p.darkSecondary,
            onSecondary = Color(0xFF10202E),
            secondaryContainer = p.darkSecondary.copy(alpha = 0.16f),
            onSecondaryContainer = p.darkSecondary,
            tertiary = p.darkTertiary,
            background = p.darkBackground,
            onBackground = p.darkOnBackground,
            surface = p.darkSurface,
            onSurface = p.darkOnBackground,
            surfaceVariant = p.darkBackground,
            onSurfaceVariant = p.darkOnBackground.copy(alpha = 0.7f),
            outline = p.darkPrimary.copy(alpha = 0.3f),
            error = Color(0xFFF2B8B5)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
