package com.heda.vulcan.ui

import com.heda.vulcan.data.CapsuleAlias
import com.heda.vulcan.data.FirstCure
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.imageio.ImageIO

/**
 * 首缸清单长图导出（1400px 宽）。
 * 版式与网页版 `firstCureCanvas` 一致：表头 120 / 行高 74 / 页脚 60，列宽相同。
 */
object FirstCureExport {

    private const val W = 1400
    private const val HEAD_H = 120
    private const val ROW_H = 74
    private const val FOOT_H = 60

    /** 列定义（名称 + 宽度），与网页版完全一致 */
    private val COLS = listOf(
        "机台" to 130, "规格" to 200, "花纹" to 120, "区域" to 80, "硫化时间" to 150,
        "模套" to 100, "胶囊" to 150, "拉直高度" to 120, "合模力" to 110, "PCI" to 170
    )

    private const val FONT = "Microsoft YaHei"

    fun render(list: List<FirstCure>, title: String, aliasLookup: Map<String, String> = emptyMap()): BufferedImage {
        val h = HEAD_H + list.size * ROW_H + FOOT_H
        val img = BufferedImage(W, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // 背景 + 顶部标题条
        g.color = Color.WHITE
        g.fillRect(0, 0, W, h)
        g.color = Color(0x15, 0x65, 0xC0)
        g.fillRect(0, 0, W, 84)
        g.color = Color.WHITE
        g.font = Font(FONT, Font.BOLD, 30)
        g.drawString(title, 30, 52)
        g.font = Font(FONT, Font.PLAIN, 18)
        g.drawString("共 ${list.size} 条 · 导出时间 $timeText", 30, 118)

        // 表头
        g.color = Color(0x37, 0x47, 0x4F)
        g.font = Font(FONT, Font.BOLD, 17)
        var x = 24
        for ((name, w) in COLS) {
            g.drawString(name, x, HEAD_H - 8)
            x += w
        }
        g.color = Color(0xCF, 0xD8, 0xDC)
        g.drawLine(0, HEAD_H, W, HEAD_H)

        // 数据行（斑马纹）
        list.forEachIndexed { i, f ->
            val y = HEAD_H + i * ROW_H
            if (i % 2 == 1) {
                g.color = Color(0xF5, 0xF8, 0xFA)
                g.fillRect(0, y, W, ROW_H)
            }
            g.color = Color(0x21, 0x21, 0x21)
            g.font = Font(FONT, Font.PLAIN, 17)
            val vals = listOf(
                f.machine.ifEmpty { "—" }, f.size, f.pattern, f.region, f.finalTime,
                "${f.temp.ifEmpty { "—" }}℃", CapsuleAlias.display(f.capsule, aliasLookup),
                "${f.height.ifEmpty { "—" }}mm", "${f.clampForce.ifEmpty { "—" }}KN",
                "${f.pciPressure}MPa / ${f.pciHeight}mm"
            )
            var cx = 24
            vals.forEachIndexed { j, v ->
                g.drawString(v, cx, y + 44)
                cx += COLS[j].second
            }
            g.color = Color(0xE0, 0xE0, 0xE0)
            g.drawLine(0, y + ROW_H - 1, W, y + ROW_H - 1)
        }

        // 页脚
        g.color = Color(0x90, 0xA0, 0xA5)
        g.font = Font(FONT, Font.PLAIN, 15)
        g.drawString("硫化工艺助手 · 桌面版", 24, h - 22)
        g.dispose()
        return img
    }

    fun save(img: BufferedImage, file: File): Boolean = try {
        file.parentFile?.mkdirs()
        ImageIO.write(img, "png", file)
        true
    } catch (_: Exception) {
        false
    }

    /** 默认文件名，如「2026年10月10日首缸图.png」 */
    fun todayFileName(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}年${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日首缸图.png"
    }

    private val timeText: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
