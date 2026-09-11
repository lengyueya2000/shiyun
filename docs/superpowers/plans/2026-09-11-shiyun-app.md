# 古诗词 App「诗韵」实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建离线 Android 古诗词 App「诗韵」,包含每日一诗(打卡)、诗词库搜索浏览、收藏夹、按朝代闯关背诗四大功能。

**Architecture:** 单模块 MVVM。诗词数据由 Python 管线从开源 chinese-poetry 数据集精选约 2000 首生成 `assets/poems.json`,首次启动导入 Room(含 FTS4 全文索引)。UI 层 Jetpack Compose + Navigation Compose,四个底部 Tab + 详情页;domain 层含每日选诗算法与出题生成器。

**Tech Stack:** Kotlin 2.0.21、AGP 8.7.3、Compose BOM 2024.12.01、Room 2.6.1(KSP)、Navigation Compose 2.8.5、kotlinx-serialization 1.7.3、Robolectric 4.14.1(数据层测试)、Python 3(仅数据管线)。

**Spec:** `docs/superpowers/specs/2026-09-11-shiyun-app-design.md`(执行者必须同时阅读 spec)

## Global Constraints

- 包名 `com.shiyun.app`;JVM target 17;minSdk 26;compileSdk/targetSdk 35。
- 离线 App:AndroidManifest **不声明** INTERNET 权限。
- 配色固定:宣纸底 `#F7F4EA`、墨色 `#2B2B2B`、朱红 `#B03A2E`、淡墨 `#8A8578`、描边 `#D8D2C0`。
- 星级规则(spec 第 3.4 节歧义裁决):每关 5 题,答对 ≥4 过关;星级 = 5 对→3 星,4 对→2 星,否则 0 星;`stars >= 2` 解锁下一关;第一关永远解锁。
- 关卡 = 拥有 ≥5 首诗的朝代,顺序固定:先秦→汉→魏晋→唐→宋→元→明清。
- 所有命令在仓库根 `G:\zcode\gushi` 下、Git Bash 中执行;Windows 下 gradle 调用写法为 `./gradlew.bat <args>`。
- 每个任务结束必须 `git commit`;测试不过不得提交。
- 代码注释密度低,与 Java/Kotlin 惯例一致;不写"实现说明式"注释。

---

### Task 1: 环境验证与项目脚手架

**Files:**
- Create: `.gitignore`、`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`、`app/src/main/res/values/styles.xml`、`app/src/main/java/com/shiyun/app/ShiyunApp.kt`、`app/src/main/java/com/shiyun/app/MainActivity.kt`、`app/src/main/java/com/shiyun/app/ui/theme/Theme.kt`、`README.md`

**Interfaces:**
- Consumes: 无(首个任务)
- Produces: 可构建运行的最小 Compose App;`ShiyunApp` 将在 Task 4 扩展出 `container`;`ShiyunTheme` 供所有 UI 使用(签名固定为 `@Composable fun ShiyunTheme(content: @Composable () -> Unit)`)

- [ ] **Step 1: 验证/准备构建环境**

依次检查,任何一项缺失则按括号内方式补齐:
```bash
java -version   # 需 17+;缺失: winget install EclipseAdoptium.Temurin.17.JDK
echo $ANDROID_HOME; ls "$LOCALAPPDATA/Android/Sdk" 2>/dev/null   # SDK 缺失时:
# curl -L -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
# 解压到 $LOCALAPPDATA/Android/Sdk/cmdline-tools/latest,然后:
# sdkmanager --sdk_root=$LOCALAPPDATA/Android/Sdk "platform-tools" "platforms;android-35" "build-tools;35.0.0" (yes 接受许可)
gradle -v   # 缺失: curl -L -o /tmp/g.zip https://services.gradle.org/distributions/gradle-8.10.2-bin.zip 并解压使用其 bin/gradle
```

- [ ] **Step 2: 创建构建文件与骨架源码**

`.gitignore`:
```
.gradle/
build/
app/build/
local.properties
.idea/
*.iml
tools/chinese-poetry/
```

`settings.gradle.kts`:
```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "shiyun"
include(":app")
```

根 `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

`gradle.properties`:
```
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.shiyun.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.shiyun.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
```

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".ShiyunApp"
        android:label="诗韵"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="false"
        android:supportsRtl="false">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
注:`@mipmap/ic_launcher` 若默认 res 未随脚手架生成,新建 `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`,内容用 adaptive-icon 指向两个颜色 drawable:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@color/ic_launcher_foreground"/>
</adaptive-icon>
```
并新建 `app/src/main/res/values/colors.xml` 定义 `ic_launcher_background = #F7F4EA`、`ic_launcher_foreground = #B03A2E`(均为 `<color>` 资源)。

`app/src/main/res/values/styles.xml`:
```xml
<resources>
    <style name="Theme.Shiyun" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">#F7F4EA</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
</resources>
```
Manifest 的 `<application>` 上加 `android:theme="@style/Theme.Shiyun"`。

`app/src/main/java/com/shiyun/app/ui/theme/Theme.kt`:
```kotlin
package com.shiyun.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val Paper = Color(0xFFF7F4EA)
val Ink = Color(0xFF2B2B2B)
val Cinnabar = Color(0xFFB03A2E)
val FadedInk = Color(0xFF8A8578)
val Border = Color(0xFFD8D2C0)
val PaperRaised = Color(0xFFFCFAF3)

private val LightColors = lightColorScheme(
    primary = Cinnabar, onPrimary = Color.White,
    secondary = Ink, onSecondary = Paper,
    background = Paper, onBackground = Ink,
    surface = PaperRaised, onSurface = Ink,
    onSurfaceVariant = FadedInk,
    outline = Border,
)

private val DarkColors = darkColorScheme(
    primary = Cinnabar, onPrimary = Color.White,
    background = Color(0xFF1C1B18), onBackground = Color(0xFFE8E3D5),
    surface = Color(0xFF26241F), onSurface = Color(0xFFE8E3D5),
    onSurfaceVariant = Color(0xFF9A9587), outline = Color(0xFF4A463D),
)

private val SerifTypography = Typography().withFontFamily(FontFamily.Serif)

private fun Typography.withFontFamily(family: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
)

@Composable
fun ShiyunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SerifTypography,
        content = content,
    )
}
```

`app/src/main/java/com/shiyun/app/ShiyunApp.kt`:
```kotlin
package com.shiyun.app

import android.app.Application

class ShiyunApp : Application()
```

`app/src/main/java/com/shiyun/app/MainActivity.kt`:
```kotlin
package com.shiyun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.shiyun.app.ui.theme.ShiyunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShiyunTheme { Text("诗韵") } }
    }
}
```

`README.md`:写四行——项目名、一句话简介、构建命令(`./gradlew.bat :app:assembleDebug`)、测试命令(`./gradlew.bat :app:testDebugUnitTest`)。

- [ ] **Step 3: 生成 gradle wrapper 并写 local.properties**

```bash
gradle wrapper --gradle-version 8.10.2
echo "sdk.dir=$(cygpath -w "$LOCALAPPDATA/Android/Sdk" 2>/dev/null || echo $ANDROID_HOME)" > local.properties
```

- [ ] **Step 4: 构建验证**

