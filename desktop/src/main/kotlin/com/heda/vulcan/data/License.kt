package com.heda.vulcan.data

import java.security.MessageDigest

/**
 * 应用版本。更新发布时修改此处；版本号变化会触发重新激活（见 License）。
 */
object AppVersion {
    const val VERSION = "1.9.0"
    const val VERSION_CODE = 190
}

/**
 * 序列号激活。
 *
 * 安全约定：代码里**只保存加盐哈希**，绝不出现序列号明文；
 * 用户输入即时计算哈希后比对，验证不通过也不回显输入内容。
 *
 * 激活状态存于 prefs.json 的 activated_version；版本号变化即视为「新版本首次打开」，
 * 需要重新输入序列号（更新发布后自动生效）。
 */
object License {

    private const val SALT = "heda-vulcan-2026:"

    /** 已授权序列号对应的 SHA-256(salt + 小写序列号)，可放多条 */
    private val ACCEPTED = setOf(
        "6c94f2ffea631a3f454006d33f624c122e7483885d7fa4b1346731b93b9d6359"
    )

    /** 校验输入是否正确（去空格、忽略大小写）。 */
    fun verify(input: String): Boolean {
        val s = input.trim()
        if (s.isEmpty()) return false
        return ACCEPTED.contains(sha256(SALT + s.lowercase()))
    }

    /** 当前版本是否已激活。 */
    fun isActivated(prefs: DesktopPrefs): Boolean =
        prefs.activatedVersion == AppVersion.VERSION

    /** 写入激活记录（绑定当前版本）。 */
    fun activate(prefs: DesktopPrefs, input: String): Boolean {
        if (!verify(input)) return false
        prefs.activatedVersion = AppVersion.VERSION
        prefs.save()
        return true
    }

    fun sha256(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
