package net.nightshade.divinity_engine.util.divinity.blessings.aethon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.nightshade.divinity_engine.common.inventory.PocketInventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-owned records survive death, unequipping, and abandoning a god. Never rewrites legacy rooms. */
public final class PocketDomainData extends SavedData {
    public static final int MAX_ROOMS = 8;
    public static final int RESIDENT_PAGE_SIZE = 12;
    public static final int SIZE = 33;
    public static final int HEIGHT = 18;
    public static final int VOLUME = SIZE * SIZE * HEIGHT;
    public static final int MIN_SIZE = 9, MAX_SIZE = 63, MIN_HEIGHT = 8, MAX_HEIGHT = 24;
    private static final int LEGACY_UNSET = -999999999;
    private final Map<UUID, Domain> domains = new LinkedHashMap<>();
    private final Map<UUID, Visit> visitors = new LinkedHashMap<>();
    private int nextSlot;

    public static PocketDomainData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PocketDomainData::load,
                PocketDomainData::new, "divinity_engine_pocket_domains");
    }

    public Domain find(UUID owner) { return domains.get(owner); }
    public List<Domain> domains() { return List.copyOf(domains.values()); }
    public record Visit(UUID host, ReturnPoint origin) {}
    public Visit visit(UUID player) { return visitors.get(player); }
    public void admit(UUID player, UUID host, ReturnPoint origin) { visitors.put(player, new Visit(host, origin)); setDirty(); }
    public void leave(UUID player) { if (visitors.remove(player) != null) setDirty(); }
    public Domain at(BlockPos position) { return domains.values().stream().filter(domain -> domain.contains(position)).findFirst().orElse(null); }

    public Domain getOrCreate(UUID owner) {
        Domain old = domains.get(owner);
        if (old != null) return old;
        Domain domain = new Domain(owner, this::setDirty);
        domains.put(owner, domain);
        setDirty();
        return domain;
    }

    public Room planRoom(Domain domain, Style style) {
        return planRoom(domain, style, new Dimensions(SIZE, HEIGHT, SIZE));
    }

    public Room planRoom(Domain domain, Style style, Dimensions size) {
        if (domain.rooms.size() >= MAX_ROOMS || nextSlot >= 1_000_000
                || !size.valid() || domain.rooms.stream().anyMatch(room -> !room.ready())) return null;
        if (domain.towerOrigin == null || domain.nextFloor >= MAX_ROOMS) {
            int slot = nextSlot++;
            // New towers occupy fresh columns. Legacy rooms are never moved or rebuilt.
            domain.towerOrigin = new BlockPos(8192 + slot % 1024 * 128, 8, 8192 + slot / 1024 * 128);
            domain.nextFloor = 0;
        }
        Room room = new Room(domain.towerOrigin.above(32 * domain.nextFloor++), style, false);
        room.size = size;
        domain.rooms.add(room);
        setDirty();
        return room;
    }

    public boolean planResize(Domain domain, Room room, Dimensions size) {
        if (!domain.rooms.contains(room) || room.legacy || !size.valid() || room.size.equals(size)
                || domain.rooms.stream().anyMatch(value -> !value.ready())) return false;
        room.pendingSize = size;
        room.stage = 3;
        room.cursor = 0;
        room.blocked = false;
        setDirty();
        return true;
    }

    public boolean importLegacy(Domain domain, CompoundTag blessing) {
        if (domain.imported) return false;
        if (domain.rooms.isEmpty() && validLegacyPosition(blessing)) {
            Room room = new Room(new BlockPos(blessing.getInt("pocket_x"), blessing.getInt("pocket_y"),
                    blessing.getInt("pocket_z")), Style.SANCTUM, true);
            room.stage = 2;
            domain.rooms.add(room);
        }
        if (blessing.contains("inventory", Tag.TAG_COMPOUND)) domain.inventory.loadFromTag(blessing.getCompound("inventory"));
        if (domain.returnPoint == null && blessing.contains("return_x") && blessing.getInt("return_x") != LEGACY_UNSET
                && !blessing.getString("return_dimension").equals("none")) {
            domain.returnPoint = new ReturnPoint(blessing.getString("return_dimension"),
                    new Vec3(blessing.getInt("return_x") + 0.5, blessing.getInt("return_y"),
                            blessing.getInt("return_z") + 0.5), 0, 0);
        }
        domain.imported = true;
        setDirty();
        return true;
    }

    public static boolean validLegacyPosition(CompoundTag tag) {
        return tag.contains("pocket_x") && tag.contains("pocket_y") && tag.contains("pocket_z")
                && Math.abs((long) tag.getInt("pocket_x")) < 29_000_000
                && Math.abs((long) tag.getInt("pocket_z")) < 29_000_000
                && tag.getInt("pocket_y") >= 0 && tag.getInt("pocket_y") < 240;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", 3);
        tag.putInt("NextSlot", nextSlot);
        ListTag owners = new ListTag();
        domains.values().forEach(domain -> owners.add(domain.save()));
        tag.put("Domains", owners);
        ListTag guests = new ListTag();
        visitors.forEach((player, visit) -> {
            CompoundTag guest = new CompoundTag(); guest.putUUID("Player", player); guest.putUUID("Host", visit.host);
            guest.put("Origin", visit.origin.save()); guests.add(guest);
        });
        tag.put("Visitors", guests);
        return tag;
    }

    public static PocketDomainData load(CompoundTag tag) {
        PocketDomainData data = new PocketDomainData();
        data.nextSlot = Math.max(0, tag.getInt("NextSlot"));
        for (Tag entry : tag.getList("Visitors", Tag.TAG_COMPOUND)) {
            CompoundTag guest = (CompoundTag) entry;
            if (guest.hasUUID("Player") && guest.hasUUID("Host"))
                data.visitors.put(guest.getUUID("Player"), new Visit(guest.getUUID("Host"), ReturnPoint.load(guest.getCompound("Origin"))));
        }
        for (Tag entry : tag.getList("Domains", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) entry;
            if (!saved.hasUUID("Owner")) continue;
            Domain domain = new Domain(saved.getUUID("Owner"), data::setDirty);
            domain.imported = saved.getBoolean("Imported");
            domain.selected = saved.getInt("Selected");
            domain.recallAfter = saved.getLong("RecallAfter");
            if (saved.contains("TowerOrigin")) domain.towerOrigin = BlockPos.of(saved.getLong("TowerOrigin"));
            domain.nextFloor = Math.max(0, Math.min(MAX_ROOMS, saved.getInt("NextFloor")));
            if (saved.contains("Entrance")) domain.entrance = ReturnPoint.load(saved.getCompound("Entrance"));
            if (saved.contains("Exit")) domain.exit = ReturnPoint.load(saved.getCompound("Exit"));
            domain.inventory.loadFromTag(saved.getCompound("Inventory"));
            if (saved.contains("Return")) domain.returnPoint = ReturnPoint.load(saved.getCompound("Return"));
            for (Tag room : saved.getList("Rooms", Tag.TAG_COMPOUND)) domain.rooms.add(Room.load((CompoundTag) room));
            for (Tag resident : saved.getList("Residents", Tag.TAG_COMPOUND)) {
                CompoundTag r = (CompoundTag) resident;
                if (!r.hasUUID("Id")) continue;
                domain.residents.put(r.getUUID("Id"), new Resident(r.getUUID("Id"), r.getString("Name"),
                        r.getInt("Room"), ReturnPoint.load(r.getCompound("Origin")), r.getInt("Revivals")));
            }
            data.domains.put(domain.owner, domain);
        }
        return data;
    }

    public enum Style { SANCTUM, GROVE, WORKSHOP, ARENA }
    public enum Rule { RESIDENT_REVIVAL, OWNER_RECALL, RESIDENT_COMBAT, NATURAL_SPAWNING, OWNER_BUILDING }

    public static final class Domain {
        public final UUID owner;
        public final List<Room> rooms = new ArrayList<>();
        public final Map<UUID, Resident> residents = new LinkedHashMap<>();
        public final PocketInventory inventory;
        public boolean imported;
        public int selected;
        public ReturnPoint returnPoint;
        public long recallAfter;
        public long nextAction;
        public BlockPos towerOrigin;
        public int nextFloor;
        public ReturnPoint entrance;
        // Legacy domain-wide anchor remains readable until its room's exit is explicitly changed.
        public ReturnPoint exit;
        public long nextPortal;
        public boolean leavePortalFirst;

        private Domain(UUID owner, Runnable dirty) {
            this.owner = owner;
            inventory = new PocketInventory() { @Override public void setChanged() { dirty.run(); } };
        }

        public Room selectedRoom() {
            selected = Math.max(0, Math.min(selected, rooms.size() - 1));
            return rooms.isEmpty() ? null : rooms.get(selected);
        }
        public boolean contains(BlockPos pos) { return rooms.stream().anyMatch(room -> room.ready() && room.contains(pos)); }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Owner", owner);
            tag.putBoolean("Imported", imported);
            tag.putInt("Selected", selected);
            tag.putLong("RecallAfter", recallAfter);
            if (towerOrigin != null) tag.putLong("TowerOrigin", towerOrigin.asLong());
            tag.putInt("NextFloor", nextFloor);
            if (entrance != null) tag.put("Entrance", entrance.save());
            if (exit != null) tag.put("Exit", exit.save());
            tag.put("Inventory", inventory.saveToTag());
            if (returnPoint != null) tag.put("Return", returnPoint.save());
            ListTag list = new ListTag();
            rooms.forEach(room -> list.add(room.save()));
            tag.put("Rooms", list);
            ListTag inhabitants = new ListTag();
            residents.values().forEach(resident -> inhabitants.add(resident.save()));
            tag.put("Residents", inhabitants);
            return tag;
        }
    }

    public static final class Room {
        public final BlockPos origin;
        public final Style style;
        public final boolean legacy;
        // 0 scan, 1 build, 2 ready, 3 resize scan, 4 resize apply. Persisted for restart recovery.
        public int stage;
        public int cursor;
        public Dimensions size = new Dimensions(SIZE, HEIGHT, SIZE);
        public Dimensions pendingSize;
        public boolean blocked;
        public ReturnPoint exit;
        public boolean exitClosed;
        public final java.util.Set<net.minecraft.world.level.ChunkPos> tickets = new java.util.HashSet<>();
        public int rules = (1 << Rule.RESIDENT_REVIVAL.ordinal()) | (1 << Rule.OWNER_RECALL.ordinal()) | (1 << Rule.OWNER_BUILDING.ordinal());
        public Room(BlockPos origin, Style style, boolean legacy) { this.origin = origin; this.style = style; this.legacy = legacy; }
        public boolean ready() { return stage == 2; }
        public boolean allows(Rule rule) { return (rules & (1 << rule.ordinal())) != 0; }
        public void setRule(Rule rule, boolean enabled) {
            if (enabled) rules |= 1 << rule.ordinal(); else rules &= ~(1 << rule.ordinal());
        }
        public boolean contains(BlockPos pos) {
            return contains(pos, legacy ? new Dimensions(29, 17, 31) : size);
        }
        public boolean contains(BlockPos pos, Dimensions dimensions) {
            int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
            return x >= 0 && x < dimensions.width && y >= 0 && y < dimensions.height && z >= 0 && z < dimensions.depth;
        }
        public Vec3 arrival() { return Vec3.atLowerCornerOf(origin).add(legacy ? 14.5 : size.width / 2 + 0.5, legacy ? 1 : 2,
                legacy ? 15.5 : size.depth / 2 + 0.5); }
        public Dimensions workSize() {
            return pendingSize == null ? size : new Dimensions(Math.max(size.width, pendingSize.width),
                    Math.max(size.height, pendingSize.height), Math.max(size.depth, pendingSize.depth));
        }
        public BlockPos at(int index) {
            Dimensions work = workSize();
            return origin.offset(index % work.width, index / (work.width * work.depth), index / work.width % work.depth);
        }
        public int progress() { return ready() ? 100 : (stage == 1 || stage == 4 ? 50 : 0) + cursor * 50 / workSize().volume(); }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Origin", origin.asLong()); tag.putString("Style", style.name()); tag.putBoolean("Legacy", legacy);
            tag.putInt("Stage", stage); tag.putInt("Cursor", cursor); tag.putInt("Rules", rules);
            tag.put("Size", size.save());
            if (pendingSize != null) tag.put("PendingSize", pendingSize.save());
            tag.putBoolean("Blocked", blocked);
            if (exit != null) tag.put("Exit", exit.save());
            tag.putBoolean("ExitClosed", exitClosed);
            return tag;
        }
        static Room load(CompoundTag tag) {
            Style style;
            try { style = Style.valueOf(tag.getString("Style")); } catch (IllegalArgumentException ex) { style = Style.SANCTUM; }
            Room room = new Room(BlockPos.of(tag.getLong("Origin")), style, tag.getBoolean("Legacy"));
            room.size = tag.contains("Size") ? Dimensions.load(tag.getCompound("Size")) : new Dimensions(SIZE, HEIGHT, SIZE);
            if (tag.contains("PendingSize")) room.pendingSize = Dimensions.load(tag.getCompound("PendingSize"));
            room.stage = Math.max(0, Math.min(4, tag.getInt("Stage")));
            if (room.stage >= 3 && room.pendingSize == null) room.stage = 2;
            room.cursor = Math.max(0, Math.min(room.workSize().volume() - 1, tag.getInt("Cursor")));
            room.blocked = tag.getBoolean("Blocked");
            if (tag.contains("Exit")) room.exit = ReturnPoint.load(tag.getCompound("Exit"));
            room.exitClosed = tag.getBoolean("ExitClosed");
            if (tag.contains("Rules")) room.rules = tag.getInt("Rules") & 31;
            return room;
        }
    }

    /** Dimensions include the protected shell and the decorative floor. UI shows usable interior sizes. */
    public record Dimensions(int width, int height, int depth) {
        public boolean valid() { return width >= MIN_SIZE && width <= MAX_SIZE && depth >= MIN_SIZE
                && depth <= MAX_SIZE && height >= MIN_HEIGHT && height <= MAX_HEIGHT; }
        public int volume() { return width * height * depth; }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putInt("Width", width); tag.putInt("Height", height); tag.putInt("Depth", depth); return tag;
        }
        public static Dimensions load(CompoundTag tag) {
            Dimensions size = new Dimensions(tag.getInt("Width"), tag.getInt("Height"), tag.getInt("Depth"));
            return size.valid() ? size : new Dimensions(SIZE, HEIGHT, SIZE);
        }
    }

    public record ReturnPoint(String dimension, Vec3 position, float yaw, float pitch) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putString("Dimension", dimension);
            tag.putDouble("X", position.x); tag.putDouble("Y", position.y); tag.putDouble("Z", position.z);
            tag.putFloat("Yaw", yaw); tag.putFloat("Pitch", pitch); return tag;
        }
        static ReturnPoint load(CompoundTag tag) {
            return new ReturnPoint(tag.getString("Dimension"), new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                    tag.getFloat("Yaw"), tag.getFloat("Pitch"));
        }
    }

    public record Resident(UUID id, String name, int room, ReturnPoint origin, int revivals) {
        public Resident revived() { return new Resident(id, name, room, origin, revivals + 1); }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("Id", id); tag.putString("Name", name);
            tag.putInt("Room", room); tag.put("Origin", origin.save()); tag.putInt("Revivals", revivals); return tag;
        }
    }
}
