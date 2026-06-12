"""Generate minimal placeholder carousel images (original assets were lost)."""
import base64
import struct
import zlib
from pathlib import Path

OUT = Path(r"C:\Users\TKB_USER\Desktop\tkb\devops\vcs\web\src\assets")

# Simple solid-color PNG generator without PIL
def make_png(width, height, rgb):
    r, g, b = rgb

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b""
    row = bytes([0, r, g, b] * width)
    for _ in range(height):
        raw += row

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def make_gif(width, height, rgb):
    r, g, b = rgb
    # minimal GIF89a single color
    header = b"GIF89a"
    logical = struct.pack("<HHB", width, height, 0xF0)
    gct = bytes([r, g, b, 0, 0, 0]) + bytes([0] * (256 * 3 - 6))
    image_desc = b"\x2C" + struct.pack("<HHHHB", 0, 0, width, height, 0)
    lzw_min = b"\x08"
    # tiny LZW data
    data = b"\x02\x02\x4C\x01\x00"
    trailer = b"\x3B"
    return header + logical + gct + image_desc + lzw_min + data + trailer


FILES = {
    "1760253304510.gif": ("gif", (90, 120, 180)),
    "swimming.png": ("png", (56, 189, 248)),
    "kp.png": ("png", (239, 68, 68)),
    "昌.png": ("png", (251, 191, 36)),
    "liar.png": ("png", (168, 85, 247)),
    "koreafish.png": ("png", (52, 211, 153)),
}

OUT.mkdir(parents=True, exist_ok=True)
for name, (kind, color) in FILES.items():
    path = OUT / name
    data = make_gif(240, 150, color) if kind == "gif" else make_png(240, 150, color)
    path.write_bytes(data)
    print(f"wrote {path} ({len(data)} bytes)")