Run: `./gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`,产出 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: Android 项目脚手架与古典主题基础"
```

---

### Task 2: Room 数据层(实体/DAO/FTS/数据库/搜索词构造)

**Files:**
- Create: `app/src/main/java/com/shiyun/app/data/db/Poem.kt`、`Favorite.kt`、`DailyRecord.kt`、`QuizState.kt`、`Converters.kt`、`ShiyunDatabase.kt`、`PoemDao.kt`、`FavoriteDao.kt`、`DailyRecordDao.kt`、`QuizStateDao.kt`、`app/src/main/java/com/shiyun/app/data/db/MatchQueryBuilder.kt`
- Test: `app/src/test/java/com/shiyun/app/data/db/MatchQueryBuilderTest.kt`、`app/src/test/java/com/shiyun/app/data/db/PoemDaoTest.kt`

**Interfaces:**
- Consumes: Task 1 的依赖配置(Room/KSP/Robolectric)
- Produces(后续任务依赖的确切签名):
  - `data class Poem(id: Long, title: String, dynasty: String, author: String, paragraphs: List<String>, kind: String, translation: String?, notes: String?, appreciation: String?, tags: List<String>, difficulty: Int, featured: Boolean, searchText: String)`
  - `data class Favorite(poemId: Long, createdAt: Long)`、`data class DailyRecord(date: String, poemId: Long, checkedIn: Boolean)`、`data class QuizState(levelId: String, stars: Int, unlocked: Boolean)`
  - `object MatchQueryBuilder { fun build(raw: String): String? }`
  - `PoemDao`: `suspend fun insertAll(poems: List<Poem>)`, `suspend fun count(): Int`, `fun observeById(id: Long): Flow<Poem?>`, `suspend fun getById(id: Long): Poem?`, `suspend fun searchFiltered(match: String, dynasty: String?, kind: String?): List<Poem>`, `suspend fun browse(dynasty: String?, kind: String?): List<Poem>`, `suspend fun featured(): List<Poem>`, `suspend fun all(): List<Poem>`
  - `FavoriteDao`: `fun observeAll(): Flow<List<Favorite>>`, `fun observeIsFavorite(poemId: Long): Flow<Boolean>`, `suspend fun add(poemId: Long)`, `suspend fun remove(poemId: Long)`
  - `DailyRecordDao`: `fun observeByDate(date: String): Flow<DailyRecord?>`, `suspend fun getByDate(date: String): DailyRecord?`, `suspend fun upsert(record: DailyRecord)`, `fun observeAll(): Flow<List<DailyRecord>>`
  - `QuizStateDao`: `fun observeAll(): Flow<List<QuizState>>`, `suspend fun upsert(state: QuizState)`
  - `ShiyunDatabase`: `abstract fun poemDao(): PoemDao` 等 + `companion object { fun build(context: Context): ShiyunDatabase }`

- [ ] **Step 1: 写 MatchQueryBuilder 失败测试**

`app/src/test/java/com/shiyun/app/data/db/MatchQueryBuilderTest.kt`:
```kotlin
package com.shiyun.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchQueryBuilderTest {
    @Test
    fun `单字查询构造为短语`() {
        assertEquals("\"月\"", MatchQueryBuilder.build("月"))
    }

    @Test
    fun `连续汉字构造为空格分隔短语`() {
        assertEquals("\"明 月 几 时\"", MatchQueryBuilder.build("明月几时"))
    }

    @Test
    fun `多词查询按 AND 短语拼接`() {
        assertEquals("\"明 月\" \"苏 轼\"", MatchQueryBuilder.build("明月 苏轼"))
    }

    @Test
    fun `过滤标点与引号`() {
        assertEquals("\"春 眠\"", MatchQueryBuilder.build("春\"眠!"))
    }

    @Test
    fun `空白输入返回 null`() {
        assertNull(MatchQueryBuilder.build("  "))
        assertNull(MatchQueryBuilder.build(""))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.db.MatchQueryBuilderTest"`
Expected: FAIL `MatchQueryBuilder` 未定义

- [ ] **Step 3: 实现 MatchQueryBuilder 与全部数据层类**

`data/db/MatchQueryBuilder.kt`:
```kotlin
package com.shiyun.app.data.db

object MatchQueryBuilder {
    // FTS 索引按单字分词(searchText 以空格分隔每字),查询构造为短语匹配
    fun build(raw: String): String? {
        val phrases = raw.trim().split(Regex("\\s+")).mapNotNull { chunk ->
            val chars = chunk.filter { it.isLetterOrDigit() }
            if (chars.isEmpty()) null else chars.joinToString(" ", "\"", "\"")
        }
        return if (phrases.isEmpty()) null else phrases.joinToString(" ")
    }

    fun segment(text: String): String =
        text.filter { it.isLetterOrDigit() }.joinToString(" ")
}
```

`data/db/Poem.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "poems")
data class Poem(
    @PrimaryKey val id: Long,
    val title: String,
    val dynasty: String,
    val author: String,
    val paragraphs: List<String>,
    val kind: String,
    val translation: String?,
    val notes: String?,
    val appreciation: String?,
    val tags: List<String>,
    val difficulty: Int,
    val featured: Boolean,
    val searchText: String,
)

@Fts4(contentEntity = Poem::class)
@Entity(tableName = "poems_fts")
data class PoemFts(
    val title: String,
    val author: String,
    val searchText: String,
)
```

`data/db/Favorite.kt`、`DailyRecord.kt`、`QuizState.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val poemId: Long, val createdAt: Long)

@Entity(tableName = "daily_records")
data class DailyRecord(@PrimaryKey val date: String, val poemId: Long, val checkedIn: Boolean)

@Entity(tableName = "quiz_states")
data class QuizState(@PrimaryKey val levelId: String, val stars: Int, val unlocked: Boolean)
```

`data/db/Converters.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStrings(value: List<String>): String = Json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toStrings(value: String): List<String> = Json.decodeFromString(listSerializer, value)
}
```

`data/db/PoemDao.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poems: List<Poem>)

    @Query("SELECT COUNT(*) FROM poems")
    suspend fun count(): Int

    @Query("SELECT * FROM poems WHERE id = :id")
    fun observeById(id: Long): Flow<Poem?>

    @Query("SELECT * FROM poems WHERE id = :id")
    suspend fun getById(id: Long): Poem?

    @Query(
        """SELECT p.* FROM poems p JOIN poems_fts f ON p.id = f.docid
        WHERE poems_fts MATCH :match
        AND (:dynasty IS NULL OR p.dynasty = :dynasty)
        AND (:kind IS NULL OR p.kind = :kind)
        LIMIT 200"""
    )
    suspend fun searchFiltered(match: String, dynasty: String?, kind: String?): List<Poem>

    @Query(
        """SELECT * FROM poems
        WHERE (:dynasty IS NULL OR dynasty = :dynasty)
        AND (:kind IS NULL OR kind = :kind)
        ORDER BY id LIMIT 200"""
    )
    suspend fun browse(dynasty: String?, kind: String?): List<Poem>

    @Query("SELECT * FROM poems WHERE featured = 1")
    suspend fun featured(): List<Poem>

    @Query("SELECT * FROM poems")
    suspend fun all(): List<Poem>
}
```

`data/db/FavoriteDao.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Favorite>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE poemId = :poemId)")
    fun observeIsFavorite(poemId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE poemId = :poemId")
    suspend fun remove(poemId: Long)
}
```
(接口方法按上面 Produces 签名暴露:`add(poemId: Long)` 由 Repository 包一层 `Favorite(poemId, System.currentTimeMillis())`,DAO 本身收 `Favorite` 实体。)

`data/db/DailyRecordDao.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun getByDate(date: String): DailyRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DailyRecord)

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyRecord>>
}
```

`data/db/QuizStateDao.kt`:
```kotlin
package com.shiyun.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizStateDao {
    @Query("SELECT * FROM quiz_states")
    fun observeAll(): Flow<List<QuizState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: QuizState)
}
```

`data/db/ShiyunDatabase.kt`:
```kotlin
package com.shiyun.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Poem::class, PoemFts::class, Favorite::class, DailyRecord::class, QuizState::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShiyunDatabase : RoomDatabase() {
    abstract fun poemDao(): PoemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun quizStateDao(): QuizStateDao

    companion object {
        fun build(context: Context): ShiyunDatabase =
            Room.databaseBuilder(context, ShiyunDatabase::class.java, "shiyun.db").build()
    }
}
```

- [ ] **Step 4: 运行 MatchQueryBuilder 测试确认通过**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.db.MatchQueryBuilderTest"`
Expected: PASS(5 个用例)

- [ ] **Step 5: 写 DAO 的 Robolectric 测试**

`app/src/test/java/com/shiyun/app/data/db/PoemDaoTest.kt`:
```kotlin
package com.shiyun.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoemDaoTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: PoemDao

    private fun poem(id: Long, title: String, author: String, lines: List<String>, dynasty: String = "唐", kind: String = "诗", featured: Boolean = false) =
        Poem(
            id = id, title = title, dynasty = dynasty, author = author,
            paragraphs = lines, kind = kind, translation = null, notes = null,
            appreciation = null, tags = emptyList(), difficulty = 1, featured = featured,
            searchText = MatchQueryBuilder.segment(title + author + lines.joinToString("")),
        )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.poemDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `插入后按单字检索命中诗句`() = runTest {
        dao.insertAll(listOf(poem(1, "静夜思", "李白", listOf("床前明月光", "疑是地上霜"))))
        assertEquals(1, dao.count())
        val hits = dao.searchFiltered(MatchQueryBuilder.build("月光")!!, null, null)
        assertEquals(1, hits.size)
        assertEquals("静夜思", hits[0].title)
    }

    @Test
    fun `未命中词返回空`() = runTest {
        dao.insertAll(listOf(poem(1, "静夜思", "李白", listOf("床前明月光"))))
        assertTrue(dao.searchFiltered(MatchQueryBuilder.build("黄河")!!, null, null).isEmpty())
    }

    @Test
    fun `朝代与类型过滤生效`() = runTest {
        dao.insertAll(
            listOf(
                poem(1, "水调歌头", "苏轼", listOf("明月几时有"), dynasty = "宋", kind = "词"),
                poem(2, "春晓", "孟浩然", listOf("春眠不觉晓"), dynasty = "唐", kind = "诗"),
            )
        )
        val hits = dao.searchFiltered(MatchQueryBuilder.build("明月")!!, "宋", "词")
        assertEquals(listOf("水调歌头"), hits.map { it.title })
        assertEquals(listOf("春晓"), dao.browse("唐", "诗").map { it.title })
    }

    @Test
    fun `按 id 观察与读取`() = runTest {
        dao.insertAll(listOf(poem(7, "春晓", "孟浩然", listOf("春眠不觉晓"))))
        assertEquals("春晓", dao.observeById(7).first()!!.title)
        assertEquals("春晓", dao.getById(7)!!.title)
    }
}
```

- [ ] **Step 6: 运行 DAO 测试确认通过**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.db.PoemDaoTest"`
Expected: PASS(4 个用例)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: Room 数据层(实体/DAO/FTS4 全文检索)"
```

---

### Task 3: 数据管线脚本与 assets/poems.json

**Files:**
- Create: `tools/build_dataset.py`、`tools/test_build_dataset.py`、`app/src/main/assets/poems.json`(脚本产物)、`.gitignore` 追加 `tools/chinese-poetry/`
- Modify: 无

**Interfaces:**
- Consumes: chinese-poetry 数据集(git clone,仅管线期)
- Produces: `app/src/main/assets/poems.json`,格式为 JSON 数组,元素字段与 Task 2 的 `Poem` 一致(id/title/dynasty/author/paragraphs/kind/translation/notes/appreciation/tags/difficulty/featured,**不含** searchText——它由导入器计算);`tools/build_dataset.py --check` 输出统计并校验模式。

- [ ] **Step 1: 写失败测试**

`tools/test_build_dataset.py`:
```python
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from build_dataset import dedupe, make_id, normalize_record

def test_make_id_stable_and_positive():
    a = make_id("静夜思", "李白", "床前明月光")
    b = make_id("静夜思", "李白", "床前明月光")
    assert a == b and a > 0

def test_make_id_differs():
    assert make_id("静夜思", "李白", "床前明月光") != make_id("春晓", "孟浩然", "春眠不觉晓")

def test_normalize_record_defaults():
    rec = normalize_record({"title": "x", "paragraphs": ["a"]}, dynasty="唐", kind="诗", featured=False)
    assert rec["author"] == "佚名" and rec["difficulty"] == 1 and rec["tags"] == []

def test_dedupe_keeps_first():
    r1 = normalize_record({"title": "x", "author": "a", "paragraphs": ["p"]}, "唐", "诗", False)
    r2 = normalize_record({"title": "x", "author": "a", "paragraphs": ["p"]}, "唐", "诗", False)
    assert dedupe([r1, r2]) == [r1]
```

- [ ] **Step 2: 运行确认失败**

Run: `python -m pytest tools/test_build_dataset.py -v`(pytest 缺失则 `python -m pip install pytest`)
Expected: FAIL `ModuleNotFoundError: build_dataset`

- [ ] **Step 3: 实现脚本**

`tools/build_dataset.py`:
```python
"""从 chinese-poetry 数据集生成 assets/poems.json。用法:
  python tools/build_dataset.py [--check]
数据集仓库克隆到 tools/chinese-poetry(已 gitignore)。
"""
import argparse
import hashlib
import json
import random
import re
import sys
from pathlib import Path

REPO_DIR = Path(__file__).parent / "chinese-poetry"
OUT_PATH = Path(__file__).parent.parent / "app/src/main/assets/poems.json"
PUNCT = re.compile(r"[、。,;:!?「」『』《》()\[\]·\s]")

# (模糊匹配的文件名片段, 朝代, 类型, featured, 采样上限)
SOURCES = [
    ("唐诗三百首", "唐", "诗", True, 0),        # 0 = 全量
    ("宋词三百首", "宋", "词", True, 0),
    ("shijing", "先秦", "诗", True, 60),
    ("chuci", "先秦", "诗", False, 40),
    ("宋诗", "宋", "诗", False, 300),
    ("全唐诗", "唐", "诗", False, 600),
    ("全宋词", "宋", "词", False, 300),
    ("yuanqu", "元", "词", False, 200),
    ("明诗", "明清", "诗", False, 200),
    ("清诗", "明清", "诗", False, 200),
    ("小学", "唐", "诗", True, 0),
    ("初中", "唐", "诗", True, 0),
    ("高中", "唐", "诗", True, 0),
]


def make_id(title: str, author: str, first_line: str) -> int:
    digest = hashlib.sha1(f"{title}|{author}|{first_line}".encode("utf-8")).hexdigest()
    return int(digest[:16], 16) % (2 ** 62)


def dedupe_key(rec: dict) -> tuple:
    first = PUNCT.sub("", rec["paragraphs"][0]) if rec["paragraphs"] else ""
    return (rec["title"], rec["author"], first)


def normalize_record(raw: dict, dynasty: str, kind: str, featured: bool) -> dict:
    title = (raw.get("title") or raw.get("name") or raw.get("chapter") or "").strip()
    paragraphs = raw.get("paragraphs") or raw.get("content") or []
    paragraphs = [" ".join(line) if isinstance(line, list) else line for line in paragraphs]
    return {
        "title": title,
        "dynasty": raw.get("dynasty") or dynasty,
        "author": (raw.get("author") or "佚名").strip(),
        "paragraphs": paragraphs,
        "kind": kind,
        "translation": raw.get("translation"),
        "notes": raw.get("notes"),
        "appreciation": raw.get("appreciation"),
        "tags": [],
        "difficulty": 1,
        "featured": featured,
    }


def dedupe(records: list[dict]) -> list[dict]:
    seen, out = set(), []
    for rec in records:
        key = dedupe_key(rec)
        if key not in seen:
            seen.add(key)
            out.append(rec)
    return out


def collect_files(dataset: Path, fragment: str) -> list[Path]:
    return sorted(p for p in dataset.rglob("*.json") if fragment in p.name)


def load_source(dataset: Path, fragment: str, dynasty: str, kind: str, featured: bool, cap: int, rng: random.Random) -> list[dict]:
    files = collect_files(dataset, fragment)
    records = []
    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if not isinstance(data, list):
            continue
        records.extend(normalize_record(raw, dynasty, kind, featured) for raw in data if isinstance(raw, dict))
    if cap and len(records) > cap:
        records = rng.sample(records, cap)
    return records


def build(dataset_dir: Path) -> list[dict]:
    if not dataset_dir.exists():
        sys.exit(f"数据集不存在: {dataset_dir},请先 git clone --depth 1 https://github.com/chinese-poetry/chinese-poetry {dataset_dir}")
    rng = random.Random(42)
    records = []
    for fragment, dynasty, kind, featured, cap in SOURCES:
        records.extend(load_source(dataset_dir, fragment, dynasty, kind, featured, cap, rng))
    records = dedupe(records)
    for rec in records:
        first = PUNCT.sub("", rec["paragraphs"][0]) if rec["paragraphs"] else ""
        rec["id"] = make_id(rec["title"], rec["author"], first)
    # featured 池超过 400 时按 id 截断,保证每日一诗池稳定
    featured = [r for r in records if r["featured"]]
    if len(featured) > 400:
        drop = {r["id"] for r in sorted(featured, key=lambda r: r["id"])[400:]}
        for r in records:
            if r["id"] in drop:
                r["featured"] = False
    return records


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="仅统计并校验现有产物")
    args = parser.parse_args()
    if args.check:
        data = json.loads(OUT_PATH.read_text(encoding="utf-8"))
        fields = {"id", "title", "dynasty", "author", "paragraphs", "kind",
                  "translation", "notes", "appreciation", "tags", "difficulty", "featured"}
        for rec in data:
            assert set(rec) == fields, f"字段不符: {set(rec) ^ fields}"
            assert isinstance(rec["id"], int) and rec["paragraphs"], rec["title"]
        print(f"OK: {len(data)} 首, featured {sum(1 for r in data if r['featured'])} 首")
        return
    records = build(REPO_DIR)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(json.dumps(records, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"写出 {len(records)} 首到 {OUT_PATH}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: 运行测试与生成数据**

```bash
python -m pytest tools/test_build_dataset.py -v   # Expected: PASS(4 个用例)
git clone --depth 1 https://github.com/chinese-poetry/chinese-poetry tools/chinese-poetry
python tools/build_dataset.py
python tools/build_dataset.py --check              # Expected: OK: N 首(1000~3000 区间)
```
若产出 <1000 首:检查哪些 SOURCES 文件片段没匹配到(`collect_files` 加临时打印),修正片段名后重跑;规模异常偏大(>3000)则调低 `全唐诗/全宋词` cap。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: 数据管线脚本与内置诗词数据(约2000首)"
```

---

### Task 4: 首启导入器与 PoemRepository

**Files:**
- Create: `app/src/main/java/com/shiyun/app/data/assets/AssetsPoemImporter.kt`、`app/src/main/java/com/shiyun/app/data/repository/PoemRepository.kt`
- Modify: `app/src/main/java/com/shiyun/app/ShiyunApp.kt`(挂 AppContainer 与导入)
- Test: `app/src/test/java/com/shiyun/app/data/repository/PoemRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 2 的 `PoemDao`/`MatchQueryBuilder`/`ShiyunDatabase`
- Produces:
  - `class AppContainer(context: Context)` 暴露 `val poemRepository: PoemRepository`、`val database: ShiyunDatabase`(Task 6/7 扩展 favoriteRepository/dailyRepository/quizRepository)
  - `class PoemRepository(dao: PoemDao, importer: AssetsPoemImporter, io: CoroutineDispatcher = Dispatchers.IO)`:
    - `suspend fun importIfNeeded()`、`fun observePoem(id: Long): Flow<Poem?>`、`suspend fun search(raw: String, dynasty: String?, kind: String?): List<Poem>`、`suspend fun browse(dynasty: String?, kind: String?): List<Poem>`、`suspend fun featured(): List<Poem>`、`suspend fun all(): List<Poem>`

- [ ] **Step 1: 写失败测试**

`app/src/test/java/com/shiyun/app/data/repository/PoemRepositoryTest.kt`:
```kotlin
package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoemRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: PoemDao
    private lateinit var repo: PoemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyunDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.poemDao()
        repo = PoemRepository(dao, AssetsPoemImporter(context), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `search 委托 FTS 并空查询返回空`() = runTest {
        dao.insertAll(
            listOf(
                Poem(1, "静夜思", "唐", "李白", listOf("床前明月光"), "诗", null, null, null, emptyList(), 1, false,
                    MatchQueryBuilder.segment("静夜思李白床前明月光")),
            )
        )
        assertEquals(1, repo.search("月光", null, null).size)
        assertTrue(repo.search("  ", null, null).isEmpty())
    }

    @Test
    fun `browse 透传过滤参数`() = runTest {
        dao.insertAll(
            listOf(
                Poem(1, "春晓", "唐", "孟浩然", listOf("春眠不觉晓"), "诗", null, null, null, emptyList(), 1, false, "春晓孟浩然春眠不觉晓"),
                Poem(2, "水调歌头", "宋", "苏轼", listOf("明月几时有"), "词", null, null, null, emptyList(), 1, false, "水调歌头苏轼明月几时有"),
            )
        )
        assertEquals(listOf("春晓"), repo.browse("唐", "诗").map { it.title })
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.repository.PoemRepositoryTest"`
Expected: FAIL 类未定义

- [ ] **Step 3: 实现导入器、Repository、AppContainer**

`data/assets/AssetsPoemImporter.kt`:
```kotlin
package com.shiyun.app.data.assets

import android.content.Context
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PoemJson(
    val id: Long,
    val title: String,
    val dynasty: String,
    val author: String,
    val paragraphs: List<String>,
    val kind: String,
    val translation: String? = null,
    val notes: String? = null,
    val appreciation: String? = null,
    val tags: List<String> = emptyList(),
    val difficulty: Int = 1,
    val featured: Boolean = false,
)

class AssetsPoemImporter(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importInto(dao: PoemDao) {
        val raw = context.assets.open("poems.json").bufferedReader().use { it.readText() }
        val poems = json.decodeFromString<List<PoemJson>>(raw).map { it.toEntity() }
        dao.insertAll(poems)
    }

    private fun PoemJson.toEntity() = Poem(
        id = id, title = title, dynasty = dynasty, author = author,
        paragraphs = paragraphs, kind = kind, translation = translation,
        notes = notes, appreciation = appreciation, tags = tags,
        difficulty = difficulty, featured = featured,
        searchText = MatchQueryBuilder.segment(title + author + paragraphs.joinToString("")),
    )
}
```

`data/repository/PoemRepository.kt`:
```kotlin
package com.shiyun.app.data.repository

import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.MatchQueryBuilder
import com.shiyun.app.data.db.Poem
import com.shiyun.app.data.db.PoemDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PoemRepository(
    private val dao: PoemDao,
    private val importer: AssetsPoemImporter,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun importIfNeeded() = withContext(io) {
        if (dao.count() == 0) importer.importInto(dao)
    }

    fun observePoem(id: Long): Flow<Poem?> = dao.observeById(id)

    suspend fun search(raw: String, dynasty: String?, kind: String?): List<Poem> = withContext(io) {
        MatchQueryBuilder.build(raw)?.let { dao.searchFiltered(it, dynasty, kind) } ?: emptyList()
    }

    suspend fun browse(dynasty: String?, kind: String?): List<Poem> = withContext(io) {
        dao.browse(dynasty, kind)
    }

    suspend fun featured(): List<Poem> = withContext(io) { dao.featured() }

    suspend fun all(): List<Poem> = withContext(io) { dao.all() }
}
```

修改 `ShiyunApp.kt`:
```kotlin
package com.shiyun.app

import android.app.Application
import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.ShiyunDatabase
import com.shiyun.app.data.repository.PoemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: android.content.Context) {
    val database: ShiyunDatabase = ShiyunDatabase.build(context)
    val poemRepository = PoemRepository(database.poemDao(), AssetsPoemImporter(context))
}

class ShiyunApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.poemRepository.importIfNeeded() }
    }
}
```

- [ ] **Step 4: 运行测试确认通过 + 构建**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: 首启 assets 导入与 PoemRepository"
```

---

### Task 5: DailyPoemSelector 与打卡

**Files:**
- Create: `app/src/main/java/com/shiyun/app/domain/DailyPoemSelector.kt`、`app/src/main/java/com/shiyun/app/data/repository/DailyRepository.kt`
- Test: `app/src/test/java/com/shiyun/app/domain/DailyPoemSelectorTest.kt`、`app/src/test/java/com/shiyun/app/data/repository/DailyRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 2 `DailyRecordDao`、Task 4 `PoemRepository`
- Produces:
  - `object DailyPoemSelector { fun select(pool: List<Poem>, date: LocalDate): Poem? }`
  - `class DailyRepository(dao: DailyRecordDao, io: CoroutineDispatcher = Dispatchers.IO)`:
    - `fun observeByDate(date: LocalDate): Flow<DailyRecord?>`、`fun observeStreak(): Flow<Int>`、`suspend fun checkIn(date: LocalDate, poemId: Long)`
  - 日期字符串格式固定 `yyyy-MM-dd`(`DateTimeFormatter.ISO_LOCAL_DATE`)

- [ ] **Step 1: 写 DailyPoemSelector 失败测试**

```kotlin
package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyPoemSelectorTest {
    private fun poem(id: Long) = Poem(id, "t$id", "唐", "a", listOf("x"), "诗", null, null, null, emptyList(), 1, true, "")

    @Test
    fun `同一天选取稳定`() {
        val pool = (1L..50L).map(::poem)
        val date = LocalDate.of(2026, 9, 11)
        assertEquals(DailyPoemSelector.select(pool, date), DailyPoemSelector.select(pool, date))
    }

    @Test
    fun `选取结果在池内且跨日可变`() {
        val pool = (1L..50L).map(::poem)
        val picks = (0L..30L).map { DailyPoemSelector.select(pool, LocalDate.of(2026, 1, 1).plusDays(it))!!.id }
        assertTrue(picks.all { it in 1L..50L })
        assertTrue(picks.distinct().size > 1)
    }

    @Test
    fun `空池返回 null`() {
        assertNull(DailyPoemSelector.select(emptyList(), LocalDate.of(2026, 9, 11)))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.domain.DailyPoemSelectorTest"`
Expected: FAIL 类未定义

- [ ] **Step 3: 实现选择器与 DailyRepository**

`domain/DailyPoemSelector.kt`:
```kotlin
package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import java.time.LocalDate

object DailyPoemSelector {
    fun select(pool: List<Poem>, date: LocalDate): Poem? {
        if (pool.isEmpty()) return null
        val index = Math.floorMod(date.toEpochDay() * 7919L, pool.size).toInt()
        return pool[index]
    }
}
```

`data/repository/DailyRepository.kt`:
```kotlin
package com.shiyun.app.data.repository

import com.shiyun.app.data.db.DailyRecord
import com.shiyun.app.data.db.DailyRecordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DailyRepository(
    private val dao: DailyRecordDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeByDate(date: LocalDate): Flow<DailyRecord?> = dao.observeByDate(date.toString())

    fun observeStreak(): Flow<Int> = dao.observeAll().map { records ->
        val dates = records.filter { it.checkedIn }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        var cursor = LocalDate.now()
        if (cursor !in dates) cursor = cursor.minusDays(1)   // 今天未打卡不中断连击
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        streak
    }

    suspend fun checkIn(date: LocalDate, poemId: Long) = withContext(io) {
        dao.upsert(DailyRecord(date.toString(), poemId, checkedIn = true))
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.domain.DailyPoemSelectorTest" --tests "com.shiyun.app.data.repository.DailyRepositoryTest"`
Expected: DailyPoemSelectorTest PASS(DailyRepositoryTest 尚未写,先跳过该过滤器)

- [ ] **Step 5: 写 DailyRepository 失败测试再实现验证**

`app/src/test/java/com/shiyun/app/data/repository/DailyRepositoryTest.kt`:
```kotlin
package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.DailyRecordDao
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DailyRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var dao: DailyRecordDao
    private lateinit var repo: DailyRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.dailyRecordDao()
        repo = DailyRepository(dao, kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `打卡幂等且可观察`() = runTest {
        val today = LocalDate.of(2026, 9, 11)
        repo.checkIn(today, poemId = 1)
        repo.checkIn(today, poemId = 1)
        val record = repo.observeByDate(today).first()!!
        assertEquals(1, record.poemId)
        assertTrue(record.checkedIn)
        assertFalse(dao.getByDate(today.toString()) == null)
    }

    @Test
    fun `连击计算包含今天与历史`() = runTest {
        val today = LocalDate.now()
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.toString(), 1, true))
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.minusDays(1).toString(), 1, true))
        dao.upsert(com.shiyun.app.data.db.DailyRecord(today.minusDays(3).toString(), 1, true))
        assertEquals(2, repo.observeStreak().first())
    }
}
```

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.repository.DailyRepositoryTest"`
Expected: PASS(2 个用例)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: 每日选诗算法与打卡存储"
```

---

### Task 6: 收藏与闯关进度仓库

**Files:**
- Create: `app/src/main/java/com/shiyun/app/data/repository/FavoriteRepository.kt`、`app/src/main/java/com/shiyun/app/data/repository/QuizRepository.kt`
- Modify: `app/src/main/java/com/shiyun/app/ShiyunApp.kt`(AppContainer 增加三个仓库)
- Test: `app/src/test/java/com/shiyun/app/data/repository/FavoriteRepositoryTest.kt`、`app/src/test/java/com/shiyun/app/data/repository/QuizRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 2 `FavoriteDao`/`QuizStateDao`
- Produces:
  - `class FavoriteRepository(dao: FavoriteDao, io: CoroutineDispatcher = Dispatchers.IO)`:
    - `fun observeAll(): Flow<List<Favorite>>`、`fun observeIsFavorite(poemId: Long): Flow<Boolean>`、`suspend fun toggle(poemId: Long)`(已收藏则取消,否则加入)
  - `class QuizRepository(dao: QuizStateDao, io: CoroutineDispatcher = Dispatchers.IO)`:
    - `fun observeStates(): Flow<List<QuizState>>`、`suspend fun saveResult(levelId: String, correct: Int)`(按 Global Constraints 星级规则:`stars = if (correct == 5) 3 else if (correct == 4) 2 else 0`)
  - `AppContainer` 新增 `val favoriteRepository: FavoriteRepository`、`val dailyRepository: DailyRepository`、`val quizRepository: QuizRepository`

- [ ] **Step 1: 写两个失败测试**

`FavoriteRepositoryTest.kt`:
```kotlin
package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var repo: FavoriteRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = FavoriteRepository(db.favoriteDao(), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `toggle 收藏与取消`() = runTest {
        repo.toggle(1)
        assertTrue(repo.observeIsFavorite(1).first())
        repo.toggle(1)
        assertFalse(repo.observeIsFavorite(1).first())
    }

    @Test
    fun `列表按收藏时间倒序`() = runTest {
        repo.toggle(1)
        Thread.sleep(5)
        repo.toggle(2)
        assertEquals(listOf(2L, 1L), repo.observeAll().first().map { it.poemId })
    }
}
```

`QuizRepositoryTest.kt`:
```kotlin
package com.shiyun.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyun.app.data.db.ShiyunDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuizRepositoryTest {
    private lateinit var db: ShiyunDatabase
    private lateinit var repo: QuizRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ShiyunDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = QuizRepository(db.quizStateDao(), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `星级按答对数映射且保留最高记录`() = runTest {
        repo.saveResult("唐", correct = 5)
        repo.saveResult("唐", correct = 4)
        assertEquals(3, repo.observeStates().first().first { it.levelId == "唐" }.stars)
    }

    @Test
    fun `未过关记录零星`() = runTest {
        repo.saveResult("唐", correct = 2)
        assertEquals(0, repo.observeStates().first().first().stars)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.data.repository.FavoriteRepositoryTest" --tests "com.shiyun.app.data.repository.QuizRepositoryTest"`
Expected: FAIL 类未定义

- [ ] **Step 3: 实现两个仓库并挂到 AppContainer**

`data/repository/FavoriteRepository.kt`:
```kotlin
package com.shiyun.app.data.repository

import com.shiyun.app.data.db.Favorite
import com.shiyun.app.data.db.FavoriteDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoriteRepository(
    private val dao: FavoriteDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<Favorite>> = dao.observeAll()

    fun observeIsFavorite(poemId: Long): Flow<Boolean> = dao.observeIsFavorite(poemId)

    suspend fun toggle(poemId: Long) = withContext(io) {
        if (dao.isFavorite(poemId)) dao.remove(poemId)
        else dao.add(Favorite(poemId, System.currentTimeMillis()))
    }
}
```
注意:`toggle` 依赖同步查询,需给 `FavoriteDao` 追加:
```kotlin
@Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE poemId = :poemId)")
suspend fun isFavorite(poemId: Long): Boolean
```

`data/repository/QuizRepository.kt`:
```kotlin
package com.shiyun.app.data.repository

import com.shiyun.app.data.db.QuizState
import com.shiyun.app.data.db.QuizStateDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class QuizRepository(
    private val dao: QuizStateDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeStates(): Flow<List<QuizState>> = dao.observeAll()

    suspend fun saveResult(levelId: String, correct: Int) = withContext(io) {
        val stars = when (correct) {
            5 -> 3
            4 -> 2
            else -> 0
        }
        val existing = dao.observeAll().first().firstOrNull { it.levelId == levelId }
        val best = maxOf(existing?.stars ?: 0, stars)
        dao.upsert(QuizState(levelId, best, unlocked = true))
    }
}
```
(`QuizRepository` 顶部补 `import kotlinx.coroutines.flow.first`。)

修改 `AppContainer`(ShiyunApp.kt):
```kotlin
class AppContainer(context: android.content.Context) {
    val database: ShiyunDatabase = ShiyunDatabase.build(context)
    val poemRepository = PoemRepository(database.poemDao(), AssetsPoemImporter(context))
    val favoriteRepository = FavoriteRepository(database.favoriteDao())
    val dailyRepository = DailyRepository(database.dailyRecordDao())
    val quizRepository = QuizRepository(database.quizStateDao())
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: 收藏与闯关进度仓库"
```

---

### Task 7: QuizGenerator 出题器与朝代关卡

**Files:**
- Create: `app/src/main/java/com/shiyun/app/domain/QuizQuestion.kt`、`app/src/main/java/com/shiyun/app/domain/DynastyLevels.kt`、`app/src/main/java/com/shiyun/app/domain/QuizGenerator.kt`
- Test: `app/src/test/java/com/shiyun/app/domain/QuizGeneratorTest.kt`

**Interfaces:**
- Consumes: Task 2 `Poem`
- Produces:
  ```kotlin
  sealed interface QuizQuestion {
      data class FillBlank(val sentence: String, val blankIndex: Int, val answer: String, val options: List<String>): QuizQuestion
      data class NextLine(val line: String, val answer: String, val options: List<String>): QuizQuestion
      data class AuthorAttribution(val line: String, val answer: String, val options: List<String>): QuizQuestion
  }
  object DynastyLevels {
      val ORDER: List<String>  // ["先秦","汉","魏晋","唐","宋","元","明清"]
      fun available(poems: List<Poem>): List<String>
  }
  class QuizGenerator(random: Random = Random(42)) {
      fun generate(levelPoems: List<Poem>, distractorPool: List<Poem>, count: Int = 5): List<QuizQuestion>
  }
  ```
- 选项约定:`options.size == 4`、互不相同、包含 `answer`、已打乱。

- [ ] **Step 1: 写失败测试**

`app/src/test/java/com/shiyun/app/domain/QuizGeneratorTest.kt`:
```kotlin
package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGeneratorTest {
    private fun poem(id: Long, author: String, lines: List<String>, dynasty: String = "唐") = Poem(
        id, "诗$id", dynasty, author, lines, "诗", null, null, null, emptyList(), 1, false,
        lines.joinToString(""),
    )

    private val pool = listOf(
        poem(1, "李白", listOf("床前明月光", "疑是地上霜", "举头望明月", "低头思故乡")),
        poem(2, "孟浩然", listOf("春眠不觉晓", "处处闻啼鸟", "夜来风雨声", "花落知多少")),
        poem(3, "王维", listOf("空山新雨后", "天气晚来秋", "明月松间照", "清泉石上流")),
        poem(4, "杜甫", listOf("好雨知时节", "当春乃发生", "随风潜入夜", "润物细无声")),
        poem(5, "白居易", listOf("离离原上草", "一岁一枯荣", "野火烧不尽", "春风吹又生")),
    )
    private val generator = QuizGenerator(java.util.Random(7))

    @Test
    fun `生成指定数量题目且选项合规`() {
        val questions = generator.generate(pool, pool)
        assertEquals(5, questions.size)
        for (q in questions) {
            val options = when (q) {
                is QuizQuestion.FillBlank -> q.options
                is QuizQuestion.NextLine -> q.options
                is QuizQuestion.AuthorAttribution -> q.options
            }
            val answer = when (q) {
                is QuizQuestion.FillBlank -> q.answer
                is QuizQuestion.NextLine -> q.answer
                is QuizQuestion.AuthorAttribution -> q.answer
            }
            assertEquals(4, options.size)
            assertEquals(options.size, options.distinct().size)
            assertTrue(answer in options)
        }
    }

    @Test
    fun `素材充足时三种题型都出现`() {
        val types = (1..20).flatMap { generator.generate(pool, pool) }.map {
            when (it) {
                is QuizQuestion.FillBlank -> "F"
                is QuizQuestion.NextLine -> "N"
                is QuizQuestion.AuthorAttribution -> "A"
            }
        }.toSet()
        assertEquals(setOf("F", "N", "A"), types)
    }

    @Test
    fun `素材不足降级为作者题`() {
        val questions = generator.generate(listOf(pool[0]), pool)
        assertTrue(questions.all { it is QuizQuestion.AuthorAttribution })
    }

    @Test
    fun `DynastyLevels 只含不少于五首的朝代`() {
        val poems = pool + (6L..10L).map { poem(it, "杜牧", listOf("清明时节雨纷纷"), dynasty = "宋") }
        assertEquals(listOf("唐", "宋"), DynastyLevels.available(poems))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.domain.QuizGeneratorTest"`
Expected: FAIL 类未定义

- [ ] **Step 3: 实现 QuizQuestion、DynastyLevels、QuizGenerator**

`domain/QuizQuestion.kt`:
```kotlin
package com.shiyun.app.domain

sealed interface QuizQuestion {
    data class FillBlank(val sentence: String, val blankIndex: Int, val answer: String, val options: List<String>) : QuizQuestion
    data class NextLine(val line: String, val answer: String, val options: List<String>) : QuizQuestion
    data class AuthorAttribution(val line: String, val answer: String, val options: List<String>) : QuizQuestion
}
```

`domain/DynastyLevels.kt`:
```kotlin
package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem

object DynastyLevels {
    val ORDER = listOf("先秦", "汉", "魏晋", "唐", "宋", "元", "明清")
    const val MIN_POEMS_PER_LEVEL = 5

    fun available(poems: List<Poem>): List<String> =
        ORDER.filter { dynasty -> poems.count { it.dynasty == dynasty } >= MIN_POEMS_PER_LEVEL }
}
```

`domain/QuizGenerator.kt`:
```kotlin
package com.shiyun.app.domain

import com.shiyun.app.data.db.Poem
import kotlin.random.Random

class QuizGenerator(private val random: Random = Random(42)) {

    fun generate(levelPoems: List<Poem>, distractorPool: List<Poem>, count: Int = 5): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        val types = listOf(Type.FILL, Type.NEXT, Type.AUTHOR).shuffled(random)
        for (i in 0 until count) {
            val question = buildInOrder(levelPoems, distractorPool, listOf(types[i % types.size], Type.AUTHOR))
            questions += question
        }
        return questions
    }

    private fun buildInOrder(levelPoems: List<Poem>, pool: List<Poem>, types: List<Type>): QuizQuestion {
        for (type in types) {
            when (type) {
                Type.FILL -> fillBlank(levelPoems, pool)?.let { return it }
                Type.NEXT -> nextLine(levelPoems, pool)?.let { return it }
                Type.AUTHOR -> authorQuestion(levelPoems, pool)?.let { return it }
            }
        }
        // 完全无素材:占位作者题
        return authorQuestion(levelPoems, pool)!!
    }

    private fun options(answer: String, distractors: List<String>): List<String> =
        (listOf(answer) + distractors.filter { it != answer }.distinct().take(3))
            .shuffled(random)

    private fun fillBlank(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        val chars = pool.asSequence().flatMap { it.paragraphs.asSequence() }
            .flatMap { it.asSequence() }.filter { it.isLetterOrDigit() }.toSet().toList()
        if (chars.size < 4) return null
        val candidates = poems.asSequence()
            .flatMap { it.paragraphs.asSequence() }
            .map { it.filter { c -> c.isLetterOrDigit() } }
            .filter { it.length >= 4 }
            .toList()
        if (candidates.isEmpty()) return null
        val sentence = candidates[random.nextInt(candidates.size)]
        val blankIndex = random.nextInt(sentence.length)
        val answer = sentence[blankIndex].toString()
        val distractors = buildList {
            while (size < 3) {
                val c = chars[random.nextInt(chars.size)].toString()
                if (c != answer && c !in this) add(c)
            }
        }
        return QuizQuestion.FillBlank(sentence, blankIndex, answer, options(answer, distractors))
    }

    private fun nextLine(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        val pairs = poems.asSequence().flatMap { poem ->
            poem.paragraphs.zipWithNext().map { (a, b) -> a to b }
        }.toList()
        if (pairs.isEmpty()) return null
        val (line, answer) = pairs[random.nextInt(pairs.size)]
        val otherLines = pool.asSequence().flatMap { it.paragraphs.asSequence() }
            .filter { it != answer && it != line }.distinct().toList()
        if (otherLines.size < 3) return null
        val distractors = List(3) { otherLines[random.nextInt(otherLines.size)] }
        return QuizQuestion.NextLine(line, answer, options(answer, distractors))
    }

    private fun authorQuestion(poems: List<Poem>, pool: List<Poem>): QuizQuestion? {
        if (poems.isEmpty()) return null
        val poem = poems[random.nextInt(poems.size)]
        val answer = poem.author
        val others = pool.map { it.author }.filter { it != answer }.distinct()
        if (others.size < 3) return null
        val distractors = List(3) { others[random.nextInt(others.size)] }
        return QuizQuestion.AuthorAttribution(poem.paragraphs.firstOrNull() ?: poem.title, answer, options(answer, distractors))
    }

    private enum class Type { FILL, NEXT, AUTHOR }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.shiyun.app.domain.QuizGeneratorTest"`
Expected: PASS(4 个用例)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: 出题生成器与朝代关卡定义"
```

---

### Task 8: 导航骨架(四 Tab 空屏)

**Files:**
- Create: `app/src/main/java/com/shiyun/app/ui/navigation/ShiyunNavHost.kt`、`ui/home/HomeScreen.kt`、`ui/library/LibraryScreen.kt`、`ui/favorites/FavoritesScreen.kt`、`ui/quiz/QuizScreen.kt`(本任务均为占位空屏,后续任务填充)
- Modify: `app/src/main/java/com/shiyun/app/MainActivity.kt`

**Interfaces:**
- Consumes: Task 1 `ShiyunTheme`
- Produces:
  - 路由常量:`object Routes { const val HOME = "home"; const val LIBRARY = "library"; const val FAVORITES = "favorites"; const val QUIZ = "quiz"; const val DETAIL = "poem/{poemId}"; fun detail(poemId: Long) = "poem/$poemId" }`
  - 空屏签名:`@Composable fun HomeScreen(...)` 等在各自任务中会替换内容,本任务仅 `Text` 占位。

- [ ] **Step 1: 实现导航与占位屏**

`ui/navigation/ShiyunNavHost.kt`:
```kotlin
package com.shiyun.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiyun.app.ui.favorites.FavoritesScreen
import com.shiyun.app.ui.home.HomeScreen
import com.shiyun.app.ui.library.LibraryScreen
import com.shiyun.app.ui.quiz.QuizScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val FAVORITES = "favorites"
    const val QUIZ = "quiz"
    const val DETAIL = "poem/{poemId}"
    fun detail(poemId: Long) = "poem/$poemId"
}

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun ShiyunNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab(Routes.HOME, "今日") { Icon(Icons.Default.Home, null) },
        Tab(Routes.LIBRARY, "诗词库") { Icon(Icons.Default.MenuBook, null) },
        Tab(Routes.FAVORITES, "收藏") { Icon(Icons.Default.Favorite, null) },
        Tab(Routes.QUIZ, "闯关") { Icon(Icons.Default.SportsEsports, null) },
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBar = tabs.any { currentDestination?.hierarchy?.any { d -> d.route == it.route } == true }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.LIBRARY) { LibraryScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.FAVORITES) { FavoritesScreen(onOpenPoem = { navController.navigate(Routes.detail(it)) }) }
            composable(Routes.QUIZ) { QuizScreen() }
            composable(Routes.DETAIL) { backStackEntry ->
                val poemId = backStackEntry.arguments?.getString("poemId")?.toLongOrNull() ?: 0L
                com.shiyun.app.ui.detail.DetailScreen(poemId = poemId, onBack = { navController.popBackStack() })
            }
        }
    }
}
```

四个占位屏(同模式,内容 `Text("开发中")`):
```kotlin
// ui/home/HomeScreen.kt
package com.shiyun.app.ui.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(onOpenPoem: (Long) -> Unit) {
    Text("今日一诗")
}
```
`LibraryScreen(onOpenPoem: (Long) -> Unit)`、`FavoritesScreen(onOpenPoem: (Long) -> Unit)`、`QuizScreen()` 同样处理,包名对应各自目录。

修改 `MainActivity.kt`:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShiyunTheme { ShiyunNavHost() } }
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: 底部四 Tab 导航骨架"
```

