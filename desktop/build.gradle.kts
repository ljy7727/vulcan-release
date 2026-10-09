import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "com.heda.vulcan"
version = "1.9.0"

dependencies {
    // Compose for Desktop（含 desktop 运行时与 Material3）
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // SQLite（桌面端用 JDBC 驱动，等价于安卓端的 SQLiteOpenHelper）
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")

    // xlsx 解析所用的 XML Pull Parser（与安卓端同款，保证解析行为一致）
    implementation("net.sf.kxml:kxml2:2.3.0")

    // 全局热键（截图 Alt+Z / 四击空格搜索）——与电脑版一致
    implementation("com.github.kwhat:jnativehook:2.2.2")

    // Compose Desktop 与 AWT 线程桥接
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    // 工程路径含中文，测试进程需显式使用 UTF-8，否则类加载失败
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8")
    systemProperty("file.encoding", "UTF-8")
    // 指定真实 Excel 数据目录：gradle test -PkulcanDataDir=<dir>
    (project.findProperty("kulcanDataDir") as String?)?.let { systemProperty("kulcan.data.dir", it) }
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}

compose.desktop {
    application {
        mainClass = "com.heda.vulcan.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            // 注意：jpackage 在 Windows 上以 GBK 读取 @argfile，包名/描述含中文会导致解析失败
            //（"Input length = 1"），故此处用 ASCII；生成后 exe 可自行重命名为「硫化工艺助手.exe」
            packageName = "vulcan-desktop"
            packageVersion = "1.9.0"
            description = "Vulcan Desktop"
            vendor = "Heda"
            // jpackage 裁剪后的运行时镜像默认不含 java.sql，SQLite JDBC 会 NoClassDefFoundError
            modules("java.sql")
            windows {
                menu = true
                shortcut = true
            }
        }
    }
}
