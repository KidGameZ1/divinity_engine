"""Generate or validate retained legacy shrine templates; never touches a save.

Run with --write to rebuild deterministic gzip NBT, or without arguments to verify.
Use --models-only for a read-only statue asset check without inspecting structures.
Standard library only. JSON/model resources remain hand-authored alongside the NBT.
"""

import argparse
import gzip
import importlib
import io
import json
import struct
from pathlib import Path
from zipfile import ZipFile
from collections import deque

from elder_temples.common import Builder


ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "src/main/resources"
DATA = RESOURCES / "data/divinity_engine"
SHRINES = ("aureth", "malgrath", "vaelith")


def string(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def payload(kind, value):
    if kind == 3:
        return struct.pack(">i", value)
    if kind == 8:
        return string(value)
    if kind == 9:
        element, entries = value
        return bytes([element]) + struct.pack(">i", len(entries)) + b"".join(payload(element, e) for e in entries)
    if kind == 10:
        return b"".join(bytes([kind]) + string(name) + payload(kind, entry)
                        for name, (kind, entry) in value.items()) + b"\0"
    raise ValueError(kind)


def read_nbt(raw):
    stream = io.BytesIO(raw)

    def text():
        return stream.read(struct.unpack(">H", stream.read(2))[0]).decode("utf-8")

    def read(kind):
        if kind == 3:
            return struct.unpack(">i", stream.read(4))[0]
        if kind == 8:
            return text()
        if kind == 9:
            item = stream.read(1)[0]
            return [read(item) for _ in range(struct.unpack(">i", stream.read(4))[0])]
        if kind == 10:
            result = {}
            while (item := stream.read(1)[0]) != 0:
                name = text()
                result[name] = read(item)
            return result
        raise ValueError(kind)

    assert stream.read(1) == b"\x0a" and text() == ""
    result = read(10)
    assert stream.read() == b""
    return result


def layout(god):
    module = importlib.import_module("elder_temples." + god)
    builder = Builder(god, module.SPEC)
    module.build(builder)
    return builder


def template(god):
    builder = layout(god)
    palette = []
    palette_index = {}
    entries = []
    for pos, (name, properties, nbt) in sorted(builder.blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        key = (name, properties)
        if key not in palette_index:
            palette_index[key] = len(palette)
            state = {"Name": (8, name)}
            if properties:
                state["Properties"] = (10, {key: (8, value) for key, value in properties})
            palette.append(state)
        entry = {"pos": (9, (3, pos)), "state": (3, palette_index[key])}
        if nbt:
            entry["nbt"] = (10, nbt)
        entries.append(entry)
    metadata = {key: (9, (3, builder.spec[key])) for key in ("entrance", "statue", "worship")}
    metadata["name"] = (8, builder.spec["name"])
    result = {"DataVersion": (3, 3465), "size": (9, (3, builder.spec["size"])),
              "palette": (9, (10, palette)), "blocks": (9, (10, entries)),
              "entities": (9, (10, [])), "divinity_temple": (10, metadata)}
    return b"\x0a\x00\x00" + payload(10, result)


def verify_statue_model(god):
    assets = RESOURCES / "assets/divinity_engine"
    original = json.loads((assets / "models/block/statue.json").read_text())
    model = json.loads((assets / f"models/block/{god}_statue.json").read_text())
    alias = json.loads((assets / "models/block/elder_statue.json").read_text())
    expected = {"parent": "divinity_engine:block/statue"}
    assert model == expected, f"{god}: must inherit the original statue without mesh or texture overrides"
    assert alias == expected, "The legacy Elder model must alias the original statue"
    assert original["elements"] and original["display"], "Original geometry/item transforms are missing"
    assert original["textures"]["0"] == "divinity_engine:block/gods_statue"
    for element in original["elements"]:
        for face in element["faces"].values():
            assert face["texture"] == "#0", "Statue face does not use the original UV-mapped texture"
    assert (assets / "textures/block/gods_statue.png").is_file(), "Original statue texture is missing"
    item = json.loads((assets / f"models/item/{god}_statue.json").read_text())
    assert item == {"parent": f"divinity_engine:block/{god}_statue"}, f"{god}: item model must share the block model"
    variants = json.loads((assets / f"blockstates/{god}_statue.json").read_text())["variants"]
    directions = ("north", "east", "south", "west")
    assert set(variants) == {f"facing={direction}" for direction in directions}
    for index, direction in enumerate(directions):
        variant = variants[f"facing={direction}"]
        assert variant["model"] == f"divinity_engine:block/{god}_statue"
        assert variant.get("y", 0) == index * 90 and variant.get("x", 0) == 0
    print(f"{god}: original statue geometry/texture inherited; item and four facings valid")


def verify(god, raw=None):
    if raw is None:
        file = DATA / "structures" / f"{god}_shrine.nbt"
        raw = gzip.decompress(file.read_bytes())
    assert raw == template(god), f"{god}: NBT differs from the reproducible authored layout"
    data = read_nbt(raw)
    spec = layout(god).spec
    assert data["size"] == list(spec["size"]) and data["DataVersion"] == 3465
    assert not data["entities"]
    palette = data["palette"]
    states = {tuple(entry["pos"]): palette[entry["state"]]["Name"] for entry in data["blocks"]}
    assert len(states) == len(data["blocks"]), f"{god}: duplicate block positions"
    sx, sy, sz = spec["size"]
    assert len(states) < sx * sy * sz * 0.9, f"{god}: exterior should remain sparse"
    assert all(0 <= x < sx and 0 <= y < sy and 0 <= z < sz for x, y, z in states)
    statues = [entry for entry in data["blocks"] if palette[entry["state"]]["Name"] == f"divinity_engine:{god}_statue"]
    assert len(statues) == 1 and statues[0]["nbt"]["id"] == f"divinity_engine:{god}_statue"
    assert statues[0]["pos"] == list(spec["statue"])
    x, y, z = spec["statue"]
    assert states[(x, y + 1, z)] == "minecraft:air"
    assert data["divinity_temple"]["worship"] == list(spec["worship"])
    validate_walkway(god, spec, states)
    banned = {"minecraft:" + name for name in ("lava", "water", "fire", "soul_fire", "campfire", "soul_campfire",
        "spawner", "chest", "trapped_chest", "barrel", "hopper", "furnace", "blast_furnace", "smoker",
        "jigsaw", "structure_block", "command_block", "redstone_wire", "piston", "sticky_piston")}
    assert not banned.intersection(states.values())
    structure = json.loads((DATA / f"worldgen/structure/{god}_shrine.json").read_text())
    pool = json.loads((DATA / f"worldgen/template_pool/{god}_shrine.json").read_text())
    expected = f"divinity_engine:{god}_shrine"
    assert structure["start_pool"] == pool["name"] == expected
    assert pool["elements"][0]["element"]["location"] == expected
    assert not (DATA / f"worldgen/structure_set/{god}_shrine.json").exists(), "Retired temples must not generate"
    biomes = json.loads((DATA / f"tags/worldgen/biome/has_structure/{god}_shrine.json").read_text())
    assert biomes == {"replace": True, "values": []}, "Retired temples must have no eligible biomes"
    jar = Path.home() / ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar"
    if jar.is_file():
        with ZipFile(jar) as vanilla:
            names = set(vanilla.namelist())
            for state in palette:
                namespace, block = state["Name"].split(":")
                if namespace == "minecraft":
                    assert f"assets/minecraft/blockstates/{block}.json" in names, block
            model = json.loads((RESOURCES / "assets/divinity_engine/models/block/statue.json").read_text())
            for texture in model["textures"].values():
                namespace, path = texture.split(":")
                resource = f"assets/{namespace}/textures/{path}.png"
                assert resource in names if namespace == "minecraft" else (RESOURCES / resource).is_file(), texture
    print(f"{god}: {len(data['blocks'])} blocks, {len(palette)} states; NBT, statue, access and resource chain valid")


def validate_walkway(god, spec, states):
    def clear(pos):
        return states.get(pos) == "minecraft:air"

    def standable(pos):
        x, y, z = pos
        below = states.get((x, y - 1, z), "minecraft:air")
        return (clear(pos) and clear((x, y + 1, z)) and below != "minecraft:air"
                and not below.endswith(("_wall", "_fence", "_pane", "_carpet", "_banner"))
                and below not in ("minecraft:chain", "minecraft:iron_bars", "minecraft:end_rod"))

    start = tuple(spec["entrance"])
    assert standable(start), (god, "blocked entrance", start)
    reached = {start}
    pending = deque([start])
    while pending:
        x, y, z = pending.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                target = (x + dx, y + dy, z + dz)
                if target in reached or not standable(target):
                    continue
                if dy == 1 and not clear((x, y + 2, z)):
                    continue
                if dy == -1 and not clear((target[0], y + 1, target[2])):
                    continue
                reached.add(target)
                pending.append(target)
    x1, y, z1, x2, z2 = spec["worship"]
    assert x2 - x1 == 4 and z2 - z1 == 4
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            assert (x, y, z) in reached, (god, "unreachable worship space", (x, y, z))
            assert clear((x, y + 2, z)), (god, "low worship ceiling", (x, y + 2, z))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--models-only", action="store_true")
    parser.add_argument("--god", choices=SHRINES, help="Check or regenerate only one Elder")
    args = parser.parse_args()
    gods = (args.god,) if args.god else SHRINES
    prepared = {}
    for god in gods:
        verify_statue_model(god)
        if args.models_only:
            continue
        if args.write:
            prepared[god] = template(god)
            verify(god, prepared[god])
        else:
            verify(god)
    # Validate every requested layout before replacing any shipped template.
    for god, raw in prepared.items():
        output = DATA / "structures" / f"{god}_shrine.nbt"
        output.write_bytes(gzip.compress(raw, mtime=0))


if __name__ == "__main__":
    main()