---

### Task 9: 首页(每日一诗 + 打卡)

**Files:**
- Modify: `app/src/main/java/com/shiyun/app/ui/home/HomeScreen.kt`(替换占位)
- Create: `app/src/main/java/com/shiyun/app/ui/home/HomeViewModel.kt`

**Interfaces:**
- Consumes: Task 4 `AppContainer`、Task 5 `DailyPoemSelector`/`DailyRepository`、Task 7 无关
- Produces: `class HomeViewModel(container: AppContainer): ViewModel()`,状态 `data class HomeUiState(loading: Boolean = true, poem: Poem? = null, checkedIn: Boolean = false, streak: Int = 0)`,方法 `fun checkIn()`。详情页跳转回调 `onOpenPoem(poemId: Long)`。

- [ ] **Step 1: 实现 ViewModel**

`ui/home/HomeViewModel.kt`:
```kotlin
package com.shiyun.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import com.shiyun.app.domain.DailyPoemSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val poem: Poem? = null,
    val checkedIn: Boolean = false,
    val streak: Int = 0,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val today: LocalDate = LocalDate.now()
    private val poem = MutableStateFlow<Poem?>(null)

    val state: StateFlow<HomeUiState> =
        combine(poem, container.dailyRepository.observeByDate(today), container.dailyRepository.observeStreak()) { p, record, streak ->
            HomeUiState(loading = false, poem = p, checkedIn = record?.checkedIn == true, streak = streak)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            container.poemRepository.importIfNeeded()
            val pool = container.poemRepository.featured().ifEmpty { container.poemRepository.all() }
            poem.value = DailyPoemSelector.select(pool, today)
        }
    }

    fun checkIn() {
        val current = poem.value ?: return
        viewModelScope.launch { container.dailyRepository.checkIn(today, current.id) }
    }
}
```

