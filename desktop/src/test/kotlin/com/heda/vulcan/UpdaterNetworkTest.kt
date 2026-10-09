package com.heda.vulcan

import com.heda.vulcan.data.Updater
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 在线更新网络测试（手动触发）。
 * 默认跳过；设置环境变量 VULCAN_TEST_NET=1 时执行真实网络拉取。
 */
class UpdaterNetworkTest {

    @Test
    fun 拉取manifest并解析双通道() {
        if (System.getenv("VULCAN_TEST_NET") != "1") {
            println("[skip] 未设置 VULCAN_TEST_NET=1，跳过网络测试")
            return
        }
        val m = Updater.fetch()
        assertTrue(m != null, "manifest 应可拉取（多源回退）")
        println("manifest.version      = ${m!!.version}")
        println("manifest.notes        = ${m.notes}")
        println("rebuild.version       = ${m.desktopRebuild.version}")
        println("rebuild.url           = ${m.desktopRebuild.url}")
        println("rebuild.size          = ${m.desktopRebuild.size}")
        println("rebuild.sha256        = ${m.desktopRebuild.sha256}")
        println("original.version      = ${m.desktopOriginal.version}")
        println("original.url          = ${m.desktopOriginal.url}")
        println("hasUpdate(rebuild)    = ${Updater.hasUpdate(m)}")
        assertTrue(m.desktopRebuild.url.isNotEmpty(), "重建版下载地址应已填入")
        assertTrue(m.desktopOriginal.url.isNotEmpty(), "原版下载地址应已填入")
    }
}
