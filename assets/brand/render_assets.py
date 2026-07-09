#!/usr/bin/env python3
"""Render vatori brand PNG/ICO/ICNS assets from the selected imagegen source."""

from __future__ import annotations

import struct
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT = Path(__file__).resolve().parent
SOURCE = OUT / "vatori-icon-source.png"
DEEP = (11, 61, 56, 255)
LIGHT = (255, 252, 244, 255)


def background_color(im: Image.Image) -> tuple[int, int, int]:
    w, h = im.size
    corners = [
        im.getpixel((0, 0)),
        im.getpixel((w - 1, 0)),
        im.getpixel((0, h - 1)),
        im.getpixel((w - 1, h - 1)),
    ]
    return tuple(sum(c[i] for c in corners) // 4 for i in range(3))


def transparent_icon_base() -> Image.Image:
    # Normalize to the canonical app-icon canvas, then apply a deterministic
    # rounded-square alpha mask. The imagegen source contains faint white
    # background texture outside the plate; threshold removal leaves speckles on
    # black backgrounds, while this mask keeps the plate clean.
    rgba = Image.open(SOURCE).convert("RGBA").resize((1024, 1024), Image.Resampling.LANCZOS)
    mask_scale = 3
    mask = Image.new("L", (1024 * mask_scale, 1024 * mask_scale), 0)
    draw = ImageDraw.Draw(mask)
    box = tuple(v * mask_scale for v in (70, 66, 954, 956))
    draw.rounded_rectangle(box, radius=146 * mask_scale, fill=255)
    mask = mask.resize((1024, 1024), Image.Resampling.LANCZOS)
    rgba.putalpha(mask)
    return rgba


def render_icon(size: int, base: Image.Image) -> Image.Image:
    return base.resize((size, size), Image.Resampling.LANCZOS)


def font(size: int, index: int = 0) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype("/System/Library/Fonts/Avenir Next.ttc", size, index=index)


def make_wordmark(icon: Image.Image, fill: tuple[int, ...], path: str) -> Image.Image:
    im = Image.new("RGBA", (1600, 520), (255, 255, 255, 0))
    im.alpha_composite(icon.resize((392, 392), Image.Resampling.LANCZOS), (64, 64))
    draw = ImageDraw.Draw(im)
    draw.text((536, 338), "vatori", font=font(202), fill=fill, anchor="ls")
    im.save(OUT / path)
    return im


def write_ico() -> None:
    files = [(OUT / f"vatori-icon-{size}.png", size) for size in [16, 32, 64, 128, 256]]
    datas: list[bytes] = []
    entries: list[bytes] = []
    offset = 6 + 16 * len(files)
    for path, size in files:
        data = path.read_bytes()
        datas.append(data)
        entries.append(
            struct.pack(
                "<BBBBHHII",
                0 if size == 256 else size,
                0 if size == 256 else size,
                0,
                0,
                1,
                32,
                len(data),
                offset,
            )
        )
        offset += len(data)
    (OUT / "vatori-icon.ico").write_bytes(
        struct.pack("<HHH", 0, 1, len(files)) + b"".join(entries) + b"".join(datas)
    )


def write_icns() -> None:
    items = [
        ("icp4", 16),
        ("icp5", 32),
        ("icp6", 64),
        ("ic07", 128),
        ("ic08", 256),
        ("ic09", 512),
        ("ic10", 1024),
        ("ic11", 32),
        ("ic12", 64),
        ("ic13", 256),
        ("ic14", 512),
    ]
    chunks: list[bytes] = []
    for code, size in items:
        data = (OUT / f"vatori-icon-{size}.png").read_bytes()
        chunks.append(code.encode("ascii") + struct.pack(">I", len(data) + 8) + data)
    body = b"".join(chunks)
    (OUT / "vatori-icon.icns").write_bytes(b"icns" + struct.pack(">I", len(body) + 8) + body)


def write_background_check(icon: Image.Image, light_wordmark: Image.Image, dark_wordmark: Image.Image) -> None:
    check = Image.new("RGBA", (1600, 860), (255, 255, 255, 255))
    draw = ImageDraw.Draw(check)
    draw.rectangle((0, 0, 800, 860), fill=(255, 255, 255, 255))
    draw.rectangle((800, 0, 1600, 860), fill=(0, 0, 0, 255))
    label = font(42, index=5)
    draw.text((400, 92), "white background", font=label, fill=(80, 80, 80, 255), anchor="mm")
    draw.text((1200, 92), "black background", font=label, fill=(230, 230, 230, 255), anchor="mm")
    check.alpha_composite(icon.resize((420, 420), Image.Resampling.LANCZOS), (190, 180))
    check.alpha_composite(icon.resize((420, 420), Image.Resampling.LANCZOS), (990, 180))
    check.alpha_composite(light_wordmark.resize((800, 260), Image.Resampling.LANCZOS), (0, 560))
    check.alpha_composite(dark_wordmark.resize((800, 260), Image.Resampling.LANCZOS), (800, 560))
    check.save(OUT / "vatori-background-check.png")


def main() -> None:
    icon = transparent_icon_base()
    for size in [16, 32, 64, 128, 256, 512, 1024]:
        render_icon(size, icon).save(OUT / f"vatori-icon-{size}.png")

    icon_1024 = Image.open(OUT / "vatori-icon-1024.png").convert("RGBA")
    light_wordmark = make_wordmark(icon_1024, DEEP, "vatori-wordmark-1600x520.png")
    dark_wordmark = make_wordmark(icon_1024, LIGHT, "vatori-wordmark-on-dark-1600x520.png")

    write_ico()
    write_icns()
    write_background_check(icon_1024, light_wordmark, dark_wordmark)


if __name__ == "__main__":
    main()
