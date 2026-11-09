"""Generates the Phase 10 glow textures (F10.1), white on transparent so vertex colour tints them.

    python tools/make_glow_textures.py

Writes src/main/resources/assets/jeg_vr_compat/textures/misc/grab_glow.png (soft radial glow) and grab_ring.png (soft
ring). Pure Python, no image library needed.
"""
import math
import os
import struct
import zlib

SIZE = 64
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'jeg_vr_compat',
                   'textures', 'misc')


def smoothstep(edge0, edge1, x):
    t = min(max((x - edge0) / (edge1 - edge0), 0.0), 1.0)
    return t * t * (3.0 - 2.0 * t)


def glow(r):
    # Bright core fading smoothly to nothing at the edge.
    return (1.0 - smoothstep(0.0, 1.0, r)) ** 1.6


def ring(r):
    # A soft band near the edge, gone at the centre and outside the circle.
    return math.exp(-((r - 0.82) / 0.09) ** 2) * (1.0 - smoothstep(0.97, 1.0, r))


def write_png(path, alpha_of_radius):
    rows = bytearray()
    half = SIZE / 2.0
    for y in range(SIZE):
        rows.append(0)  # filter: none
        for x in range(SIZE):
            r = math.hypot(x + 0.5 - half, y + 0.5 - half) / half
            a = round(255 * min(max(alpha_of_radius(r), 0.0), 1.0))
            rows += bytes((255, 255, 255, a))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(rows), 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    write_png(os.path.join(OUT, 'grab_glow.png'), glow)
    write_png(os.path.join(OUT, 'grab_ring.png'), ring)
    print('Wrote grab_glow.png and grab_ring.png to', os.path.normpath(OUT))
