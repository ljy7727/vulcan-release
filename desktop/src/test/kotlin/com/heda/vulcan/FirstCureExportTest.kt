package com.heda.vulcan

import com.heda.vulcan.data.FirstCure
import com.heda.vulcan.ui.FirstCureExport
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 首缸长图导出测试（版式与网页版 firstCureCanvas 一致）。 */
class FirstCureExportTest {

    private fun sample(n: Int): List<FirstCure> = (1..n).map { i ->
        FirstCure(
            id = i.toLong(), machine = "110$i", pattern = "RU06", material = "225/50ZR17 XL RU06 4 98W CH LB DURUN",
            size = "225/50ZR17", region = "CH", timeCode = "PB178T10", temp = "178",
            finalTime = "10分59秒", capsule = "DRB01", height = "430",
            clampForce = "1300", pciPressure = "0.2", pciHeight = "196",
            extra = "", createdAt = System.currentTimeMillis()
        )
    }

    @Test
    fun 渲染尺寸符合版式() {
        val img = FirstCureExport.render(sample(3), "硫化首缸清单")
        assertEquals(1400, img.width, "宽度应为 1400")
        assertEquals(120 + 3 * 74 + 60, img.height, "高度 = 表头120 + 3行*74 + 页脚60")
    }

    @Test
    fun 单条与空列表不崩() {
        assertEquals(120 + 74 + 60, FirstCureExport.render(sample(1), "T").height)
        assertEquals(120 + 0 + 60, FirstCureExport.render(emptyList(), "T").height)
    }

    @Test
    fun 保存为有效的PNG() {
        val img = FirstCureExport.render(sample(5), "硫化首缸清单")
        val f = File.createTempFile("vulcan-export-test", ".png")
        try {
            assertTrue(FirstCureExport.save(img, f), "保存应成功")
            assertTrue(f.length() > 1000, "PNG 文件应有内容")
            val read = ImageIO.read(f)
            assertEquals(1400, read.width)
        } finally {
            f.delete()
        }
    }

    @Test
    fun 文件名格式含年月日() {
        val name = FirstCureExport.todayFileName()
        assertTrue(name.endsWith("首缸图.png"), "文件名应以「首缸图.png」结尾，实际：$name")
        assertTrue(name.contains("年") && name.contains("月") && name.contains("日"))
    }
}
