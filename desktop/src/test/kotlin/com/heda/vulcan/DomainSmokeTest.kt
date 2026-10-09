package com.heda.vulcan

import com.heda.vulcan.data.DesktopDb
import com.heda.vulcan.data.ImportManager
import com.heda.vulcan.data.MaterialParser
import com.heda.vulcan.data.TableType
import com.heda.vulcan.data.TimeCode
import com.heda.vulcan.data.asIntOr
import com.heda.vulcan.data.asList
import com.heda.vulcan.data.asMap
import com.heda.vulcan.data.asStr
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 冒烟测试：用项目内真实 Excel 跑通「解析 → 识别 → 写库 → 查询」完整链路。
 * 数据文件来自 ../../04-共享数据与脚本/data/
 */
class DomainSmokeTest {

    private fun dataFile(name: String): File {
        // 允许通过 -Dkulcan.data.dir=<dir> 指定数据目录；否则用工程相对路径
        val configured = System.getProperty("kulcan.data.dir")
        val candidates = buildList {
            if (!configured.isNullOrBlank()) add(File(configured, name))
            add(File("../../04-共享数据与脚本/data/$name"))
            add(File("../../../04-共享数据与脚本/data/$name"))
            add(File("data/$name"))
        }
        val f = candidates.firstOrNull { it.exists() }
        assertNotNull(f, "找不到测试数据文件：$name（候选：${candidates.joinToString { it.absolutePath }}）")
        return f!!
    }

    private fun tempDb(): Pair<DesktopDb, File> {
        val dir = Files.createTempDirectory("vulcan-test").toFile()
        val dbFile = File(dir, "test.db")
        return DesktopDb(dbFile) to dir
    }

    // ---------- 领域逻辑 ----------

    @Test
    fun `时间代码解码 PB178T10 加机动14秒等于10分59秒`() {
        val r = assertNotNull(TimeCode.parse("PB178T10", 14))
        assertEquals(178, r.temp)
        assertEquals('T', r.letter)
        assertEquals(45, r.letterSec)
        assertEquals(10, r.minutes)
        assertEquals("10分59秒", r.finalText)
    }

    @Test
    fun `时间代码字母秒数映射`() {
        assertEquals(15, assertNotNull(TimeCode.parse("PB200Q1", 0)).letterSec)
        assertEquals(30, assertNotNull(TimeCode.parse("PB200H1", 0)).letterSec)
        assertEquals(45, assertNotNull(TimeCode.parse("PB200T1", 0)).letterSec)
        assertEquals(0, assertNotNull(TimeCode.parse("PB200Z1", 0)).letterSec)
    }

    @Test
    fun `物料字符串解析尺寸花纹区域`() {
        val patterns = listOf("RU06", "Dimax Sprint", "BC7778")
        val (size, pattern, region) = MaterialParser.extractFromFull(
            "225/50ZR17 XL RU06 4 98W CH LB DURUN", patterns
        )
        assertEquals("225/50ZR17", size)
        assertEquals("RU06", pattern)
        assertEquals("CH", region)
    }

    @Test
    fun `含空格的花纹名无法由物料解析直接命中_属原实现已知限制`() {
        val (_, pattern, _) = MaterialParser.extractFromFull(
            "215/45ZR17 XL Dimax Sprint 4 96H CN", listOf("RU06", "Dimax Sprint")
        )
        // 忠实复刻原实现：按空白分词后逐词匹配，含空格的花纹名无法命中
        // （原版电脑版/手机版同样如此，需在查询页用"花纹下拉"直接选择）
        assertNull(pattern)
    }

    @Test
    fun `不含空格的花纹名可正常命中`() {
        val (size, pattern, region) = MaterialParser.extractFromFull(
            "215/45ZR17 XL RU06 4 96H CN", listOf("RU06", "BC7778")
        )
        assertEquals("215/45ZR17", size)
        assertEquals("RU06", pattern)
        assertEquals("CN", region)
    }

    // ---------- xlsx 解析 + 导入 + 查询（真实数据） ----------