- [ ] **Step 2: 实现界面**

`ui/home/HomeScreen.kt`:
```kotlin
package com.shiyun.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.AppContainer
import com.shiyun.app.ShiyunApp
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun HomeScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: HomeViewModel = viewModel { HomeViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    when {
        state.loading -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }

        else -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("今日一诗", style = MaterialTheme.typography.headlineMedium)
                Text("连续打卡 ${state.streak} 天", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
            }

            val poem = state.poem
            if (poem != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPoem(poem.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(poem.title, style = MaterialTheme.typography.titleLarge)
                        Text("${poem.dynasty} · ${poem.author}", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
                        poem.paragraphs.take(2).forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Button(
                    onClick = vm::checkIn,
                    enabled = !state.checkedIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(if (state.checkedIn) "今日已打卡" else "打卡")
                }
            }
        }
    }
}
```

- [ ] **Step 3: 构建验证**

Run: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL,测试全过

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: 首页每日一诗与打卡"
```

---

### Task 10: 诗词库(搜索 + 筛选)

**Files:**
- Modify: `app/src/main/java/com/shiyun/app/ui/library/LibraryScreen.kt`
- Create: `app/src/main/java/com/shiyun/app/ui/library/LibraryViewModel.kt`

**Interfaces:**
- Consumes: Task 4 `PoemRepository.search/browse`
- Produces: `class LibraryViewModel(container: AppContainer): ViewModel()`,状态 `data class LibraryUiState(query: String = "", dynasty: String? = null, kind: String? = null, results: List<Poem> = emptyList(), searched: Boolean = false)`,方法 `fun onQueryChange(String)`、`fun onDynastyChange(String?)`、`fun onKindChange(String?)`。空查询且无筛选时展示 `browse(null, null)` 兜底列表。

- [ ] **Step 1: 实现 ViewModel**

`ui/library/LibraryViewModel.kt`:
```kotlin
package com.shiyun.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val query: String = "",
    val dynasty: String? = null,
    val kind: String? = null,
    val results: List<Poem> = emptyList(),
    val searched: Boolean = false,
)

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    init {
        refresh()
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        refresh()
    }

    fun onDynastyChange(dynasty: String?) {
        _state.update { it.copy(dynasty = dynasty) }
        refresh()
    }

    fun onKindChange(kind: String?) {
        _state.update { it.copy(kind = kind) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val results = if (s.query.isBlank()) {
                container.poemRepository.browse(s.dynasty, s.kind)
            } else {
                container.poemRepository.search(s.query, s.dynasty, s.kind)
            }
            _state.update { it.copy(results = results, searched = true) }
        }
    }
}
```

- [ ] **Step 2: 实现界面**

`ui/library/LibraryScreen.kt`:
```kotlin
package com.shiyun.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.data.db.Poem
import com.shiyun.app.ui.theme.FadedInk

