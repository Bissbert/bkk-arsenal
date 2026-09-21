#!/usr/bin/env python3
"""Generate source-derived SVG charts for the projectile runtime."""

from html import escape
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))

import arsenal_source as source


COLORS = ["#58a6ff", "#3fb950", "#d29922", "#bc8cff", "#f85149"]
BG = "#0d1117"
PANEL = "#161b22"
GRID = "#30363d"
TEXT = "#c9d1d9"
MUTED = "#8b949e"


def text(x, y, value, size=14, color=TEXT, anchor="start", weight="400"):
    return (
        f'<text x="{x}" y="{y}" fill="{color}" font-family="system-ui, sans-serif" '
        f'font-size="{size}px" font-weight="{weight}" text-anchor="{anchor}">'
        f"{escape(str(value))}</text>"
    )


def svg_start(width, height, title):
    return [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" '
        f'height="{height}" viewBox="0 0 {width} {height}">',
        f'<rect width="{width}" height="{height}" fill="{BG}"/>',
        f'<rect x="24" y="24" width="{width - 48}" height="{height - 48}" '
        f'rx="12" fill="{PANEL}" stroke="{GRID}"/>',
        text(52, 64, title, 22, TEXT, weight="700"),
    ]


def scale(value, low, high, start, end):
    if high == low:
        return (start + end) / 2
    return start + (value - low) / (high - low) * (end - start)


