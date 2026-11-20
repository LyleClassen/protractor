"""Generate the mod logo from the Protractor item texture.

Standard library only. Run from anywhere:

    python3 -I tools/logo/make_logo.py

Writes the in-game mod list logo (128x128) and the README / CurseForge logo (512x512).
"""

import math
import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEXTURE = os.path.join(ROOT, "src/main/resources/assets/protractor/textures/item/protractor.png")
OUTPUTS = [
	(os.path.join(ROOT, "src/main/resources/assets/protractor/icon.png"), 128),
	(os.path.join(ROOT, "docs/logo.png"), 512),
]

GRID = 40  # the logo is laid out on a 40x40 grid of "art pixels"
SPRITE_SCALE = 2  # item texture pixels per side, on the art grid
SPRITE_ORIGIN = (4, 3)  # art-grid position of the texture's top-left corner

BG_TOP = (196, 232, 255)
BG_BOTTOM = (226, 212, 255)
CORNER_RADIUS = 0.2  # fraction of the logo size


# --- PNG I/O ---------------------------------------------------------------

def read_png_rgba(path):
	data = open(path, "rb").read()
	pos, idat, width, height = 8, b"", 0, 0
	while pos < len(data):
		length = struct.unpack(">I", data[pos:pos + 4])[0]
		kind, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
		if kind == b"IHDR":
			width, height, depth, colour = struct.unpack(">IIBB", body[:10])
			assert depth == 8 and colour == 6, "expected 8-bit RGBA"
		elif kind == b"IDAT":
			idat += body
		pos += 12 + length
	raw = zlib.decompress(idat)
	stride = width * 4
	rows, prev = [], bytearray(stride)
	for y in range(height):
		f = raw[y * (stride + 1)]
		line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
		for i in range(stride):
			a = line[i - 4] if i >= 4 else 0
			b = prev[i]
			c = prev[i - 4] if i >= 4 else 0
			if f == 1:
				line[i] = (line[i] + a) & 255
			elif f == 2:
				line[i] = (line[i] + b) & 255
			elif f == 3:
				line[i] = (line[i] + (a + b) // 2) & 255
			elif f == 4:
				p = a + b - c
				pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
				pred = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
				line[i] = (line[i] + pred) & 255
		rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(width)])
		prev = line
	return rows


def write_png_rgba(path, pixels):
	height, width = len(pixels), len(pixels[0])
	raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

	def chunk(kind, body):
		return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "wb") as f:
		f.write(b"\x89PNG\r\n\x1a\n")
		f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
		f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
		f.write(chunk(b"IEND", b""))


# --- Compositing -----------------------------------------------------------

def over(dst, src):
	"""Porter-Duff source-over for straight-alpha RGBA tuples."""
	sa, da = src[3] / 255, dst[3] / 255
	oa = sa + da * (1 - sa)
	if oa == 0:
		return (0, 0, 0, 0)
	rgb = tuple(round((src[i] * sa + dst[i] * da * (1 - sa)) / oa) for i in range(3))
	return rgb + (round(oa * 255),)


def lerp(a, b, t):
	return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# --- Art -------------------------------------------------------------------

TICK = (30, 50, 80)
SOFT_TICK = (150, 186, 214)
EYE = (27, 36, 56, 255)
SHINE = (255, 255, 255, 255)
BLUSH = (255, 128, 160, 200)
MOUTH = (27, 36, 56, 255)
SPARKLE = (255, 255, 255, 255)
SPARKLE_CORE = (255, 236, 150, 255)


def art_layer():
	"""The protractor sprite plus its face and sparkles, on the GRID x GRID art grid."""
	tex = read_png_rgba(TEXTURE)
	art = [[(0, 0, 0, 0)] * GRID for _ in range(GRID)]
	ox, oy = SPRITE_ORIGIN
	for ty, row in enumerate(tex):
		for tx, px in enumerate(row):
			if px[3] == 0:
				continue
			# Soften the dark tick marks inside the dome so they don't read as extra eyes.
			if px[:3] == TICK and ty < 12:
				px = SOFT_TICK + (255,)
			# Glass is translucent in-game; make it read as frosted glass on any page.
			solid = over((255, 255, 255, 255), px) if px[3] < 255 else px
			for dy in range(SPRITE_SCALE):
				for dx in range(SPRITE_SCALE):
					art[oy + ty * SPRITE_SCALE + dy][ox + tx * SPRITE_SCALE + dx] = solid

	def put(x, y, colour):
		art[y][x] = over(art[y][x], colour)

	# Face, left of the needle, relative to the sprite origin.
	fy = oy + 16
	for ex in (11, 18):  # eyes: 2x3, with a white shine
		for dy in range(3):
			for dx in range(2):
				put(ex + dx, fy + dy, EYE)
		put(ex, fy, SHINE)
	for bx in (8, 20):  # blush
		put(bx, fy + 3, BLUSH)
		put(bx + 1, fy + 3, BLUSH)
	for x, dy in ((14, 3), (15, 4), (16, 4), (17, 3)):  # smile
		put(x, fy + dy, MOUTH)

	# Sparkles: little four-point stars.
	for cx, cy, r in ((33, 6, 2), (6, 10, 1), (36, 13, 1)):
		put(cx, cy, SPARKLE_CORE)
		for i in range(1, r + 1):
			for x, y in ((cx + i, cy), (cx - i, cy), (cx, cy + i), (cx, cy - i)):
				put(x, y, SPARKLE)
	return art


def rounded_coverage(x, y, size, radius, samples=4):
	"""Anti-aliased coverage of pixel (x, y) by a rounded square."""
	inside = 0
	for sy in range(samples):
		for sx in range(samples):
			px = x + (sx + 0.5) / samples
			py = y + (sy + 0.5) / samples
			cx = min(max(px, radius), size - radius)
			cy = min(max(py, radius), size - radius)
			if (px - cx) ** 2 + (py - cy) ** 2 <= radius ** 2:
				inside += 1
	return inside / samples ** 2


def render(size):
	cell = size / GRID
	art = art_layer()
	radius = size * CORNER_RADIUS
	# Soft shadow under the protractor's base.
	sh_cx, sh_cy = size * 0.5, size * 0.8
	sh_rx, sh_ry = size * 0.36, size * 0.045

	out = []
	for y in range(size):
		bg = lerp(BG_TOP, BG_BOTTOM, y / (size - 1)) + (255,)
		row = []
		for x in range(size):
			cov = rounded_coverage(x, y, size, radius) if (x < radius or x >= size - radius) and (y < radius or y >= size - radius) else 1.0
			px = bg[:3] + (round(255 * cov),)
			d = ((x + 0.5 - sh_cx) / sh_rx) ** 2 + ((y + 0.5 - sh_cy) / sh_ry) ** 2
			if d < 1:
				px = over(px, (70, 60, 140, round(70 * (1 - d) ** 1.5 * cov)))
			a = art[min(int(y / cell), GRID - 1)][min(int(x / cell), GRID - 1)]
			if a[3]:
				px = over(px, a)
			row.append(px)
		out.append(row)
	return out


def main():
	for path, size in OUTPUTS:
		write_png_rgba(path, render(size))
		print("wrote", os.path.relpath(path, ROOT), f"{size}x{size}")


if __name__ == "__main__":
	main()
