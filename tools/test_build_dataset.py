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