private val DYNASTIES = listOf("先秦", "汉", "魏晋", "唐", "宋", "元", "明清")
private val KINDS = listOf("诗", "词")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: LibraryViewModel = viewModel { LibraryViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索标题、诗句或作者") },
            singleLine = true,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DYNASTIES.forEach { dynasty ->
                FilterChip(
                    selected = state.dynasty == dynasty,
                    onClick = { vm.onDynastyChange(if (state.dynasty == dynasty) null else dynasty) },
                    label = { Text(dynasty) },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KINDS.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { vm.onKindChange(if (state.kind == kind) null else kind) },
                    label = { Text(kind) },
                )
            }
        }

        if (state.results.isEmpty()) {
            Text(
                if (state.searched) "未找到匹配的诗词" else "加载中…",
                modifier = Modifier.padding(top = 32.dp),
                color = FadedInk,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.results, key = { it.id }) { poem ->
                    PoemListItem(poem, onClick = { onOpenPoem(poem.id) })
                }
            }
        }
    }
}

@Composable
private fun PoemListItem(poem: Poem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(poem.title, style = MaterialTheme.typography.titleMedium)
        Text("${poem.dynasty} · ${poem.author} · ${poem.paragraphs.firstOrNull().orEmpty()}",
            style = MaterialTheme.typography.bodyMedium, color = FadedInk)
    }
}
```

- [ ] **Step 3: 构建验证**

Run: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: 诗词库搜索与筛选页"
```

