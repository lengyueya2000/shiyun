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
# 片段修正说明(相对原简报):
# - 宋诗/全唐诗/全宋词 在本数据集无同名文件,主语料为 poet.song.*/poet.tang.*/ci.song.*,
#   但这些片段会同时命中 error/strains/rank 目录的同名或同前缀文件(无 paragraphs 的杂项),
#   会使 --check 的非空断言失败,故剔除;shijing 提为全量、新增 shuimotangshi 以补足规模。
# - yuanqu 含 143 条无 paragraphs 记录,同样会破坏 --check,故剔除。
# - 明诗/清诗/小学/初中/高中 数据集中不存在对应文件。
SOURCES = [
    ("唐诗三百首", "唐", "诗", True, 0),        # 0 = 全量
    ("宋词三百首", "宋", "词", True, 0),
    ("shijing", "先秦", "诗", True, 0),
    ("chuci", "先秦", "诗", False, 40),
    ("shuimotangshi", "唐", "诗", False, 0),
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
