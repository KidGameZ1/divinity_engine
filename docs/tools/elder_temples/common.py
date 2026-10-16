"""Small, deterministic construction helpers, not a procedural building grammar."""


class Builder:
    def __init__(self, god, spec):
        self.god = god
        self.spec = spec
        self.blocks = {}

    def put(self, x, y, z, name, properties=None, nbt=None):
        position = (x, y, z)
        assert all(isinstance(v, int) and 0 <= v < limit
                   for v, limit in zip(position, self.spec["size"])), (self.god, position)
        if ":" not in name:
            name = "minecraft:" + name
        self.blocks[position] = (name, tuple(sorted((properties or {}).items())), nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, name, properties=None):
        assert x1 <= x2 and y1 <= y2 and z1 <= z2
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                for x in range(x1, x2 + 1):
                    self.put(x, y, z, name, properties)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "air")

    def column(self, x, z, y1, y2, block):
        self.fill(x, y1, z, x, y2, z, block)

    def stair(self, x, y, z, block, facing, half="bottom", shape="straight"):
        self.put(x, y, z, block, {"facing": facing, "half": half,
                                "shape": shape, "waterlogged": "false"})

    def slab(self, x, y, z, block, kind="bottom"):
        self.put(x, y, z, block, {"type": kind, "waterlogged": "false"})

    def lantern(self, x, y, z, hanging=False):
        self.put(x, y, z, "lantern", {"hanging": str(hanging).lower(), "waterlogged": "false"})

    def banner(self, x, y, z, color="blue", facing="north"):
        self.put(x, y, z, color + "_wall_banner", {"facing": facing},
                 {"id": (8, "minecraft:banner"), "Patterns": (9, (10, [
                     {"Color": (3, 4), "Pattern": (8, "bo")},
                     {"Color": (3, 4), "Pattern": (8, "mc")}
                 ]))})

    def statue(self):
        x, y, z = self.spec["statue"]
        name = f"divinity_engine:{self.god}_statue"
        self.put(x, y, z, name, {"facing": "north", "waterlogged": "false"},
                 {"id": (8, name), "Items": (9, (10, []))})
        self.put(x, y + 1, z, "air")
