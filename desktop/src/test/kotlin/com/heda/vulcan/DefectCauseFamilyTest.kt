package com.heda.vulcan

import com.heda.vulcan.data.DefectCause
import com.heda.vulcan.data.DesktopDb
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 判胎记录「家族聚合」测试。
 * 背景：判胎表里 FM 家族有 17 个 code（FM/FMC/FMG/FMW…），但不良表只有 FM 一条，
 * 精确匹配会漏掉全部子类记录；queryCausesFamily 用前缀聚合修正该问题。
 */
class DefectCauseFamilyTest {

    private fun newDb(): Pair<DesktopDb, File> {
        val f = File.createTempFile("vulcan-family-test", ".db")
        f.delete()
        return DesktopDb(f) to f
    }

    @Test
    fun FM聚合_包含全部子类() {
        val (db, f) = newDb()
        try {
            db.insertJudgeBatch(
                listOf(
                    DefectCause("FM", "异物", "原因A", "技术科", 100),
                    DefectCause("FMG", "异物G", "原因B", "成型", 20),
                    DefectCause("FMW", "异物W", "原因C", "硫化", 5),
                    DefectCause("FMR", "异物R", "原因D", "技术科", 30),
                    DefectCause("XX", "其他不良", "原因E", "技术科", 7)
                )
            )
            val fam = db.queryCausesFamily("FM")
            assertEquals(4, fam.size, "FM 家族应含 FM/FMG/FMW/FMR 共 4 条")
            assertEquals(1, db.queryCauses("FM").size, "精确匹配只有 FM 本身 1 条")
            assertEquals(100, fam.first().count, "应按 count 降序排列")
            assertTrue(fam.none { it.code == "XX" }, "非 FM 家族不得混入")
            assertEquals(155, fam.sumOf { it.count }, "家族总条数应为 100+20+5+30")
        } finally {
            f.delete()
        }
    }

    @Test
    fun 非FM代码_退化为精确匹配() {
        val (db, f) = newDb()
        try {
            db.insertJudgeBatch(
                listOf(
                    DefectCause("BC", "缺胶", "原因X", "成型", 9),
                    DefectCause("BCA", "缺胶A", "原因Y", "成型", 1)
                )
            )
            // BC 同样存在同前缀子类，聚合行为一致
            assertEquals(2, db.queryCausesFamily("BC").size)
            assertEquals(1, db.queryCauses("BC").size)
        } finally {
            f.delete()
        }
    }
}
