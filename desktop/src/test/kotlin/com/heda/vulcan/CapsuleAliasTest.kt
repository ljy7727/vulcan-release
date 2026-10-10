package com.heda.vulcan

import com.heda.vulcan.data.CapsuleAlias
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 胶囊对齐（花纹别名）测试：默认三条、忽略大小写、换算逻辑。 */
class CapsuleAliasTest {

    @Test
    fun 默认三条映射() {
        val lookup = CapsuleAlias.toLookup(CapsuleAlias.defaultAliases())
        assertEquals("A2000", lookup["STEADY-33"])
        assertEquals("HG918", lookup["DRB01"])
        assertEquals("RU06", lookup["DRB02"])
        assertEquals(3, lookup.size)
    }

    @Test
    fun 全部忽略大小写() {
        val lookup = CapsuleAlias.toLookup(CapsuleAlias.defaultAliases())
        // lookup 的 key 契约是「大写」；对外接口（display/resolvePattern）内部自动转大写
        assertEquals("A2000", lookup["steady-33".uppercase()])
        assertEquals("A2000", lookup["Steady-33".uppercase()])
        assertEquals("HG918", lookup["drb01".uppercase()])
        // display 忽略大小写（对外接口）
        assertEquals("drb01（HG918）", CapsuleAlias.display("drb01", lookup))
        assertEquals("Steady-33（A2000）", CapsuleAlias.display("Steady-33", lookup))
        assertEquals("drb02（RU06）", CapsuleAlias.display("drb02", lookup))
    }

    @Test
    fun 不认识的花纹按对齐换算() {
        val lookup = CapsuleAlias.toLookup(listOf(listOf("ZDT888", "HG918")))
        val known = listOf("HG918", "RU06", "A2000")
        // 未见过 zdt888 → 换算为 HG918（忽略大小写）
        assertEquals("HG918", CapsuleAlias.resolvePattern("ZDT888", known, lookup))
        assertEquals("HG918", CapsuleAlias.resolvePattern("zdt888", known, lookup))
        // 已在库中的花纹不换算
        assertNull(CapsuleAlias.resolvePattern("HG918", known, lookup))
        // 无对应关系 → null
        assertNull(CapsuleAlias.resolvePattern("XXXX", known, lookup))
        // 目标不在已知库 → null（不能凭空造）
        assertNull(CapsuleAlias.resolvePattern("ZDT888", listOf("RU06"), lookup))
    }

    @Test
    fun display追加花纹与空值处理() {
        val lookup = CapsuleAlias.toLookup(CapsuleAlias.defaultAliases())
        assertEquals("DRB01（HG918）", CapsuleAlias.display("DRB01", lookup))
        assertEquals("UNKNOWN", CapsuleAlias.display("UNKNOWN", lookup))
        assertEquals("", CapsuleAlias.display("", lookup))
    }

    @Test
    fun 自定义配置可增删() {
        val custom = listOf(listOf("ABC", "XYZ"), listOf("FOO", "BAR"))
        val lookup = CapsuleAlias.toLookup(custom)
        assertEquals(2, lookup.size)
        assertEquals("XYZ", lookup["abc".uppercase()])
        assertEquals("BAR", lookup["foo".uppercase()])
        // display 走对外接口，自动忽略大小写
        assertEquals("abc（XYZ）", CapsuleAlias.display("abc", lookup))
        // 空值/坏数据被忽略
        val withBad = CapsuleAlias.toLookup(listOf(listOf("", "A"), listOf("B", ""), listOf("C")))
        assertEquals(0, withBad.size)
    }
}
