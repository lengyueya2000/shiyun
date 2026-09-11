# 古诗词 App「诗韵」设计文档

日期:2026-09-11
状态:已经用户口头确认设计方向,待最终审阅

## 1. 目标与范围

开发一款 Android 原生古诗词应用「诗韵」,离线可用,第一版包含四个核心功能:

1. **每日一诗**:每日展示一首精选诗词,支持打卡与连续打卡天数统计。
2. **诗词库 + 搜索浏览**:全文搜索(标题/诗句/作者),按朝代、类型筛选。
3. **收藏夹**:收藏诗词,按时间倒序查看。
4. **背诗闯关**:按朝代分关卡,填字、上下句衔接、作者归属三种题型,过关解锁。

明确不在第一版范围内:账号系统、云端同步、朗读 TTS、社区/评论、iOS/桌面端。

## 2. 技术栈

| 项 | 选择 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose(Compose BOM)+ Material3 |
| 导航 | Navigation Compose,底部四 Tab(首页/诗词库/收藏/闯关)+ 详情页 |
| 数据库 | Room 2.6 + FTS4 全文索引 |
| 架构 | 单模块 MVVM:ui / data / domain 三个包 |
| 异步 | Coroutines + StateFlow |
| 序列化 | kotlinx-serialization |
| SDK | minSdk 26,targetSdk 35 |
| 构建 | Gradle (Kotlin DSL) |

数据完全离线:诗词数据打包进 APK assets,首次启动导入 Room。

## 3. 页面结构

### 3.1 首页
- 今日一诗卡片:标题、作者·朝代、正文前两行预览;点击进入详情页。
- 打卡按钮:当日首次点击打卡,展示连续打卡天数与累计天数。
- 每日选诗:以当天日期(本地时区)哈希从「精选池」确定性选取,同一自然日内固定不变,无需服务器。

### 3.2 诗词库
- 顶部搜索框:Room FTS4 全文检索,匹配标题、诗句、作者。
- 筛选:朝代(先秦/汉/魏晋/唐/宋/元/明清)、类型(诗/词)。
- 列表项:标题、作者·朝代、首句。

### 3.3 收藏
- 收藏列表,按收藏时间倒序;点击进详情;可取消收藏。

### 3.4 闯关
- 关卡按朝代分组,顺序解锁(先秦→汉魏晋→唐→宋→元明清)。
- 每关 5 题,题型三选一随机:
  1. **填字**:诗句挖空 1~2 字,从 4 个候选字中选择(干扰项取同诗或同朝代常见字)。
  2. **上下句**:给出上句,从 4 个候选下句中选择(干扰项取同诗其他句或同作者其他诗的句)。
  3. **作者归属**:给出诗句,从 4 位作者中选择。
- 过关条件:答对 ≥ 4 题;解锁下一关;记录每关星级(按答对数 1~3 星)。

### 3.5 详情页
- 诗词全文居中排版,下附译文、注释、赏析(数据缺失时对应区块不显示)。
- 右上角收藏/取消收藏。

## 4. 数据模型(Room)

```kotlin
@Entity(tableName = "poems")
data class Poem(
    @PrimaryKey val id: Long,          // 数据管线生成的稳定 ID
    val title: String,
    val dynasty: String,               // 先秦/汉/魏晋/唐/宋/元/明清
    val author: String,
    val paragraphs: List<String>,      // 正文,按句/段存储
    val kind: String,                  // "诗" | "词"
    val translation: String?,          // 译文,可为空
    val notes: String?,                // 注释,可为空
    val appreciation: String?,         // 赏析,可为空
    val tags: List<String>,            // 主题标签,如 写景/思乡/边塞
    val difficulty: Int,               // 1~3,用于出题难度
    val featured: Boolean,             // 是否属于每日一诗精选池
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val poemId: Long,
    val createdAt: Long,
)

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: String,      // yyyy-MM-dd,本地时区
    val poemId: Long,
    val checkedIn: Boolean,
)

@Entity(tableName = "quiz_states")
data class QuizState(
    @PrimaryKey val levelId: String,   // 如 "tang_01"
    val stars: Int,                    // 0~3
    val unlocked: Boolean,
)
```

- 搜索:为 `poems` 建 FTS4 虚表 `poems_fts`(title/author/paragraphs 触发器同步),支持中文按句匹配。
- 类型转换:paragraphs/tags 用 `TypeConverter`(JSON 编码)。
- 首次启动导入:Application 启动时在 IO 协程检查 poems 表是否为空,为空则从 `assets/poems.json` 批量插入(约 1 秒),期间首页显示加载态。

## 5. 数据管线(Python 脚本)

- 输入:开源 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 数据集(唐诗三百首、宋词三百首、宋诗、中小学背诵篇目等子集)。
- 处理:合并去重(按标题+作者+首句)、筛选译文赏析齐全的优先、标注朝代/类型/难度/标签、分配稳定 ID(标题+作者+首句的 sha1 前 16 位转数字)。
- 目标规模:约 2000 首(1000~3000 区间),精选池(featured)约 400 首。
- 输出:`app/src/main/assets/poems.json`(UTF-8),供首次导入使用。
- 脚本位于仓库 `tools/build_dataset.py`,可重复运行。

## 6. 视觉设计(古典雅致风)

- 配色:宣纸底 `#F7F4EA`、墨色正文 `#2B2B2B`、朱红强调 `#B03A2E`(按钮/选中/打卡)、辅以淡墨 `#8A8578` 做次级文字。
- 字体:诗词正文使用衬线字体(优先系统衬线,内嵌思源宋体子集作为后备)。
- 卡片:细描边(1dp 淡墨色)代替投影;页面大留白。
- 详情页标题居中,正文行距宽松;导航图标线性风格。

## 7. 代码结构

```
app/src/main/java/com/shiyun/app/
├── ShiyunApp.kt              # Application,触发数据导入
├── MainActivity.kt
├── ui/
│   ├── theme/                # 古典雅致主题(色板/字体/形状)
│   ├── home/  library/  favorites/  quiz/  detail/
│   └── navigation/
├── data/
│   ├── db/                   # Room: entities, dao, database, converters
│   ├── assets/               # 首启导入器
│   └── repository/           # PoemRepository / FavoriteRepository / QuizRepository
└── domain/
    ├── DailyPoemSelector.kt  # 日期哈希选诗
    └── QuizGenerator.kt      # 出题(题干+4 选项+答案+题型)
```

## 8. 错误处理

- assets 导入失败:首页展示重试入口,不崩溃。
- 数据缺失字段(无译文等):详情页对应区块整体不渲染。
- 搜索无结果:空态插画 + 文案。
- 出题素材不足(某关诗句过少):降级为仅作者归属题型。

## 9. 测试策略

- 单元测试(JVM):
  - `DailyPoemSelector`:同日稳定、跨日可变、精选池边界。
  - `QuizGenerator`:答案必在选项中、干扰项不与答案重复、题型分布。
  - `PoemRepository`:FTS 查询、筛选组合(用 in-memory Room)。
- UI:手动验收为主,关键路径(首启导入、打卡、收藏、闯关过关)走真机冒烟。

## 10. 风险

- chinese-poetry 数据集字段不完全统一(词与诗结构不同),管线需分别适配;
  译文/赏析覆盖不全时接受留空。
- FTS4 中文分词按字匹配,短语搜索体验有限,第一版按"包含任一关键词"处理。
