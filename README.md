# 诗韵 (Shiyun)

一款 Android 原生古诗词阅读应用,以宣纸、墨、朱红构成的古典视觉呈现中华诗词。数据来自 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 开源数据集,经本地管线处理为约 2003 首精选诗词,首启自动导入 Room 数据库。

## 功能

- **首页 · 今日一诗**:每日按日期稳定推荐一首精选诗词,支持一键打卡并累计连续打卡天数。
- **诗词库**:支持关键词搜索(分词匹配)、按朝代/体裁筛选,结果为空时展示空态。
- **收藏与详情**:诗词详情页展示正文与译文,可收藏/取消收藏,收藏页集中管理。
- **背诗闯关**:按朝代分关的问答玩法,含诗句补全、作者、朝代三种题型,按正确率给星并解锁下一关。

## 构建与测试

```bash
./gradlew.bat :app:assembleDebug        # 构建 debug APK
./gradlew.bat :app:testDebugUnitTest    # 运行全量单元测试
```

APK 产出路径:`app/build/outputs/apk/debug/app-debug.apk`。

## 手动验收清单

安装:`adb install -r app/build/outputs/apk/debug/app-debug.apk`

1. 首启进入首页,1~2 秒内出现今日一诗卡片;
2. 点打卡 → 按钮变"今日已打卡",连击数显示 1;
3. 杀进程重开 → 今日诗不变、打卡状态保持;
4. 诗词库:输入"明月"有结果;输入乱串显示空态;点朝代"宋"筛选生效;
5. 点列表项进详情,收藏 → 图标变实心,收藏页出现该诗,详情页再点取消;
6. 闯关:第一关可开始,答 5 题(故意答错 1 题)→ 得 2 星过关,第二关解锁;
7. 明细核对:详情页无译文时不显示译文区块。

异常路径:若 assets 导入失败,首页显示"数据加载失败"与"重试"按钮,点击可重新加载。

## 重跑数据管线(可选)

仅在需要更新语料时执行:

- 依赖:Python 3 + `pip install opencc-python-reimplemented`,并 `git clone https://github.com/chinese-poetry/chinese-poetry`(含 `json/chinese-poetry` 语料目录);
- 入口脚本位于 `tools/` 目录,输出 JSON 资产放入 `app/src/main/assets/`,随 APK 打包,首启由导入器写入 Room。
