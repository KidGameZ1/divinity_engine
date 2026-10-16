"""Hall of the First Oath: maintained civic architecture with an older side archive."""

SPEC = {
    "name": "Hall of the First Oath",
    "size": (31, 21, 31),
    "entrance": (15, 4, 3),
    "statue": (15, 5, 26),
    "worship": (13, 5, 21, 17, 25),
}


def build(b):
    # Three connected foundations, not a square platform spanning the whole site.
    for x1, z1, x2, z2 in ((5, 1, 25, 12), (6, 11, 24, 28), (1, 16, 6, 26), (24, 13, 29, 21)):
        b.fill(x1, 0, z1, x2, 2, z2, "stone_bricks")
        b.fill(x1, 3, z1, x2, 3, z2, "smooth_stone")
        b.air(x1, 4, z1, x2, 10, z2)
        for x in range(x1, x2 + 1):
            b.put(x, 2, z1, "chiseled_stone_bricks")
            b.put(x, 2, z2, "stone_bricks")
    # A wide, usable street-side stair and two smaller landings.
    b.fill(11, 0, 0, 19, 0, 0, "stone_bricks")
    b.air(11, 1, 0, 19, 5, 2)
    for x in range(11, 20):
        for z in range(3):
            b.fill(x, 0, z, x, z, z, "stone_bricks")
            b.stair(x, z + 1, z, "stone_brick_stairs", "south")

    # Courtyard: low seating walls, paved borders and two non-mirrored planting beds.
    for x in (5, 25):
        b.fill(x, 4, 2, x, 4, 9, "stone_bricks")
        for z in range(2, 10):
            b.slab(x, 5, z, "stone_brick_slab")
    for x1, z1, x2, z2 in ((7, 3, 10, 6), (20, 5, 23, 8)):
        b.fill(x1, 3, z1, x2, 3, z2, "dirt")
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                b.put(x, 4, z, "azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
        b.put(x1 + 1, 5, z1 + 1, "flowering_azalea_leaves",
              {"persistent": "true", "distance": "1", "waterlogged": "false"})
    for x in (12, 18):
        for z in range(2, 20):
            b.put(x, 3, z, "cut_sandstone")
    for x, z in ((6, 2), (24, 2), (9, 9), (21, 9)):
        b.column(x, z, 4, 5, "chiseled_stone_bricks")
        b.lantern(x, 6, z)

    # Main hall and deep entrance reveal.
    b.fill(6, 4, 11, 6, 10, 28, "stone_bricks")
    b.fill(24, 4, 11, 24, 10, 28, "stone_bricks")
    # Close the eaves below the first roof riser; the cornice carries the roof.
    for x in (6, 24):
        b.fill(x, 11, 11, x, 11, 28, "cut_sandstone")
    b.fill(6, 4, 28, 24, 10, 28, "stone_bricks")
    b.fill(6, 4, 11, 11, 10, 11, "stone_bricks")
    b.fill(19, 4, 11, 24, 10, 11, "stone_bricks")
    b.fill(11, 10, 11, 19, 11, 11, "cut_sandstone")
    for x in (11, 19):
        b.column(x, 10, 4, 9, "smooth_sandstone")
        b.put(x, 9, 10, "chiseled_sandstone")
        b.stair(x + (1 if x == 11 else -1), 9, 11, "sandstone_stairs",
                "west" if x == 11 else "east", "top")
    b.fill(12, 10, 10, 18, 10, 10, "smooth_sandstone")
    b.banner(10, 8, 10, facing="north")
    b.banner(20, 8, 10, facing="north")
    # Three structural bays, glass recessed between outside buttresses.
    for x in (6, 24):
        outward = -1 if x == 6 else 1
        for z in (12, 19, 27):
            b.column(x + outward, z, 3, 9, "chiseled_stone_bricks")
            b.put(x + outward, 10, z, "cut_sandstone")
            b.stair(x + outward * 2, 4, z, "stone_brick_stairs", "east" if x == 6 else "west")
        for z in (15, 23):
            b.fill(x, 6, z, x, 8, z + 2, "blue_stained_glass")
            for window_z in range(z, z + 3):
                b.slab(x + outward, 5, window_z, "sandstone_slab", "top")
                b.stair(x + outward, 9, window_z, "sandstone_stairs", "east" if x == 6 else "west", "top")

    # Roof is a real pitched volume; only the occupied vault is cleared of terrain.
    for x in range(7, 24):
        ceiling = 19 - abs(x - 15)
        b.air(x, 11, 12, x, ceiling, 27)
    for z in range(9, 30):
        for layer in range(10):
            y = 11 + layer
            b.stair(5 + layer, y, z, "deepslate_tile_stairs", "east")
            b.stair(25 - layer, y, z, "deepslate_tile_stairs", "west")
        b.put(15, 20, z, "deepslate_tiles")
    for z in (10, 28):
        for x in range(6, 25):
            peak = 19 - abs(x - 15)
            if peak >= 11:
                b.fill(x, 11, z, x, peak, z, "smooth_sandstone")
        b.fill(14, 12, z, 16, 16, z, "blue_stained_glass")
        b.put(15, 17, z, "chiseled_sandstone")
    # Stone ribs and timber ties explain how the hall carries its roof.
    for z in (14, 20, 26):
        for x in range(7, 24):
            height = 18 - abs(x - 15)
            b.put(x, height, z, "stripped_dark_oak_log", {"axis": "x"})
        for x in (7, 23):
            b.column(x, z, 4, 9, "smooth_sandstone")
            b.stair(x + (1 if x == 7 else -1), 9, z, "sandstone_stairs",
                    "west" if x == 7 else "east", "top")
    for z in (16, 23):
        b.fill(15, 12, z, 15, 19, z, "chain", {"axis": "y", "waterlogged": "false"})
        b.lantern(15, 11, z, hanging=True)

    # Seating faces the clear aisle, rather than occupying it.
    for z in (14, 17):
        for x in range(9, 12):
            b.stair(x, 4, z, "stone_brick_stairs", "south")
        for x in range(19, 22):
            b.stair(x, 4, z, "stone_brick_stairs", "south")
    for z in range(12, 20):
        for x in range(14, 17):
            b.put(x, 3, z, "blue_terracotta")
    # Raised sanctuary, generously clear around the player's offering area.
    b.fill(12, 4, 21, 18, 4, 27, "smooth_sandstone")
    for x in range(12, 19):
        b.stair(x, 4, 20, "sandstone_stairs", "south")
    for x in (11, 19):
        b.column(x, 27, 4, 8, "chiseled_sandstone")
        b.lantern(x, 9, 27)
        b.banner(x, 7, 26, facing="north")
    b.put(15, 4, 26, "gold_block")

    # Side archive: older, lower masonry with shelving and a writing table.
    b.fill(1, 4, 16, 1, 8, 26, "stone_bricks")
    b.fill(1, 4, 16, 6, 8, 16, "stone_bricks")
    b.fill(1, 4, 26, 6, 8, 26, "stone_bricks")
    b.air(6, 4, 21, 6, 7, 22)
    b.fill(2, 4, 17, 2, 6, 24, "bookshelf")
    b.fill(4, 4, 24, 5, 4, 24, "dark_oak_planks")
    b.lantern(4, 5, 24)
    b.put(5, 5, 24, "potted_fern")
    for x in range(1, 7):
        for z in range(16, 27):
            b.slab(x, 9, z, "deepslate_tile_slab")
    # Right-hand cloister is a smaller open arcade, not a mirrored second archive.
    for z in (13, 17, 21):
        b.column(29, z, 4, 7, "smooth_sandstone")
        b.stair(28, 7, z, "sandstone_stairs", "east", "top")
    b.fill(25, 8, 13, 29, 8, 21, "cut_sandstone")
    for z in range(13, 22):
        b.slab(29, 9, z, "sandstone_slab")
    b.air(24, 4, 17, 24, 7, 18)
    b.fill(27, 4, 19, 28, 4, 19, "smooth_stone")
    b.lantern(28, 5, 19)

    # Localized age: repaired archive corners and a worn courtyard edge.
    for x, y, z in ((1, 4, 17), (1, 6, 25), (3, 4, 26), (5, 2, 7), (22, 2, 1), (24, 5, 27)):
        b.put(x, y, z, "cracked_stone_bricks")
    for x, y, z in ((1, 4, 25), (2, 4, 26), (5, 4, 8)):
        b.put(x, y, z, "mossy_stone_bricks")
    b.statue()