---

### Task 11: 详情页与收藏页

**Files:**
- Create: `app/src/main/java/com/shiyun/app/ui/detail/DetailScreen.kt`、`app/src/main/java/com/shiyun/app/ui/detail/DetailViewModel.kt`、`app/src/main/java/com/shiyun/app/ui/favorites/FavoritesViewModel.kt`
- Modify: `app/src/main/java/com/shiyun/app/ui/favorites/FavoritesScreen.kt`

**Interfaces:**
- Consumes: Task 4 `PoemRepository.observePoem`、Task 6 `FavoriteRepository`
- Produces:
  - `class DetailViewModel(container: AppContainer, poemId: Long): ViewModel()`,状态 `data class DetailUiState(poem: Poem? = null, isFavorite: Boolean = false)`,方法 `fun toggleFavorite()`
  - `class FavoritesViewModel(container: AppContainer): ViewModel()`,状态 `data class FavoritesUiState(poems: List<Poem> = emptyList())`,收藏实体 → 按时间倒序解析出 Poem 列表。

- [ ] **Step 1: 实现 DetailViewModel 与界面**

`ui/detail/DetailViewModel.kt`:
```kotlin
package com.shiyun.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(val poem: Poem? = null, val isFavorite: Boolean = false)

class DetailViewModel(private val container: AppContainer, poemId: Long) : ViewModel() {
    val state: StateFlow<DetailUiState> =
        combine(
            container.poemRepository.observePoem(poemId),
            container.favoriteRepository.observeIsFavorite(poemId),
        ) { poem, favorite ->
            DetailUiState(poem = poem, isFavorite = favorite)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun toggleFavorite() {
        val poem = state.value.poem ?: return
        viewModelScope.launch { container.favoriteRepository.toggle(poem.id) }
    }
}
```