    @Test
    fun `解析真实工艺标准Excel并识别为工艺标准表`() {
        val f = dataFile("硫化规格示方导入夏季26.5.27.xlsx")
        val book = com.heda.vulcan.data.XlsxParser().parse(f)
        assertTrue(book.errors.isEmpty(), "解析报错：${book.errors}")
        assertTrue(book.sheets.isNotEmpty(), "未解析到任何 sheet")
        assertEquals(TableType.PROCESS, ImportManager.detectType(book))
    }

    @Test
    fun `真实工艺数据导入数据库并可查询回`() {
        val f = dataFile("硫化规格示方导入夏季26.5.27.xlsx")
        val (db, dir) = tempDb()
        try {
            val stats = ImportManager.applyImport(db, f)
            assertEquals(TableType.PROCESS.label, stats.type)
            assertTrue(stats.rowsImported > 100, "导入条数偏少：${stats.rowsImported}")

            val patterns = db.queryAllPatterns()
            assertTrue(patterns.isNotEmpty(), "没有花纹")

            // 按 花纹+尺寸 查询，验证工艺卡片可用
            val pattern = patterns.first()
            val sizes = db.querySizes(pattern)
            assertTrue(sizes.isNotEmpty(), "花纹 $pattern 没有尺寸")
            val size = sizes.first()
            val specs = db.querySpecs(pattern, size)
            assertTrue(specs.isNotEmpty(), "查不到 $pattern / $size")

            val spec = specs.first()
            val tc = assertNotNull(TimeCode.parse(spec.timeCode, 14), "时间代码解析失败：${spec.timeCode}")
            assertNotNull(tc.temp)

            // 首缸写入与回读
            val id = db.insertFirstCure(
                com.heda.vulcan.data.FirstCure(
                    id = 0, machine = "1101", pattern = spec.pattern, material = spec.material,
                    size = spec.size, region = spec.region, timeCode = spec.timeCode,
                    temp = tc.temp?.toString() ?: "", finalTime = tc.finalText, capsule = spec.capsule,
                    height = "", clampForce = spec.clampForce, pciPressure = spec.pciPressure,
                    pciHeight = spec.pciHeight, extra = "", createdAt = System.currentTimeMillis()
                )
            )
            assertTrue(id > 0, "首缸写入失败")
            assertEquals(1, db.queryFirstCures().size)

            // 统计
            val s = db.stats()
            assertTrue(s.specCount > 0, "统计条数为 0")
            assertTrue(s.patternCount > 0, "统计花纹数为 0")
            println("冒烟测试导入统计：$s")
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `真实不良简称Excel可导入且保留备注能力`() {
        val f = dataFile("外观不良简称-和达.xlsx")
        val (db, dir) = tempDb()
        try {
            val stats = ImportManager.applyImport(db, f)
            assertEquals(TableType.DEFECT.label, stats.type)
            assertTrue(stats.rowsImported > 10, "不良导入条数偏少：${stats.rowsImported}")

            val code = db.queryDefects().first().code
            db.updateDefectNote(code, "测试备注")
            assertEquals("测试备注", db.queryDefects().first { it.code == code }.note)
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `真实拉直高度Excel可导入`() {
        val f = dataFile("2. 拉直高度设定基准2026326.xlsx")
        val (db, dir) = tempDb()
        try {
            val stats = ImportManager.applyImport(db, f)
            assertEquals(TableType.HEIGHT.label, stats.type)
            assertTrue(stats.rowsImported > 0, "拉直高度导入 0 条")
        } finally {
            db.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `JSON数据包可序列化与反序列化`() {
        val root = linkedMapOf<String, Any?>(
            "app" to "vulcan-desktop",
            "n" to 3,
            "list" to listOf("a", "b"),
            "flag" to true
        )
        val text = com.heda.vulcan.data.Json.write(root)
        val back = com.heda.vulcan.data.Json.parse(text).asMap()
        assertEquals("vulcan-desktop", back["app"].asStr())
        assertEquals(3, back["n"].asIntOr(0))
        assertEquals(2, back["list"].asList().size)
        assertEquals(true, back["flag"])
    }
}
