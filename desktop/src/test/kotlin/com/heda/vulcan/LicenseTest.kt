package com.heda.vulcan

import com.heda.vulcan.data.DesktopPrefs
import com.heda.vulcan.data.License
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 序列号激活测试。
 * 注意：仓库中不保存任何序列号明文；正向用例通过环境变量 VULCAN_TEST_SN 注入，
 * 未设置时自动跳过正向断言（避免明文入库）。
 */
class LicenseTest {

    @Test
    fun 空输入被拒绝() {
        assertFalse(License.verify(""))
        assertFalse(License.verify("   "))
    }

    @Test
    fun 错误序列号被拒绝() {
        assertFalse(License.verify("000000"))
        assertFalse(License.verify("abc123"))
        assertFalse(License.verify("wrong-sn-999"))
    }

    @Test
    fun 哈希函数符合SHA256() {
        // 已知向量：SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEqualsSha(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            License.sha256("abc")
        )
    }

    @Test
    fun 未激活版本识别() {
        val f = File.createTempFile("vulcan-prefs-test", ".json")
        f.delete()
        val prefs = DesktopPrefs(f)
        prefs.activatedVersion = "0.0.0-old"
        assertFalse(License.isActivated(prefs))
        f.delete()
    }

    @Test
    fun 正确序列号可通过_需环境变量() {
        val sn = System.getenv("VULCAN_TEST_SN")
        if (sn.isNullOrBlank()) {
            // 仓库内不含明文，跳过正向断言
            assertTrue(true)
        } else {
            assertTrue(License.verify(sn), "环境变量提供的序列号应通过校验")
            assertTrue(License.verify(sn.uppercase()), "应忽略大小写")
        }
    }

    private fun assertEqualsSha(expected: String, actual: String) {
        assertTrue(expected == actual, "SHA-256 结果不符")
    }
}