def trajectory_chart(weapons):
    direct = [weapon for weapon in weapons if not weapon["indirekt"]]
    max_ticks = max(weapon["cooldown"] for weapon in direct)
    max_drop = max(
        weapon["gravity"] * max_ticks * (max_ticks - 1) / 2 for weapon in direct
    )
    width, height = 1120, 700
    left, top, right, bottom = 100, 110, 1010, 585
    lines = svg_start(
        width,
        height,
        "Source-derived drop after a horizontal launch",
    )
    lines.append(
        text(
            52,
            88,
            "Discrete per-tick integration: drop = gravity × tick × (tick − 1) ÷ 2",
            13,
            MUTED,
        )
    )
    lines.extend(
        [
            f'<line x1="{left}" y1="{top}" x2="{left}" y2="{bottom}" '
            f'stroke="{GRID}"/>',
            f'<line x1="{left}" y1="{bottom}" x2="{right}" y2="{bottom}" '
            f'stroke="{GRID}"/>',
            text(left - 12, top + 5, "0", 12, MUTED, "end"),
            text(left - 12, bottom, f"−{max_drop:.1f}", 12, MUTED, "end"),
            text((left + right) / 2, bottom + 42, "server ticks after launch", 13, MUTED, "middle"),
            text(42, (top + bottom) / 2, "vertical drop (blocks)", 13, MUTED, "middle"),
        ]
    )
    for tick in range(0, max_ticks + 1, max(1, max_ticks // 4)):
        x = scale(tick, 0, max_ticks, left, right)
        lines.append(f'<line x1="{x:.1f}" y1="{top}" x2="{x:.1f}" y2="{bottom}" stroke="{GRID}" stroke-dasharray="3 5"/>')
        lines.append(text(x, bottom + 22, tick, 12, MUTED, "middle"))
    for index, weapon in enumerate(direct):
        points = []
        for tick in range(max_ticks + 1):
            drop = weapon["gravity"] * tick * (tick - 1) / 2
            x = scale(tick, 0, max_ticks, left, right)
            y = scale(-drop, -max_drop, 0, bottom, top)
            points.append(f"{x:.1f},{y:.1f}")
        color = COLORS[index % len(COLORS)]
        lines.append(
            f'<polyline points="{" ".join(points)}" fill="none" stroke="{color}" '
            f'stroke-width="3" stroke-linejoin="round"/>'
        )
        legend_x = 110 + (index % 3) * 285
        legend_y = 635 + (index // 3) * 22
        lines.append(f'<line x1="{legend_x}" y1="{legend_y - 5}" x2="{legend_x + 24}" y2="{legend_y - 5}" stroke="{color}" stroke-width="3"/>')
        lines.append(text(legend_x + 32, legend_y, f'{weapon["id"]} · {weapon["speed"]:g} blocks/tick', 12, TEXT))
    lines.append(text(52, height - 34, f"The x-axis ends at the longest direct-weapon default cooldown: {max_ticks} ticks.", 12, MUTED))
    lines.append("</svg>")
    return "\n".join(lines), max_ticks, max_drop


def impact_chart(weapons):
    artillery = {"field_cannon", "howitzer", "rocket_artillery"}
    values = []
    for weapon in weapons:
        values.append(weapon["power"])
        if weapon["id"] in artillery:
            values.append(weapon["power"] * weapon["impact_scale"])
    maximum = max(values)
    width, height = 1120, 700
    left, top, right, bottom = 90, 120, 1060, 575
    chart_width = right - left
    group_width = chart_width / len(weapons)
    bar_width = min(22, group_width * 0.24)
    lines = svg_start(
        width,
        height,
        "Impact power is not attenuated by travel distance",
    )
    lines.append(
        text(
            52,
            88,
            "Configured power and platform scaling for shared artillery HE; no distance-falloff function exists",
            13,
            MUTED,
        )
    )
    lines.extend(
        [
            f'<line x1="{left}" y1="{top}" x2="{left}" y2="{bottom}" stroke="{GRID}"/>',
            f'<line x1="{left}" y1="{bottom}" x2="{right}" y2="{bottom}" stroke="{GRID}"/>',
            text(left - 12, top + 5, f"{maximum:.2f}", 12, MUTED, "end"),
            text(left - 12, bottom, "0", 12, MUTED, "end"),
            text((left + right) / 2, bottom + 46, "weapon defaults", 13, MUTED, "middle"),
            text(36, (top + bottom) / 2, "explosion power", 13, MUTED, "middle"),
        ]
    )
    for value in (0, maximum / 2, maximum):
        y = scale(value, 0, maximum, bottom, top)
        lines.append(f'<line x1="{left}" y1="{y:.1f}" x2="{right}" y2="{y:.1f}" stroke="{GRID}" stroke-dasharray="3 5"/>')
        lines.append(text(left - 12, y + 4, f"{value:.2f}", 12, MUTED, "end"))
    for index, weapon in enumerate(weapons):
        center = left + (index + 0.5) * group_width
        base_x = center - bar_width - 2
        base_height = weapon["power"] / maximum * (bottom - top)
        lines.append(f'<rect x="{base_x:.1f}" y="{bottom - base_height:.1f}" width="{bar_width}" height="{base_height:.1f}" fill="#58a6ff"/>')
        if weapon["id"] in artillery:
            scaled = weapon["power"] * weapon["impact_scale"]
            scaled_height = scaled / maximum * (bottom - top)
            scaled_x = center + 2
            lines.append(f'<rect x="{scaled_x:.1f}" y="{bottom - scaled_height:.1f}" width="{bar_width}" height="{scaled_height:.1f}" fill="#d29922"/>')
        lines.append(text(center, bottom + 20, weapon["id"].replace("_", "-"), 11, TEXT, "middle"))
        lines.append(text(center, bottom + 34, f'{weapon["power"]:g}', 10, MUTED, "middle"))
    lines.extend(
        [
            '<rect x="820" y="58" width="14" height="14" fill="#58a6ff"/>',
            text(842, 70, "configured power", 12, TEXT),
            '<rect x="980" y="58" width="14" height="14" fill="#d29922"/>',
            text(1002, 70, "shared HE scaled", 12, TEXT),
            text(52, height - 34, "A projectile deals no impact damage while in flight; power is selected when it detonates.", 12, MUTED),
            "</svg>",
        ]
    )
    return "\n".join(lines), maximum


def main():
    weapons = source.waffen()
    media = ROOT / "media"
    media.mkdir(exist_ok=True)
    trajectory, max_ticks, max_drop = trajectory_chart(weapons)
    impact, maximum = impact_chart(weapons)
    (media / "trajectory.svg").write_text(trajectory, encoding="utf-8")
    (media / "impact-model.svg").write_text(impact, encoding="utf-8")
    print(
        "trajectory: direct_weapons=%d max_ticks=%d max_drop=%.3f blocks"
        % (sum(not w["indirekt"] for w in weapons), max_ticks, max_drop)
    )
    print("impact model: weapons=%d chart_max=%.3f power" % (len(weapons), maximum))
    print("wrote media/trajectory.svg and media/impact-model.svg")


if __name__ == "__main__":
    main()
