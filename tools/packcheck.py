#!/usr/bin/env python3
"""Audit resource-pack dimensions and plugin/model selector tokens."""

import json
from pathlib import Path
import struct
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))

import arsenal_source as source


def png_header(path):
    with path.open("rb") as stream:
        if stream.read(8) != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"not a PNG: {path}")
        length = struct.unpack(">I", stream.read(4))[0]
        if stream.read(4) != b"IHDR" or length < 13:
            raise ValueError(f"missing IHDR: {path}")
        width, height, depth, color_type, _, _, _ = struct.unpack(">IIBBBBB", stream.read(13))
        return width, height, depth, color_type


def selector_cases(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    return data["model"]["cases"]


def main():
    pack = ROOT / "resource-pack"
    texture_files = sorted((pack / "assets/arsenal/textures/item").glob("*.png"))
    model_files = sorted((pack / "assets/arsenal/models/item").glob("*.json"))
    bad_dimensions = []
    formats = set()
    for path in texture_files:
        width, height, depth, color_type = png_header(path)
        formats.add((width, height, depth, color_type))
        if (width, height, depth, color_type) != (16, 16, 8, 6):
            bad_dimensions.append(path.name)

    weapons = source.waffen()
    munitions = source.munitionen()
    source_tokens = {item["id"] for item in weapons + munitions}
    source_model_tokens = {
        item["modell"].split(":", 1)[-1] for item in weapons + munitions
    }
    weapon_tokens = {item["id"] for item in weapons}
    ammo_tokens = {item["id"] for item in munitions}
    weapon_cases = selector_cases(pack / "assets/minecraft/items/blaze_rod.json")
    ammo_cases = selector_cases(pack / "assets/minecraft/items/firework_star.json")
    all_cases = weapon_cases + ammo_cases
    actual_case_tokens = {
        case["when"].split(":", 1)[-1] for case in all_cases
    }
    expected_cases = weapon_tokens | ammo_tokens
    missing_cases = sorted(expected_cases - actual_case_tokens)
    extra_cases = sorted(actual_case_tokens - expected_cases)
    missing_model_tokens = sorted(source_model_tokens - actual_case_tokens)
    selector_targets = {
        case["model"]["model"].split(":", 1)[-1].split("/", 1)[-1]
        for case in all_cases
    }
    missing_targets = sorted(
        target
        for target in selector_targets
        if not (pack / "assets/arsenal/models/item" / f"{target}.json").exists()
    )

    print(
        "pack files: textures=%d models=%d selector_cases=%d"
        % (len(texture_files), len(model_files), len(actual_case_tokens))
    )
    print(
        "shell texture files: %d"
        % sum(path.stem.startswith("shell_") for path in texture_files)
    )
    print(
        "texture format: %s"
        % ", ".join("%dx%d/%dbit/color_type_%d" % item for item in sorted(formats))
    )
    print("non-16x16 RGBA textures: %s" % (", ".join(bad_dimensions) or "none"))
    print("selector cases missing source IDs: %s" % (", ".join(missing_cases) or "none"))
    print("selector cases not in source IDs: %s" % (", ".join(extra_cases) or "none"))
    print("source model tokens not in selector cases: %s" % (", ".join(missing_model_tokens) or "none"))
    print("selector target model files missing: %s" % (", ".join(missing_targets) or "none"))
    if missing_cases or extra_cases or missing_model_tokens or missing_targets or bad_dimensions:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
