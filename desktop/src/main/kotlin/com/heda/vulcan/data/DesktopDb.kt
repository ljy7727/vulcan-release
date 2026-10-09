package com.heda.vulcan.data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * 桌面版 SQLite 数据访问层（JDBC）。
 * 对应安卓端 DbHelper.kt（SQLiteOpenHelper）。库表结构、字段、索引保持一致，
 * 数据目录与电脑版一致：%USERPROFILE%\.vulcan-desktop\vulcan.db（与手机版/网页版通过 JSON 数据包互通）。
 */
class DesktopDb(private val dbFile: File = defaultDbFile()) {

    companion object {
        private const val DB_VERSION = 3

        fun dataDir(): File {
            val home = System.getProperty("user.home") ?: "."
            return File(home, ".vulcan-desktop")
        }

        fun defaultDbFile(): File = File(dataDir(), "vulcan.db")
    }

    private val conn: Connection = open()

    // 注意：conn 赋值完成后才能写 meta（initSchema 内部不可调用依赖 conn 的方法）
    init {
        setMeta("db_version", DB_VERSION.toString())
    }

    private fun open(): Connection {
        dbFile.parentFile?.mkdirs()
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (_: Exception) {
            // 若驱动已由 ServiceLoader 注册，则忽略
        }
        val c = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        c.autoCommit = true
        c.createStatement().use { it.execute("PRAGMA foreign_keys=ON") }
        initSchema(c)
        return c
    }

