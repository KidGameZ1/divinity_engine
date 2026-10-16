"""Observatory of the Unwritten: a supported terrace around an interrupted sky.

The open north-south procession is independent of the ruined eastern stair.
Only occupied rooms, the courtyard, and deliberate architectural gaps are cleared.
"""


SPEC = {
    "name": "Observatory of the Unwritten",
    "size": (29, 23, 33),
    "statue": (14, 4, 25),
    "entrance": (14, 4, 4),
    "worship": (12, 4, 20, 16, 24),
}


def build(b):
    # Three solid foundation layers under the terrace; the entrance tapers to terrain.
    terraces = (
        (4, 4, 10, 18),
        (5, 8, 7, 21),
        (9, 11, 4, 24),
        (12, 26, 2, 26),
        (27, 29, 4, 24),
        (30, 32, 7, 21),
    )
    for z1, z2, left, right in terraces:
        b.fill(left, 0, z1, right, 2, z2, "end_stone")
        b.fill(left, 3, z1, right, 3, z2, "end_stone_bricks")
        for x in (left, right):
            b.fill(x, 1, z1, x, 1, z2, "end_stone_bricks")
            b.fill(x, 3, z1, x, 3, z2, "purpur_block")
        for z in range(z1, z2 + 1):
            b.stair(left - 1, 2, z, "purpur_stairs", "east", half="top")
            b.stair(right + 1, 2, z, "purpur_stairs", "west", half="top")

    # Room-shaped air volumes leave the surrounding End island untouched.
    b.air(11, 4, 0, 17, 10, 4)
    b.air(8, 4, 5, 20, 12, 8)
    b.air(5, 4, 9, 23, 16, 11)
    b.air(8, 4, 12, 20, 21, 29)
    b.air(3, 4, 14, 7, 11, 26)
    b.air(21, 4, 13, 25, 15, 25)
    b.air(8, 4, 30, 20, 11, 31)

    # Boundary apron has feet at y=1. South-facing risers reach the first flat landing.
    b.air(11, 1, 0, 17, 10, 3)
    for z in range(4):
        b.fill(10, 0, z, 18, max(0, z - 1), z, "end_stone")
        for x in range(11, 18):
            if z == 0:
                b.put(x, 0, z, "end_stone_bricks")
            else:
                b.stair(x, z, z, "purpur_stairs", "south")
        for x in (10, 18):
            b.column(x, z, 0, z, "end_stone_bricks")
            b.put(x, z + 1, z, "purpur_block")
            b.slab(x, z + 2, z, "smooth_quartz_slab")

    # The ground plan is a drawing of two interrupted orbits, not a noise texture.
    for z in range(10, 30):
        for x in range(5, 24):
            radius = abs(x - 14) + abs(z - 21)
            if radius in (6, 10) and not (x > 16 and z < 18):
                b.put(x, 3, z, "purpur_block")
            elif radius == 7 and x < 12:
                b.put(x, 3, z, "smooth_quartz")
    b.fill(12, 3, 4, 16, 3, 24, "smooth_quartz")
    for z in (4, 8, 13, 18):
        b.put(14, 3, z, "chiseled_quartz_block")
    for x in (11, 17):
        b.fill(x, 3, 5, x, 3, 24, "purpur_block")
    for x, z in ((10, 10), (18, 10), (10, 16), (18, 16),
                 (10, 22), (18, 22), (12, 28), (16, 28)):
        b.put(x, 2, z, "sea_lantern")
        b.put(x, 3, z, "cyan_stained_glass")
        for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            b.put(x + dx, 3, z + dz, "smooth_quartz")

    # Low stone balustrades follow the changing footprint without closing the entrance.
    for z1, z2, left, right in terraces:
        for x in (left, right):
            for z in range(z1, z2 + 1):
                b.put(x, 4, z, "end_stone_bricks")
                b.slab(x, 5, z, "purpur_slab")
    for x in range(7, 22):
        b.put(x, 4, 32, "end_stone_bricks")
        b.slab(x, 5, 32, "purpur_slab")
    for z, left, right, outer_left, outer_right in (
        (5, 7, 21, 10, 18),
        (9, 4, 24, 7, 21),
        (12, 2, 26, 4, 24),
        (29, 4, 24, 7, 21),
    ):
        for x in range(left, right + 1):
            if x < outer_left or x > outer_right:
                b.put(x, 4, z, "end_stone_bricks")
                b.slab(x, 5, z, "purpur_slab")

    # Successive doors disagree about their center, but share a generous safe passage.
    for z, left, right, lintel in ((6, 8, 19, 11), (11, 10, 23, 14)):
        for x in (left, right):
            b.fill(x, 4, z, x, lintel - 1, z + 1, "purpur_pillar", {"axis": "y"})
            b.fill(x, 4, z - 1, x, 5, z - 1, "chiseled_quartz_block")
            b.slab(x, 6, z - 1, "smooth_quartz_slab")
        b.fill(left, lintel, z, right, lintel, z + 1,
               "purpur_pillar", {"axis": "x"})
        b.fill(left + 1, lintel + 1, z, right - 1, lintel + 1, z,
               "smooth_quartz")
        b.stair(left + 1, lintel - 1, z, "smooth_quartz_stairs", "west", half="top")
        b.stair(right - 1, lintel - 1, z, "smooth_quartz_stairs", "east", half="top")
        for x in (left + 2, right - 2):
            b.put(x, lintel - 1, z + 1, "end_rod", {"facing": "down"})
        # A cyan slit is backed by the lintel, never a floating sheet over the path.
        b.fill(left + 3, lintel, z - 1, right - 3, lintel, z - 1,
               "cyan_stained_glass")

    # Western archive: two deeply recessed, east-facing alcoves and one continuous wall.
    b.fill(2, 4, 14, 2, 10, 26, "end_stone_bricks")
    b.fill(2, 10, 14, 7, 10, 26, "purpur_block")
    for z in (14, 20, 26):
        b.fill(3, 4, z, 7, 9, z, "end_stone_bricks")
        b.column(7, z, 4, 10, "purpur_pillar")
        b.stair(6, 9, z, "smooth_quartz_stairs", "east", half="top")
        b.slab(7, 11, z, "purpur_slab")
    for center in (17, 23):
        b.fill(2, 5, center - 1, 2, 8, center + 1, "smooth_quartz")
        b.fill(2, 6, center - 1, 2, 7, center + 1, "sea_lantern")
        b.fill(3, 6, center - 1, 3, 7, center + 1, "cyan_stained_glass")
        b.fill(3, 4, center - 1, 3, 4, center + 1, "purpur_block")
        for z in range(center - 1, center + 2):
            b.stair(4, 4, z, "smooth_quartz_stairs", "west")
            b.slab(4, 8, z, "smooth_quartz_slab", kind="top")
        b.fill(5, 3, center - 1, 6, 3, center + 1, "smooth_quartz")
        b.put(6, 3, center, "chiseled_quartz_block")
        b.put(6, 9, center, "end_rod", {"facing": "down"})
    for z in range(15, 26):
        b.slab(3, 11, z, "end_stone_brick_slab")

    # Eastern stair: intact lower flight to a guarded overlook, then a missing upper flight.
    # The break is scenery beyond a parapet, not a required jump on the worship route.
    for step in range(5):
        z, y = 24 - step, 4 + step
        if y > 4:
            b.fill(22, 4, z, 24, y - 1, z, "end_stone_bricks")
        for x in range(22, 25):
            b.stair(x, y, z, "purpur_stairs", "north")
        for x in (21, 25):
            b.column(x, z, 4, y, "end_stone_bricks")
            b.put(x, y + 1, z, "purpur_block")
            b.slab(x, y + 2, z, "smooth_quartz_slab")
    for x in (22, 25):
        for z in (16, 19):
            b.column(x, z, 4, 7, "end_stone_bricks")
            b.put(x, 5, z, "chiseled_quartz_block")
    b.fill(22, 7, 16, 25, 7, 19, "purpur_pillar", {"axis": "z"})
    b.fill(22, 8, 16, 25, 8, 19, "smooth_quartz")
    for z in range(16, 20):
        for x in (21, 25):
            b.put(x, 8, z, "purpur_block")
            b.put(x, 9, z, "cyan_stained_glass")
            b.slab(x, 10, z, "purpur_slab")
    b.fill(21, 9, 16, 25, 9, 16, "purpur_block")
    for x in range(21, 26):
        b.slab(x, 10, 16, "smooth_quartz_slab")
    b.put(23, 7, 18, "sea_lantern")
    b.put(23, 8, 18, "cyan_stained_glass")
    b.column(24, 13, 4, 10, "end_stone_bricks")
    b.fill(23, 10, 13, 24, 10, 14, "purpur_block")
    b.stair(23, 11, 13, "purpur_stairs", "north")
    b.stair(24, 11, 13, "purpur_stairs", "north")
    b.put(24, 12, 13, "end_rod", {"facing": "up"})
    b.air(22, 9, 15, 24, 13, 15)

    # A ribbed rear wall contains three shallow apertures rather than a closed hall.
    b.fill(7, 4, 31, 21, 9, 31, "end_stone_bricks")
    for x in (7, 12, 16, 21):
        b.column(x, 31, 4, 11, "purpur_pillar")
        b.slab(x, 12, 31, "smooth_quartz_slab")
    for x1, x2 in ((8, 11), (13, 15), (17, 20)):
        b.fill(x1, 5, 31, x2, 8, 31, "cyan_stained_glass")
        b.fill(x1, 4, 30, x2, 4, 30, "smooth_quartz")
        b.fill(x1, 9, 30, x2, 9, 30, "purpur_block")
        for x in range(x1, x2 + 1):
            b.slab(x, 10, 30, "smooth_quartz_slab")
        b.put((x1 + x2) // 2, 8, 30, "end_rod", {"facing": "down"})

    # Stepped arms of an incomplete monumental ring rise from broad, grounded feet.
    for x in (6, 22):
        b.fill(x - 1, 4, 27, x + 1, 5, 28, "end_stone_bricks")
        b.fill(x, 6, 27, x, 13, 28, "purpur_pillar", {"axis": "y"})
        b.put(x, 7, 26, "end_rod", {"facing": "north"})
    ring_segments = (
        (7, 13, 7, 15), (8, 15, 8, 17), (9, 17, 9, 18),
        (10, 18, 12, 19),
        (21, 13, 21, 15), (20, 15, 20, 17), (18, 17, 19, 18),
        (17, 19, 18, 20),
    )
    for x1, y1, x2, y2 in ring_segments:
        b.fill(x1, y1, 27, x2, y2, 28, "purpur_block")
    for x, y in ((7, 12), (8, 14), (9, 16), (10, 17)):
        b.stair(x, y, 27, "purpur_stairs", "west", half="top")
    for x, y in ((21, 12), (20, 14), (19, 16), (18, 18)):
        b.stair(x, y, 27, "purpur_stairs", "east", half="top")
    b.fill(10, 20, 27, 12, 20, 28, "smooth_quartz")
    b.fill(17, 21, 27, 18, 21, 28, "smooth_quartz")
    b.slab(12, 21, 27, "purpur_slab")
    b.slab(17, 22, 28, "purpur_slab")
    b.put(12, 19, 26, "cyan_stained_glass")
    b.put(17, 20, 26, "cyan_stained_glass")
    b.air(13, 18, 27, 16, 22, 28)

    # Separate cantilevered canopy leaves are carried by side ribs, not floating blocks.
    for x in (8, 20):
        b.column(x, 23, 4, 11, "purpur_pillar")
        b.fill(x, 4, 22, x, 5, 24, "chiseled_quartz_block")
    for x, y in ((9, 11), (10, 12), (11, 13),
                 (19, 11), (18, 12), (17, 13)):
        b.fill(x, y, 23, x, y + 1, 24, "purpur_block")
    b.fill(10, 14, 22, 13, 14, 26, "purpur_pillar", {"axis": "z"})
    b.fill(10, 15, 22, 13, 15, 26, "smooth_quartz")
    b.fill(16, 14, 23, 19, 15, 26, "purpur_block")
    b.fill(16, 16, 23, 19, 16, 26, "smooth_quartz")
    for x, y in ((11, 15), (18, 16)):
        for z in (23, 25):
            b.put(x, y, z, "sea_lantern")
            b.put(x, y + 1, z, "cyan_stained_glass")
    for z in range(22, 27):
        b.slab(13, 16, z, "purpur_slab")
    for z in range(23, 27):
        b.slab(16, 17, z, "purpur_slab")
    b.put(11, 13, 24, "end_rod", {"facing": "down"})
    b.put(18, 13, 24, "end_rod", {"facing": "down"})
    b.air(14, 12, 22, 15, 22, 26)

    # The sanctuary stays level: its plinth is a floor inlay, not another obstacle.
    b.fill(11, 3, 25, 17, 3, 26, "purpur_block")
    b.fill(13, 3, 25, 15, 3, 26, "chiseled_quartz_block")
    for x in (10, 18):
        b.put(x, 4, 25, "chiseled_quartz_block")
        b.put(x, 5, 25, "end_rod", {"facing": "up"})
    x1, feet, z1, x2, z2 = SPEC["worship"]
    b.fill(x1, feet - 1, z1, x2, feet - 1, z2, "smooth_quartz")
    b.put(14, feet - 1, 23, "chiseled_quartz_block")
    b.air(12, 4, 0, 16, 6, 24)
    b.air(x1, feet, z1, x2, feet + 2, z2)
    b.air(13, 4, 25, 15, 7, 26)
    b.statue()