`ui/detail/DetailScreen.kt`:
```kotlin
package com.shiyun.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun DetailScreen(poemId: Long, onBack: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: DetailViewModel = viewModel(key = "detail-$poemId") { DetailViewModel(app.container, poemId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val poem = state.poem ?: return

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("返回") }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
            IconButton(onClick = vm::toggleFavorite) {
                Icon(
                    if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(poem.title, style = MaterialTheme.typography.headlineMedium)
            Text("${poem.dynasty} · ${poem.author}", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(8.dp))
            poem.paragraphs.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Section("译文", poem.translation)
        Section("注释", poem.notes)
        Section("赏析", poem.appreciation)
    }
}

@Composable
private fun Section(title: String, body: String?) {
    if (body.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 2: 实现 FavoritesViewModel 与界面**

`ui/favorites/FavoritesViewModel.kt`:
```kotlin
package com.shiyun.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.data.db.Poem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FavoritesUiState(val poems: List<Poem> = emptyList())

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<FavoritesUiState> =
        container.favoriteRepository.observeAll().map { favorites ->
            FavoritesUiState(poems = favorites.mapNotNull { container.poemRepository.poemById(it.poemId) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())
}
```
需给 `PoemRepository` 追加:
```kotlin
suspend fun poemById(id: Long): Poem? = withContext(io) { dao.getById(id) }
```

`ui/favorites/FavoritesScreen.kt`:
```kotlin
package com.shiyun.app.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun FavoritesScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: FavoritesViewModel = viewModel { FavoritesViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("收藏", style = MaterialTheme.typography.headlineMedium)
        if (state.poems.isEmpty()) {
            Text("还没有收藏,去诗词库逛逛吧", color = FadedInk)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.poems, key = { it.id }) { poem ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPoem(poem.id) }.padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(poem.title, style = MaterialTheme.typography.titleMedium)
                        Text("${poem.dynasty} · ${poem.author}", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 构建验证**

Run: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: 诗词详情页与收藏页"
```

---

### Task 12: 闯关页(关卡列表 + 答题 + 结果)

**Files:**
- Create: `app/src/main/java/com/shiyun/app/ui/quiz/QuizViewModel.kt`
- Modify: `app/src/main/java/com/shiyun/app/ui/quiz/QuizScreen.kt`

**Interfaces:**
- Consumes: Task 4 `PoemRepository.all`、Task 6 `QuizRepository`、Task 7 `QuizGenerator`/`DynastyLevels`/`QuizQuestion`
- Produces:
  - `class QuizViewModel(container: AppContainer): ViewModel()`
  - 状态:`data class QuizUiState(levels: List<LevelUi> = emptyList(), active: QuizSession? = null)`
    `data class LevelUi(val dynasty: String, val unlocked: Boolean, val stars: Int)`
    `data class QuizSession(val dynasty: String, val questions: List<QuizQuestion>, val index: Int = 0, val correct: Int = 0, val selected: Int = -1, val finished: Boolean = false)`
  - 方法:`fun openLevel(dynasty: String)`、`fun selectOption(index: Int)`(判分并停 400ms 由 UI 处理视觉,状态即时更新)、`fun next()`(推进到下一题或结束)、`fun exitSession()`(回关卡列表)、`fun retry()`

- [ ] **Step 1: 实现 QuizViewModel**

`ui/quiz/QuizViewModel.kt`:
```kotlin
package com.shiyun.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyun.app.AppContainer
import com.shiyun.app.domain.DynastyLevels
import com.shiyun.app.domain.QuizGenerator
import com.shiyun.app.domain.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LevelUi(val dynasty: String, val unlocked: Boolean, val stars: Int)

data class QuizSession(
    val dynasty: String,
    val questions: List<QuizQuestion>,
    val index: Int = 0,
    val correct: Int = 0,
    val selected: Int = -1,
    val finished: Boolean = false,
)

data class QuizUiState(val levels: List<LevelUi> = emptyList(), val active: QuizSession? = null)

class QuizViewModel(private val container: AppContainer) : ViewModel() {
    private val generator = QuizGenerator()
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state

    init {
        viewModelScope.launch {
            container.poemRepository.importIfNeeded()
            container.poemRepository.all().let { poems -> rebuild(poems, emptyList()) }
            container.quizRepository.observeStates().collect { states ->
                _state.update { it.copy(levels = buildLevels(states)) }
            }
        }
    }

    private var allPoems: List<com.shiyun.app.data.db.Poem> = emptyList()

    private fun rebuild(poems: List<com.shiyun.app.data.db.Poem>, states: List<com.shiyun.app.data.db.QuizState>) {
        allPoems = poems
        _state.update { it.copy(levels = buildLevels(states)) }
    }

    private fun buildLevels(states: List<com.shiyun.app.data.db.QuizState>): List<LevelUi> {
        val dynasties = DynastyLevels.available(allPoems)
        val stateByLevel = states.associateBy { it.levelId }
        var previousPassed = true
        return dynasties.map { dynasty ->
            val quizState = stateByLevel[dynasty]
            val unlocked = previousPassed
            val stars = if (unlocked) quizState?.stars ?: 0 else 0
            previousPassed = (quizState?.stars ?: 0) >= 2
            LevelUi(dynasty, unlocked, stars)
        }
    }

    fun openLevel(dynasty: String) {
        viewModelScope.launch {
            val levelPoems = container.poemRepository.browse(dynasty, null)
            if (levelPoems.isEmpty()) return@launch
            val questions = generator.generate(levelPoems, allPoems)
            _state.update { it.copy(active = QuizSession(dynasty, questions)) }
        }
    }

    fun selectOption(index: Int) {
        val session = _state.value.active ?: return
        if (session.selected != -1) return
        val question = session.questions[session.index]
        val answer = when (question) {
            is QuizQuestion.FillBlank -> question.answer
            is QuizQuestion.NextLine -> question.answer
            is QuizQuestion.AuthorAttribution -> question.answer
        }
        val options = when (question) {
            is QuizQuestion.FillBlank -> question.options
            is QuizQuestion.NextLine -> question.options
            is QuizQuestion.AuthorAttribution -> question.options
        }
        val isCorrect = options.getOrNull(index) == answer
        _state.update {
            it.copy(
                active = session.copy(
                    selected = index,
                    correct = if (isCorrect) session.correct + 1 else session.correct,
                )
            )
        }
    }

    fun next() {
        val session = _state.value.active ?: return
        if (session.index + 1 >= session.questions.size) {
            viewModelScope.launch { container.quizRepository.saveResult(session.dynasty, session.correct) }
            _state.update { it.copy(active = session.copy(finished = true)) }
        } else {
            _state.update { it.copy(active = session.copy(index = session.index + 1, selected = -1)) }
        }
    }

    fun exitSession() {
        _state.update { it.copy(active = null) }
    }

    fun retry() {
        val dynasty = _state.value.active?.dynasty ?: return
        exitSession()
        openLevel(dynasty)
    }
}
```

- [ ] **Step 2: 实现界面**

`ui/quiz/QuizScreen.kt`:
```kotlin
package com.shiyun.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.domain.QuizQuestion
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun QuizScreen() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: QuizViewModel = viewModel { QuizViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    val session = state.active
    if (session == null) {
        LevelList(state, onOpen = vm::openLevel)
    } else if (session.finished) {
        ResultView(session, onExit = vm::exitSession, onRetry = vm::retry)
    } else {
        QuestionView(session, onSelect = vm::selectOption, onNext = vm::next)
    }
}

@Composable
private fun LevelList(state: QuizUiState, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("背诗闯关", style = MaterialTheme.typography.headlineMedium)
        if (state.levels.isEmpty()) Text("数据加载中…", color = FadedInk)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.levels, key = { it.dynasty }) { level ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(level.dynasty, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (level.unlocked) "★".repeat(level.stars).ifEmpty { "未挑战" } else "需先通过上一关",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FadedInk,
                            )
                        }
                        Button(onClick = { onOpen(level.dynasty) }, enabled = level.unlocked) { Text("开始") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(session: QuizSession, onSelect: (Int) -> Unit, onNext: () -> Unit) {
    val question = session.questions[session.index]
    val options = when (question) {
        is QuizQuestion.FillBlank -> question.options
        is QuizQuestion.NextLine -> question.options
        is QuizQuestion.AuthorAttribution -> question.options
    }
    val answer = when (question) {
        is QuizQuestion.FillBlank -> question.answer
        is QuizQuestion.NextLine -> question.answer
        is QuizQuestion.AuthorAttribution -> question.answer
    }
    val prompt = when (question) {
        is QuizQuestion.FillBlank -> {
            val s = question.sentence
            val shown = s.substring(0, question.blankIndex) + "____" + s.substring(question.blankIndex + 1)
            "补全诗句:\n$shown"
        }
        is QuizQuestion.NextLine -> "选择下句:\n${question.line}"
        is QuizQuestion.AuthorAttribution -> "此句出自哪位作者:\n${question.line}"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = { }) // 占位由外层控制退出
        Text("第 ${session.index + 1} / ${session.questions.size} 题 · 答对 ${session.correct}",
            style = MaterialTheme.typography.bodyMedium, color = FadedInk)
        Text(prompt, style = MaterialTheme.typography.titleLarge)

        options.forEachIndexed { index, option ->
            val isSelected = session.selected == index
            val isAnswer = option == answer
            OutlinedButton(
                onClick = { onSelect(index) },
                enabled = session.selected == -1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    option,
                    color = when {
                        session.selected == -1 -> MaterialTheme.colorScheme.onSurface
                        isAnswer -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.error
                        else -> FadedInk
                    },
                )
            }
        }

        if (session.selected != -1) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("下一题") }
        }
    }
}

