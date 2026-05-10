"""Generate Recommend desktop banner — calligraphic wordmark with brand gradient."""
from PIL import Image, ImageDraw, ImageFont
import numpy as np
import os

BRAND_DIR = os.path.dirname(os.path.abspath(__file__))
FONT_DIR  = os.path.join(BRAND_DIR, "fonts")

W, H = 2560, 1440

# Artisan Pastel
BG     = (248, 247, 244)
DARK   = (26, 42, 36)
VIOLET = (124, 111, 224)   # #7C6FE0
TEAL   = (59, 212, 192)    # #3BD4C0
GOLD   = (212, 175, 55)


def radial_blob(size, color, max_alpha=0.35, falloff=2.4):
    cx = cy = size / 2
    y, x = np.ogrid[:size, :size]
    d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
    t = np.clip(1 - d / (size / 2), 0, 1)
    alpha = (255 * (t ** falloff) * max_alpha).astype(np.uint8)
    arr = np.zeros((size, size, 4), dtype=np.uint8)
    arr[..., 0] = color[0]; arr[..., 1] = color[1]; arr[..., 2] = color[2]
    arr[..., 3] = alpha
    return Image.fromarray(arr, "RGBA")


def linear_gradient(w, h, c1, c2):
    """Diagonal gradient (top-left c1 → bottom-right c2)."""
    arr = np.zeros((h, w, 3), dtype=np.uint8)
    # diagonal axis: (x+y) / (w+h)
    xs = np.arange(w).reshape(1, -1)
    ys = np.arange(h).reshape(-1, 1)
    t = (xs + ys) / (w + h - 2)
    t = np.clip(t, 0, 1)
    for i, (a, b) in enumerate(zip(c1, c2)):
        arr[..., i] = (a + (b - a) * t).astype(np.uint8)
    return Image.fromarray(arr, "RGB")


def gradient_text(word, font, c1, c2):
    """Render text in a diagonal gradient. Returns RGBA image cropped to text bounds."""
    # First, measure text
    tmp = Image.new("L", (10, 10))
    d = ImageDraw.Draw(tmp)
    bbox = d.textbbox((0, 0), word, font=font, anchor="lt")
    tw = bbox[2] - bbox[0] + 80   # padding for descenders/swashes
    th = bbox[3] - bbox[1] + 80

    # Render text as alpha mask
    mask = Image.new("L", (tw, th), 0)
    md = ImageDraw.Draw(mask)
    md.text((40 - bbox[0], 40 - bbox[1]), word, fill=255, font=font, anchor="lt")

    # Create gradient image of same size
    grad = linear_gradient(tw, th, c1, c2)

    # Combine: gradient where mask, transparent elsewhere
    rgba = grad.convert("RGBA")
    rgba.putalpha(mask)
    return rgba


def main():
    canvas = Image.new("RGB", (W, H), BG)

    # Atmospheric blobs (subtle warmth)
    violet = radial_blob(2200, VIOLET, max_alpha=0.40)
    teal   = radial_blob(2200, TEAL,   max_alpha=0.45)
    gold   = radial_blob(900,  GOLD,   max_alpha=0.16)
    canvas.paste(violet, (-700, -800), violet)
    canvas.paste(teal,   (W - 1500, H - 1400), teal)
    canvas.paste(gold,   (W - 950, 60), gold)

    # Calligraphic wordmark
    font_path = os.path.join(FONT_DIR, "pacifico.ttf")
    word_font = ImageFont.truetype(font_path, 460)
    word = "Recommend"

    word_img = gradient_text(word, word_font, VIOLET, TEAL)

    # Center on canvas
    wx = (W - word_img.width) // 2
    wy = (H - word_img.height) // 2
    canvas.paste(word_img, (wx, wy), word_img)

    out1 = os.path.join(BRAND_DIR, "recommend_banner_2560x1440.png")
    canvas.save(out1, "PNG", optimize=True)
    print("saved:", out1)
    canvas1080 = canvas.resize((1920, 1080), Image.LANCZOS)
    out2 = os.path.join(BRAND_DIR, "recommend_banner_1920x1080.png")
    canvas1080.save(out2, "PNG", optimize=True)
    print("saved:", out2)


if __name__ == "__main__":
    main()