    private fun initSchema(c: Connection) {
        c.createStatement().use { st ->
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS process_spec (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pattern TEXT, material TEXT, size TEXT, region TEXT,
                    program TEXT, time_code TEXT, capsule TEXT, inch TEXT, external_temp TEXT,
                    clamp_force TEXT, pci_pressure TEXT, pci_height TEXT, rubber TEXT,
                    bead_code TEXT, bead_angle TEXT, effective_date TEXT
                )"""
            )
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ps_pattern_size ON process_spec(pattern, size)")
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ps_region ON process_spec(region)")
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS capsule_height (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    capsule TEXT UNIQUE, manufacturer TEXT, height TEXT, perimeter TEXT, diff TEXT
                )"""
            )
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS defect (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT UNIQUE, name TEXT, description TEXT, note TEXT DEFAULT ''
                )"""
            )
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS defect_cause (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT, name TEXT, cause TEXT, dept TEXT, count INTEGER
                )"""
            )
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dc_code ON defect_cause(code)")
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS first_cure (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    machine TEXT, pattern TEXT, material TEXT, size TEXT, region TEXT,
                    time_code TEXT, temp TEXT, final_time TEXT, capsule TEXT, height TEXT,
                    clamp_force TEXT, pci_pressure TEXT, pci_height TEXT, extra TEXT, created_at INTEGER
                )"""
            )
            st.executeUpdate(
                """CREATE TABLE IF NOT EXISTS memo (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    content TEXT, created_at INTEGER, updated_at INTEGER
                )"""
            )
            st.executeUpdate("CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT)")
        }
    }

    fun close() = conn.close()

    // ---------- 元信息 ----------
    fun setMeta(key: String, value: String) {
        conn.prepareStatement("INSERT INTO meta(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value").use {
            it.setString(1, key); it.setString(2, value); it.executeUpdate()
        }
    }

    fun getMeta(key: String): String? =
        conn.prepareStatement("SELECT value FROM meta WHERE key=?").use { ps ->
            ps.setString(1, key)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }

    // ---------- 工艺标准 ----------
    fun clearProcess() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM process_spec") }
    }

    fun insertProcessBatch(items: List<ProcessSpec>) {
        transaction {
            conn.prepareStatement(
                """INSERT INTO process_spec(pattern,material,size,region,program,time_code,capsule,inch,
                   external_temp,clamp_force,pci_pressure,pci_height,rubber,bead_code,bead_angle,effective_date)
                   VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"""
            ).use { ps ->
                for (s in items) {
                    ps.setString(1, s.pattern); ps.setString(2, s.material); ps.setString(3, s.size); ps.setString(4, s.region)
                    ps.setString(5, s.program); ps.setString(6, s.timeCode); ps.setString(7, s.capsule); ps.setString(8, s.inch)
                    ps.setString(9, s.externalTemp); ps.setString(10, s.clampForce); ps.setString(11, s.pciPressure)
                    ps.setString(12, s.pciHeight); ps.setString(13, s.rubber); ps.setString(14, s.beadCode)
                    ps.setString(15, s.beadAngle); ps.setString(16, s.effectiveDate)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    fun queryAllPatterns(): List<String> =
        queryStrings("SELECT DISTINCT pattern FROM process_spec ORDER BY pattern")

    fun querySizes(pattern: String): List<String> {
        val out = mutableListOf<String>()
        conn.prepareStatement("SELECT DISTINCT size FROM process_spec WHERE pattern=? ORDER BY size").use { ps ->
            ps.setString(1, pattern)
            ps.executeQuery().use { rs -> while (rs.next()) out.add(rs.getString(1) ?: "") }
        }
        return out
    }

    fun queryRegions(pattern: String, size: String): List<String> {
        val out = mutableListOf<String>()
        conn.prepareStatement(
            "SELECT DISTINCT region FROM process_spec WHERE pattern=? AND size=? ORDER BY region"
        ).use { ps ->
            ps.setString(1, pattern); ps.setString(2, size)
            ps.executeQuery().use { rs -> while (rs.next()) out.add(rs.getString(1) ?: "") }
        }
        return out
    }

    fun querySpecs(pattern: String, size: String): List<ProcessSpec> {
        val out = mutableListOf<ProcessSpec>()
        conn.prepareStatement("SELECT * FROM process_spec WHERE pattern=? AND size=?").use { ps ->
            ps.setString(1, pattern); ps.setString(2, size)
            ps.executeQuery().use { rs -> while (rs.next()) out.add(readSpec(rs)) }
        }
        return out
    }

    private fun readSpec(rs: ResultSet) = ProcessSpec(
        pattern = rs.getString("pattern") ?: "",
        material = rs.getString("material") ?: "",
        size = rs.getString("size") ?: "",
        region = rs.getString("region") ?: "",
        program = rs.getString("program") ?: "",
        timeCode = rs.getString("time_code") ?: "",
        capsule = rs.getString("capsule") ?: "",
        inch = rs.getString("inch") ?: "",
        externalTemp = rs.getString("external_temp") ?: "",
        clampForce = rs.getString("clamp_force") ?: "",
        pciPressure = rs.getString("pci_pressure") ?: "",
        pciHeight = rs.getString("pci_height") ?: "",
        rubber = rs.getString("rubber") ?: "",
        beadCode = rs.getString("bead_code") ?: "",
        beadAngle = rs.getString("bead_angle") ?: "",
        effectiveDate = rs.getString("effective_date") ?: ""
    )

    // ---------- 拉直高度 ----------
    fun clearHeight() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM capsule_height") }
    }

    fun insertHeightBatch(items: List<CapsuleHeight>) {
        transaction {
            conn.prepareStatement(
                "INSERT INTO capsule_height(capsule,manufacturer,height,perimeter,diff) VALUES(?,?,?,?,?) " +
                        "ON CONFLICT(capsule) DO UPDATE SET manufacturer=excluded.manufacturer,height=excluded.height,perimeter=excluded.perimeter,diff=excluded.diff"
            ).use { ps ->
                for (h in items) {
                    ps.setString(1, h.capsule); ps.setString(2, h.manufacturer); ps.setString(3, h.height)
                    ps.setString(4, h.perimeter); ps.setString(5, h.diff)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    fun queryHeight(capsule: String): CapsuleHeight? {
        conn.prepareStatement("SELECT * FROM capsule_height WHERE capsule=?").use { ps ->
            ps.setString(1, capsule)
            ps.executeQuery().use { rs ->
                if (rs.next()) return CapsuleHeight(
                    rs.getString("capsule") ?: "",
                    rs.getString("manufacturer") ?: "",
                    rs.getString("height") ?: "",
                    rs.getString("perimeter") ?: "",
                    rs.getString("diff") ?: ""
                )
            }
        }
        return null
    }

    // ---------- 不良简称 ----------
    fun clearDefect() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM defect") }
    }

    /** 导入不良：新记录写入，已存在的保留用户备注、仅更新名称/说明 */
    fun insertDefectBatch(items: List<Defect>) {
        transaction {
            conn.prepareStatement(
                "INSERT INTO defect(code,name,description,note) VALUES(?,?,?,'') " +
                        "ON CONFLICT(code) DO UPDATE SET name=excluded.name, description=excluded.description"
            ).use { ps ->
                for (d in items) {
                    ps.setString(1, d.code); ps.setString(2, d.name); ps.setString(3, d.description)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    fun queryDefects(): List<Defect> {
        val out = mutableListOf<Defect>()
        conn.createStatement().use { st ->
            st.executeQuery("SELECT code,name,description,note FROM defect ORDER BY code").use { rs ->
                while (rs.next()) out.add(
                    Defect(rs.getString(1) ?: "", rs.getString(2) ?: "", rs.getString(3) ?: "", rs.getString(4) ?: "")
                )
            }
        }
        return out
    }

    fun updateDefectNote(code: String, note: String) {
        conn.prepareStatement("UPDATE defect SET note=? WHERE code=?").use {
            it.setString(1, note); it.setString(2, code); it.executeUpdate()
        }
    }

    // ---------- 判胎分析 ----------
    fun clearJudge() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM defect_cause") }
    }

    fun insertJudgeBatch(items: List<DefectCause>) {
        transaction {
            conn.prepareStatement("INSERT INTO defect_cause(code,name,cause,dept,count) VALUES(?,?,?,?,?)").use { ps ->
                for (d in items) {
                    ps.setString(1, d.code); ps.setString(2, d.name); ps.setString(3, d.cause)
                    ps.setString(4, d.dept); ps.setInt(5, d.count)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    fun queryCauses(code: String): List<DefectCause> {
        val out = mutableListOf<DefectCause>()
        conn.prepareStatement("SELECT code,name,cause,dept,count FROM defect_cause WHERE code=? ORDER BY count DESC").use { ps ->
            ps.setString(1, code)
            ps.executeQuery().use { rs ->
                while (rs.next()) out.add(
                    DefectCause(rs.getString(1) ?: "", rs.getString(2) ?: "", rs.getString(3) ?: "", rs.getString(4) ?: "", rs.getInt(5))
                )
            }
        }
        return out
    }

    // ---------- 首缸 ----------
    fun insertFirstCure(f: FirstCure): Long {
        conn.prepareStatement(
            """INSERT INTO first_cure(machine,pattern,material,size,region,time_code,temp,final_time,capsule,
               height,clamp_force,pci_pressure,pci_height,extra,created_at)
               VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            java.sql.Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setString(1, f.machine); ps.setString(2, f.pattern); ps.setString(3, f.material); ps.setString(4, f.size)
            ps.setString(5, f.region); ps.setString(6, f.timeCode); ps.setString(7, f.temp); ps.setString(8, f.finalTime)
            ps.setString(9, f.capsule); ps.setString(10, f.height); ps.setString(11, f.clampForce)
            ps.setString(12, f.pciPressure); ps.setString(13, f.pciHeight); ps.setString(14, f.extra); ps.setLong(15, f.createdAt)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> if (rs.next()) return rs.getLong(1) }
        }
        return -1
    }

    fun queryFirstCures(): List<FirstCure> {
        val out = mutableListOf<FirstCure>()
        conn.createStatement().use { st ->
            st.executeQuery("SELECT * FROM first_cure ORDER BY created_at DESC").use { rs ->
                while (rs.next()) out.add(
                    FirstCure(
                        id = rs.getLong("id"), machine = rs.getString("machine") ?: "",
                        pattern = rs.getString("pattern") ?: "", material = rs.getString("material") ?: "",
                        size = rs.getString("size") ?: "", region = rs.getString("region") ?: "",
                        timeCode = rs.getString("time_code") ?: "", temp = rs.getString("temp") ?: "",
                        finalTime = rs.getString("final_time") ?: "", capsule = rs.getString("capsule") ?: "",
                        height = rs.getString("height") ?: "", clampForce = rs.getString("clamp_force") ?: "",
                        pciPressure = rs.getString("pci_pressure") ?: "", pciHeight = rs.getString("pci_height") ?: "",
                        extra = rs.getString("extra") ?: "", createdAt = rs.getLong("created_at")
                    )
                )
            }
        }
        return out
    }

    fun updateFirstCureMachine(id: Long, machine: String) {
        conn.prepareStatement("UPDATE first_cure SET machine=? WHERE id=?").use {
            it.setString(1, machine); it.setLong(2, id); it.executeUpdate()
        }
    }

    fun deleteFirstCure(id: Long) {
        conn.prepareStatement("DELETE FROM first_cure WHERE id=?").use { it.setLong(1, id); it.executeUpdate() }
    }

    fun clearFirstCure() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM first_cure") }
    }

    // ---------- 备忘录 ----------
    fun insertMemo(content: String): Long {
        val now = System.currentTimeMillis()
        conn.prepareStatement(
            "INSERT INTO memo(content,created_at,updated_at) VALUES(?,?,?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setString(1, content); ps.setLong(2, now); ps.setLong(3, now)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> if (rs.next()) return rs.getLong(1) }
        }
        return -1
    }

    fun updateMemo(id: Long, content: String) {
        conn.prepareStatement("UPDATE memo SET content=?, updated_at=? WHERE id=?").use {
            it.setString(1, content); it.setLong(2, System.currentTimeMillis()); it.setLong(3, id); it.executeUpdate()
        }
    }

    fun deleteMemo(id: Long) {
        conn.prepareStatement("DELETE FROM memo WHERE id=?").use { it.setLong(1, id); it.executeUpdate() }
    }

    fun queryMemos(): List<Memo> {
        val out = mutableListOf<Memo>()
        conn.createStatement().use { st ->
            st.executeQuery("SELECT id,content,created_at,updated_at FROM memo ORDER BY updated_at DESC").use { rs ->
                while (rs.next()) out.add(Memo(rs.getLong(1), rs.getString(2) ?: "", rs.getLong(3), rs.getLong(4)))
            }
        }
        return out
    }

    // ---------- 统计 ----------
    fun stats(): DbStats {
        fun count(table: String): Int =
            conn.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> if (rs.next()) rs.getInt(1) else 0 } }
        fun countDistinct(sql: String): Int =
            conn.createStatement().use { st -> st.executeQuery(sql).use { rs -> if (rs.next()) rs.getInt(1) else 0 } }
        return DbStats(
            patternCount = countDistinct("SELECT COUNT(DISTINCT pattern) FROM process_spec"),
            specCount = count("process_spec"),
            heightCount = count("capsule_height"),
            defectCount = count("defect"),
            judgeCount = count("defect_cause"),
            firstCureCount = count("first_cure"),
            memoCount = count("memo"),
            lastImport = getMeta("last_import") ?: ""
        )
    }

    // ---------- 工具 ----------
    private fun queryStrings(sql: String): List<String> {
        val out = mutableListOf<String>()
        conn.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> while (rs.next()) out.add(rs.getString(1) ?: "") }
        }
        return out
    }

    private inline fun transaction(block: () -> Unit) {
        conn.autoCommit = false
        try {
            block()
            conn.commit()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }
}