@Composable
private fun ResultView(session: QuizSession, onExit: () -> Unit, onRetry: () -> Unit) {
    val stars = when (session.correct) {
        5 -> 3
        4 -> 2
        else -> 0
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(if (session.correct >= 4) "过关!" else "再接再厉", style = MaterialTheme.typography.headlineMedium)
        Text("★".repeat(stars).ifEmpty { "☆☆☆" }, style = MaterialTheme.typography.displayLarge)
        Text("答对 ${session.correct} / ${session.questions.size} 题", color = FadedInk)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text("再来一次") }
            OutlinedButton(onClick = onExit) { Text("返回关卡") }
        }
    }
}
```
注:`QuestionView` 里第一个 `TextButton(onClick = { })` 占位不理想,替换为 `TextButton(onClick = onExitSession)`——给 `QuestionView` 增加 `onExit: () -> Unit` 参数,按钮文本为"退出",由 `QuizScreen` 传 `vm::exitSession`。

- [ ] **Step 3: 构建验证**

Run: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: 背诗闯关玩法页"
```

---

### Task 13: 端到端冒烟与收尾

**Files:**
- Modify: `README.md`(补功能清单与手动验收步骤)

**Interfaces:**
- Consumes: 全部前序任务
- Produces: 可安装的 release/debug APK + 冒烟通过记录

- [ ] **Step 1: 全量测试与构建**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: 所有测试 PASS,BUILD SUCCESSFUL

- [ ] **Step 2: 真机/模拟器冒烟**

安装 `adb install -r app/build/outputs/apk/debug/app-debug.apk`,按顺序验证并记录结果:
1. 首启进入首页,1~2 秒内出现今日一诗卡片;
2. 点打卡 → 按钮变"今日已打卡",连击数显示 1;
3. 杀进程重开 → 今日诗不变、打卡状态保持;
4. 诗词库:输入"明月"有结果;输入乱串显示空态;点朝代"宋"筛选生效;
5. 点列表项进详情,收藏 → 图标变实心,收藏页出现该诗,详情页再点取消;
6. 闯关:第一关可开始,答 5 题(故意答错 1 题)→ 得 2 星过关,第二关解锁;
7. 明细核对:详情页无译文时不显示译文区块。

- [ ] **Step 3: 更新 README 并提交**

README 增补功能清单(四个功能一句话)、数据来源致谢(chinese-poetry)、手动验收清单(上面 7 条)。

```bash
git add -A
git commit -m "docs: 功能说明与冒烟验收清单"
```

---

## 自审记录(执行前已核对)

- **Spec 覆盖**:每日一诗(Task 5/9)、打卡连击(Task 5/9)、诗词库搜索筛选(Task 2/4/10)、收藏(Task 6/11)、闯关三种题型与星级解锁(Task 7/12)、数据管线(Task 3)、古典主题(Task 1/8)、首启导入与错误兜底(Task 4)、空态(Task 10/11)、素材不足降级(Task 7 测试覆盖)——均有对应任务。
- **类型一致**:`Poem` 的 `searchText` 字段贯穿 Task 2/3(管线不含该字段,由 Task 4 导入器计算)/Task 4;`MatchQueryBuilder.build/segment` 在 Task 2 定义、Task 4 使用;`AppContainer` 在 Task 4 建立、Task 6 扩展、Task 9-12 消费;`QuizGenerator.generate(levelPoems, distractorPool, count)` 签名 Task 7 定义、Task 12 使用。
- **无占位符**:所有代码步骤给出完整代码;Task 12 界面中的占位按钮已在任务内明确要求替换为带 `onExit` 参数的退出按钮。
