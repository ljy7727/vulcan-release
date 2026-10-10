package com.heda.vulcan.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.heda.vulcan.data.DbStats
import com.heda.vulcan.data.Defect
import com.heda.vulcan.data.DesktopDb
import com.heda.vulcan.data.DesktopPrefs
import com.heda.vulcan.data.FirstCure
import com.heda.vulcan.data.Memo
import com.heda.vulcan.data.ProcessSpec

/** 应用全局状态（Compose 可观察）。 */
class AppState(val db: DesktopDb, val prefs: DesktopPrefs) {

    var tab by mutableStateOf(0)

    var statusText by mutableStateOf("")

    var extraSeconds by mutableStateOf(prefs.extraSeconds)
    var themeId by mutableStateOf(prefs.themeId)
    var darkMode by mutableStateOf(prefs.darkMode)
    var trayEnabled by mutableStateOf(prefs.trayEnabled)
    var closeToTray by mutableStateOf(prefs.closeToTray)

    val patterns = mutableStateListOf<String>()
    val firstCures = mutableStateListOf<FirstCure>()
    val defects = mutableStateListOf<Defect>()
    val memos = mutableStateListOf<Memo>()

    var queryPattern by mutableStateOf("")
    var querySize by mutableStateOf("")
    var queryRegion by mutableStateOf("")
    var queryResult by mutableStateOf<ProcessSpec?>(null)
    var queryCandidates by mutableStateOf(listOf<ProcessSpec>())

    var stats by mutableStateOf(DbStats())

    /** 胶囊对齐查找表（大写关键词 → 花纹，忽略大小写） */
    val aliasLookup: Map<String, String>
        get() = com.heda.vulcan.data.CapsuleAlias.toLookup(prefs.capsuleAliases)

    fun reloadAll() {
        patterns.clear(); patterns.addAll(db.queryAllPatterns())
        firstCures.clear(); firstCures.addAll(db.queryFirstCures())
        defects.clear(); defects.addAll(db.queryDefects())
        memos.clear(); memos.addAll(db.queryMemos())
        stats = db.stats()
        extraSeconds = prefs.extraSeconds
    }

    fun persistPrefs() {
        prefs.extraSeconds = extraSeconds
        prefs.themeId = themeId
        prefs.darkMode = darkMode
        prefs.trayEnabled = trayEnabled
        prefs.closeToTray = closeToTray
        prefs.save()
    }

    fun setStatus(msg: String) { statusText = msg }
}
