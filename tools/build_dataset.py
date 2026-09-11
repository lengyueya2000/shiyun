"""从 chinese-poetry 数据集生成 assets/poems.json。用法:
  python tools/build_dataset.py [--check]
数据集仓库克隆到 tools/chinese-poetry(已 gitignore)。
依赖(仅本脚本与测试环境需要): pip install opencc-python-reimplemented
"""
import argparse
import hashlib
import json
import random
import re
import sys
from pathlib import Path

import opencc

_T2S = opencc.OpenCC("t2s")

REPO_DIR = Path(__file__).parent / "chinese-poetry"
OUT_PATH = Path(__file__).parent.parent / "app/src/main/assets/poems.json"
PUNCT = re.compile(r"[、。,;:!?「」『』《》()\[\]·\s]")

# - 明诗/清诗/小学/初中/高中 数据集中不存在对应文件。
# - 元曲(yuanqu.json, raw dynasty 为 "yuan")/曹操诗集(caocao.json, 无 author/dynasty)/
#   纳兰性德诗集(para 字段为正文键, 无 dynasty)分别经朝代别名映射、author 缺省、para 回退纳入。
SOURCES = [
    # (模糊匹配的文件名片段, 朝代, 类型, featured, 采样上限, author 缺省)
    ("唐诗三百首", "唐", "诗", True, 0, None),         # 0 = 全量
    ("宋词三百首", "宋", "词", True, 0, None),
    ("shijing", "先秦", "诗", True, 0, None),
    ("chuci", "先秦", "诗", False, 40, None),
    ("shuimotangshi", "唐", "诗", False, 0, None),
    ("poet.tang", "唐", "诗", False, 600, None),
    ("ci.song", "宋", "词", False, 300, None),
    ("yuanqu", "元", "词", False, 300, None),
    ("caocao", "魏晋", "诗", False, 0, "曹操"),
    ("纳兰性德诗集", "明清", "词", False, 0, None),
]

# 数据集部分来源的 dynasty 字段为英文, 统一映射为中文朝代名
DYNASTY_ALIASES = {"yuan": "元"}


def make_id(title: str, author: str, first_line: str) -> int:
    digest = hashlib.sha1(f"{title}|{author}|{first_line}".encode("utf-8")).hexdigest()
    return int(digest[:16], 16) % (2 ** 62)


def dedupe_key(rec: dict) -> tuple:
    first = PUNCT.sub("", rec["paragraphs"][0]) if rec["paragraphs"] else ""
    return (rec["title"], rec["author"], first)


def normalize_record(raw: dict, dynasty: str, kind: str, featured: bool, author_default: str | None = None) -> dict:
    title = (raw.get("title") or raw.get("name") or raw.get("rhythmic") or raw.get("chapter") or "").strip()
    # "para" 仅纳兰性德诗集.json 使用, 作为额外正文键回退
    paragraphs = raw.get("paragraphs") or raw.get("content") or raw.get("para") or []
    paragraphs = [" ".join(line) if isinstance(line, list) else line for line in paragraphs]
    raw_dynasty = raw.get("dynasty")
    return {
        "title": title,
        "dynasty": DYNASTY_ALIASES.get(raw_dynasty, raw_dynasty) or dynasty,
        "author": (raw.get("author") or author_default or "佚名").strip(),
        "paragraphs": paragraphs,
        "kind": kind,
        "translation": raw.get("translation"),
        "notes": raw.get("notes"),
        "appreciation": raw.get("appreciation"),
        "tags": [],
        "difficulty": 1,
        "featured": featured,
    }


def to_simplified(rec: dict) -> dict:
    for key in ("title", "author"):
        rec[key] = _T2S.convert(rec[key])
    rec["paragraphs"] = [_T2S.convert(p) for p in rec["paragraphs"]]
    for key in ("translation", "notes", "appreciation"):
        value = rec[key]
        if isinstance(value, list):
            # poet.tang 等源的 notes/translation 可能为 list,拼接为字符串
            value = "\n".join(x for x in value if isinstance(x, str) and x.strip())
        if value:
            rec[key] = _T2S.convert(value)
    return rec


def has_content(rec: dict) -> bool:
    # 丢弃 title 为空或 paragraphs 为空/全为空白/标点的记录
    return bool(rec["title"]) and any(PUNCT.sub("", line) for line in rec["paragraphs"])


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


def load_source(dataset: Path, fragment: str, dynasty: str, kind: str, featured: bool, cap: int, rng: random.Random, author_default: str | None = None) -> list[dict]:
    files = collect_files(dataset, fragment)
    records = []
    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if not isinstance(data, list):
            continue
        records.extend(
            normalize_record(raw, dynasty, kind, featured, author_default)
            for raw in data if isinstance(raw, dict)
        )
    records = [rec for rec in records if has_content(rec)]
    if cap and len(records) > cap:
        records = rng.sample(records, cap)
    return records


def build(dataset_dir: Path) -> list[dict]:
    if not dataset_dir.exists():
        sys.exit(f"数据集不存在: {dataset_dir},请先 git clone --depth 1 https://github.com/chinese-poetry/chinese-poetry {dataset_dir}")
    rng = random.Random(42)
    records = []
    for fragment, dynasty, kind, featured, cap, author_default in SOURCES:
        records.extend(load_source(dataset_dir, fragment, dynasty, kind, featured, cap, rng, author_default))
    # 繁→简转换(normalize 之后、去重与 id 生成之前;转换幂等)
    records = [to_simplified(rec) for rec in records]
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
            assert isinstance(rec["id"], int) and rec["paragraphs"] and rec["title"], rec["title"]
        print(f"OK: {len(data)} 首, featured {sum(1 for r in data if r['featured'])} 首")
        return
    records = build(REPO_DIR)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(json.dumps(records, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"写出 {len(records)} 首到 {OUT_PATH}")


if __name__ == "__main__":
    main()
