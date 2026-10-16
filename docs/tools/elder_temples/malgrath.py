"""The Broken Crucible: a fortified sanctuary built around a sealed rupture."""


SPEC = {
    "name": "The Broken Crucible",
    "size": (33, 19, 29),
    "statue": (19, 4, 22),
    "entrance": (6, 4, 4),
    "worship": (17, 4, 17, 21, 21),
}


def build(b):
    brick = "polished_blackstone_bricks"
    cracked = "cracked_polished_blackstone_bricks"
    dressed = "polished_blackstone"
    carved = "chiseled_polished_blackstone"
    basalt = "polished_basalt"
    stairs = "polished_blackstone_brick_stairs"
    slabs = "polished_blackstone_brick_slab"

    def foundation(x1, z1, x2, z2, surface=brick):
        b.fill(x1, 0, z1, x2, 2, z2, "blackstone")
        b.fill(x1, 3, z1, x2, 3, z2, surface)

    def walls(x1, z1, x2, z2, top):
        b.fill(x1, 4, z1, x1, top, z2, brick)
        b.fill(x2, 4, z1, x2, top, z2, brick)
        b.fill(x1, 4, z1, x2, top, z1, brick)
        b.fill(x1, 4, z2, x2, top, z2, brick)

    def basalt_column(x, z, top):
        b.put(x, 4, z, carved)
        if top > 4:
            b.fill(x, 5, z, x, top, z, basalt, {"axis": "y"})

    def roof_height(x):
        distance = abs(x - 18)
        return 13 if distance >= 9 else 14 if distance >= 7 else 15 if distance >= 5 else 16 if distance >= 3 else 17

    def bars(x, y, z):
        b.put(x, y, z, "iron_bars", {
            "north": "false", "east": "false", "south": "false", "west": "false",
            "waterlogged": "false",
        })

    # Separate, fully founded footprints leave the outside terrain untouched.
    foundation(9, 8, 27, 26)
    foundation(3, 3, 11, 10, dressed)
    for z in range(4):
        b.fill(4, 0, z, 9, min(z, 2), z, "blackstone")
        if z == 0:
            b.fill(5, 0, z, 8, 0, z, dressed)
        else:
            for x in range(5, 9):
                b.stair(x, z, z, stairs, "south")
        b.air(5, z + 1, z, 8, 6, z)
    foundation(11, 5, 17, 10, dressed)
    foundation(1, 12, 9, 24, dressed)
    foundation(27, 9, 31, 24, cracked)

    # Hollow only occupied rooms and their passages, never the enclosing volume.
    b.air(10, 4, 9, 26, 12, 25)
    b.air(4, 4, 4, 10, 10, 9)
    b.air(12, 4, 6, 16, 8, 9)
    b.air(2, 4, 13, 8, 9, 23)
    b.air(28, 4, 10, 30, 10, 23)
    walls(9, 8, 27, 26, 12)
    walls(3, 3, 11, 10, 10)
    walls(1, 12, 9, 24, 9)
    b.fill(11, 4, 5, 17, 8, 5, brick)
    b.fill(17, 4, 5, 17, 8, 8, brick)

    # Off-axis gatehouse: a broad portal, stout jambs, a protected right turn.
    for x in (4, 9):
        b.fill(x, 4, 2, x, 8, 3, carved)
        b.stair(x, 9, 2, stairs, "south", half="top")
    b.fill(4, 9, 2, 9, 9, 3, dressed)
    b.fill(5, 10, 2, 8, 10, 2, carved)
    b.air(5, 4, 2, 8, 7, 3)
    b.air(11, 4, 6, 11, 7, 8)
    b.air(13, 4, 8, 16, 7, 8)
    b.fill(12, 8, 6, 16, 8, 8, dressed)
    b.fill(11, 9, 5, 17, 9, 9, brick)
    for z in (5, 9):
        for x in range(11, 18):
            b.slab(x, 10, z, slabs)
    b.fill(3, 11, 3, 11, 11, 10, brick)
    for x in range(2, 13):
        b.stair(x, 11, 2, stairs, "south", half="top")
        b.stair(x, 11, 11, stairs, "north", half="top")
    for z in range(3, 11):
        b.stair(2, 11, z, stairs, "east", half="top")
        b.stair(12, 11, z, stairs, "west", half="top")
    for x1, x2 in ((3, 4), (10, 11)):
        b.fill(x1, 12, 3, x2, 13, 4, carved)
        b.fill(x1, 14, 3, x2, 14, 4, dressed)
        b.fill(x1, 12, 9, x2, 12, 10, brick)
    for x in range(5, 10):
        bars(x, 12, 3)
    b.put(6, 10, 4, "chain", {"axis": "y", "waterlogged": "false"})
    b.put(6, 9, 4, "chain", {"axis": "y", "waterlogged": "false"})
    b.put(6, 8, 4, "chain", {"axis": "y", "waterlogged": "false"})
    b.lantern(6, 7, 4, hanging=True)
    b.banner(4, 7, 1, color="black", facing="north")
    b.banner(9, 7, 1, color="orange", facing="north")
    for x in (4, 9):
        b.put(x, 0, 0, carved)
        b.lantern(x, 1, 0)

    # The main roof is a continuous stepped vault, borne by gables and three ribs.
    for x in range(8, 29):
        top = roof_height(x)
        if 10 <= x <= 26 and top > 13:
            b.air(x, 13, 9, x, top - 1, 25)
        for z in range(7, 28):
            b.put(x, top, z, brick)
        if 9 <= x <= 27:
            b.fill(x, 13, 8, x, top, 8, brick)
            b.fill(x, 13, 26, x, top, 26, brick)
        for z in (7, 27):
            b.slab(x, top + 1, z, slabs)
    b.fill(17, 18, 9, 19, 18, 25, dressed)
    for z in range(9, 26):
        b.stair(8, 12, z, stairs, "east", half="top")
        b.stair(28, 12, z, stairs, "west", half="top")

    for z in (11, 16, 23):
        for x in (12, 24):
            basalt_column(x, z, 11)
            b.fill(x - 1, 11, z - 1, x + 1, 11, z + 1, dressed)
            b.put(x, 12, z, carved)
        for x in range(12, 25):
            spring = 11 + min(x - 12, 24 - x, 4)
            b.fill(x, spring, z, x, roof_height(x) - 1, z, basalt, {"axis": "x"})

    # Three stepped buttresses on each flank, with their own buried footings.
    for z in (10, 18, 24):
        foundation(7, z, 9, z + 1)
        b.fill(8, 4, z, 9, 10, z + 1, brick)
        b.fill(7, 4, z, 7, 7, z + 1, dressed)
        b.stair(7, 8, z, stairs, "east")
        b.stair(8, 11, z, stairs, "east")
    for z in (10, 17, 24):
        foundation(27, z, 29, z + 1)
        b.fill(27, 4, z, 28, 10, z + 1, brick)
        b.fill(29, 4, z, 29, 7, z + 1, dressed)
        b.stair(29, 8, z, stairs, "west")
        b.stair(28, 11, z, stairs, "west")
    for x in (12, 18, 24):
        foundation(x, 26, x + 1, 28)
        b.fill(x, 4, 26, x + 1, 11, 27, brick)
        b.fill(x, 4, 28, x + 1, 7, 28, dressed)
        b.stair(x, 8, 28, stairs, "north")

    # Recessed clerestories are closed glass, not holes in the sanctuary roof.
    for x, facing in ((9, "east"), (27, "west")):
        for z1, z2 in ((12, 14), (20, 21)):
            b.fill(x, 8, z1, x, 10, z2, "gray_stained_glass")
            for z in range(z1, z2 + 1):
                b.stair(x, 7, z, stairs, facing, half="top")
                b.put(x, 11, z, carved)
    b.air(9, 4, 14, 9, 7, 16)
    b.air(27, 4, 12, 27, 7, 14)
    b.fill(9, 8, 14, 9, 8, 16, carved)
    b.fill(27, 8, 12, 27, 8, 14, carved)

    # Workshop: low masonry roof, stone benches and a sealed, purely visual forge.
    b.fill(1, 10, 12, 9, 10, 24, brick)
    for z in range(12, 25):
        b.stair(0, 10, z, stairs, "east", half="top")
    for x in range(1, 10):
        b.stair(x, 10, 11, stairs, "south", half="top")
        b.stair(x, 10, 25, stairs, "north", half="top")
    b.fill(2, 4, 18, 4, 6, 22, basalt, {"axis": "y"})
    b.fill(3, 4, 19, 3, 5, 21, "shroomlight")
    b.fill(4, 4, 19, 4, 5, 21, "orange_stained_glass")
    b.fill(2, 7, 18, 4, 7, 22, dressed)
    b.fill(2, 8, 20, 3, 13, 21, brick)
    b.fill(2, 14, 20, 3, 14, 21, carved)
    b.fill(2, 4, 23, 6, 4, 23, dressed)
    b.put(6, 4, 19, "anvil", {"facing": "north"})
    b.put(6, 4, 21, "chipped_anvil", {"facing": "east"})
    b.put(2, 4, 14, "smithing_table")
    b.put(2, 4, 15, "crafting_table")
    b.put(3, 4, 14, "stonecutter", {"facing": "east"})
    b.put(2, 5, 16, "grindstone", {"face": "wall", "facing": "east"})
    b.fill(1, 6, 14, 1, 7, 16, "gray_stained_glass")
    b.lantern(5, 9, 15, hanging=True)
    b.put(5, 10, 15, dressed)
    b.fill(7, 4, 22, 8, 4, 23, cracked)
    b.slab(8, 5, 23, slabs)

    # East gallery: the northern bays remain roofed; the southern bay has fallen.
    for z, top in ((9, 10), (15, 10), (21, 5), (24, 8)):
        basalt_column(31, z, top)
        b.put(31, top + 1, z, cracked if top < 10 else carved)
    b.fill(28, 11, 9, 31, 11, 16, brick)
    for z in range(9, 17):
        b.stair(32, 11, z, stairs, "west", half="top")
    b.fill(28, 11, 17, 29, 11, 18, cracked)
    b.slab(30, 11, 17, slabs)
    b.fill(28, 4, 20, 29, 4, 22, cracked)
    b.put(28, 5, 21, basalt, {"axis": "z"})
    b.stair(29, 5, 22, stairs, "north", shape="outer_left")
    b.slab(30, 4, 23, slabs)
    b.put(30, 4, 21, carved)
    b.fill(31, 4, 10, 31, 4, 20, dressed)
    for z1, z2 in ((10, 14), (16, 20)):
        for z in range(z1, z2 + 1):
            bars(31, 5, z)
            bars(31, 6, z)
    b.put(30, 10, 12, "chain", {"axis": "y", "waterlogged": "false"})
    b.lantern(30, 9, 12, hanging=True)

    # Processional inlays lead from the dogleg to the unraised sacred floor.
    b.fill(5, 3, 4, 8, 3, 9, dressed)
    b.fill(8, 3, 6, 16, 3, 7, dressed)
    b.fill(13, 3, 8, 16, 3, 14, dressed)
    b.fill(14, 3, 13, 21, 3, 16, dressed)
    b.fill(16, 3, 16, 22, 3, 22, dressed)
    for x in (16, 22):
        b.fill(x, 3, 16, x, 3, 22, carved)
    b.fill(16, 3, 16, 22, 3, 16, carved)

    # One authored fault, including its displaced shoulders, not random floor noise.
    fracture = (
        (10, 10), (11, 11), (12, 11), (13, 12), (14, 13), (15, 14),
        (16, 14), (17, 15), (18, 16), (19, 17), (20, 18), (20, 19),
        (21, 20), (22, 21), (23, 22), (24, 23), (25, 23), (26, 24),
    )
    for x, z in fracture:
        b.put(x, 1, z, "shroomlight")
        b.put(x, 2, z, "orange_stained_glass")
        b.put(x, 3, z, "black_stained_glass")
    for x, z in ((13, 13), (16, 15), (19, 18), (22, 22), (25, 24)):
        b.put(x, 3, z, cracked)
    for x, z in ((13, 12), (14, 13), (15, 14), (16, 14), (17, 15), (18, 16), (19, 17), (20, 18)):
        top = roof_height(x)
        b.put(x, top, z, "black_stained_glass")
        b.put(x, top - 1, z, "shroomlight")

    # The altar is framed like a broken fastening, not a second royal throne.
    for x in (16, 22):
        basalt_column(x, 24, 9)
        b.put(x, 10, 24, carved)
    b.fill(16, 10, 24, 18, 10, 24, dressed)
    b.fill(20, 10, 24, 22, 10, 24, cracked)
    b.slab(19, 10, 24, slabs, kind="top")
    b.fill(18, 6, 26, 20, 8, 26, "shroomlight")
    b.fill(18, 6, 25, 20, 8, 25, "orange_stained_glass")
    b.fill(17, 5, 25, 21, 5, 25, carved)
    b.fill(17, 9, 25, 21, 9, 25, carved)
    b.column(17, 25, 6, 8, "iron_block")
    b.column(21, 25, 6, 8, "iron_block")
    b.put(19, 3, 22, "gilded_blackstone")
    for x, z in ((11, 20), (25, 20), (15, 24), (23, 24)):
        b.put(x, 4, z, carved)
        b.lantern(x, 5, z)
    b.banner(10, 7, 17, color="black", facing="east")
    b.banner(26, 7, 22, color="orange", facing="west")

    # Connections are derived after all abutments exist; bars never claim air anchors.
    solid = {"minecraft:" + name for name in (brick, cracked, dressed, carved, basalt, "blackstone", "iron_block")}
    for (x, y, z), (name, _properties, nbt) in list(b.blocks.items()):
        if name != "minecraft:iron_bars":
            continue
        properties = {"waterlogged": "false"}
        for direction, dx, dz in (("north", 0, -1), ("east", 1, 0), ("south", 0, 1), ("west", -1, 0)):
            neighbor = b.blocks.get((x + dx, y, z + dz))
            connected = neighbor is not None and (neighbor[0] in solid or neighbor[0] == name)
            properties[direction] = str(connected).lower()
        b.put(x, y, z, name, properties, nbt)

    # This is intentionally last: neither rubble, anvil work nor decoration may obstruct worship.
    b.air(17, 4, 17, 21, 6, 21)
    b.statue()
    b.air(19, 5, 22, 19, 6, 22)
