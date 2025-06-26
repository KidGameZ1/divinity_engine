package net.nightshade.divinity_engine.divinity.gods;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.LoseFaithEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.OfferEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.worship_events.PrayEvent;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class BaseGodInstance implements Cloneable {
    private int favor = 0;

    private Set<BlessingsInstance> blessingsInstances = new HashSet<BlessingsInstance>();

    private boolean dirty = false;

    @Nullable
    private CompoundTag tag;
    private final Holder.Reference<BaseGod> godsReference;

    public BaseGodInstance(BaseGod god) {
        this.godsReference = GodsRegistry.GODS_REGISTRY.get().getDelegateOrThrow(god);


    }

    public BaseGod getBaseGod() {
        return (BaseGod)this.godsReference.get();
    }

    public ResourceLocation getBaseGodId() {
        return this.godsReference.key().location();
    }

    public BaseGodInstance clone() {
        BaseGodInstance clone = new BaseGodInstance(this.getBaseGod());
        clone.dirty = this.dirty;
        return clone;
    }

    @ApiStatus.NonExtendable
    public final CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("God", this.getBaseGodId().toString());
        tag.putInt("Favor", this.favor);

        // Save blessings
        ListTag blessingsTag = new ListTag();
        for (BlessingsInstance blessing : this.blessingsInstances) {
            try {
                CompoundTag blessingTag = new CompoundTag();
                ResourceLocation blessingId = BlessingsRegistry.BLESSINGS_REGISTRY.get()
                        .getKey(blessing.getBlessing());
                if (blessingId != null) {
                    blessingTag.putString("Type", blessingId.toString());
                    blessingTag.putInt("Cooldown", blessing.getCooldown());

                    // Save blessing's custom data if any
                    CompoundTag blessingData = new CompoundTag();
                    blessing.serialize(blessingData);
                    if (!blessingData.isEmpty()) {
                        blessingTag.put("Data", blessingData);
                    }

                    blessingsTag.add(blessingTag);
                }
            } catch (Exception e) {
                System.err.println("Failed to save blessing: " + e.getMessage());
            }
        }
        tag.put("Blessings", blessingsTag);

        // Save custom data
        if (this.tag != null) {
            tag.put("CustomData", this.tag.copy());
        }

        return tag;
    }



    public CompoundTag serialize(CompoundTag tag) {
        tag.putInt("Favor", this.favor);

        ListTag blessingsList = new ListTag();
        for (BlessingsInstance instance : blessingsInstances) {
            CompoundTag blessingTag = new CompoundTag();
            // Save the blessing type
            blessingTag.putString("BlessingType", BlessingsRegistry.BLESSINGS_REGISTRY.get()
                    .getKey(instance.getBlessing()).toString());
            // Save the blessing's data
            instance.serialize(blessingTag);
            blessingsList.add(blessingTag);
        }
        tag.put("Blessings", blessingsList);



        if (this.tag != null) {
            tag.put("tag", this.tag.copy());
        }

        return tag;
    }

    public void deserialize(CompoundTag tag) {
        this.favor = tag.getInt("Favor");

        // Handle blessings
        if (tag.contains("Blessings", 10)) { // 10 is the NBT tag type for CompoundTag
            blessingsInstances.clear();
            ListTag blessingsList = tag.getList("Blessings", 10);

            for (int i = 0; i < blessingsList.size(); i++) {
                CompoundTag blessingTag = blessingsList.getCompound(i);
                try {
                    // Get the blessing type from the registry
                    ResourceLocation blessingId = new ResourceLocation(blessingTag.getString("BlessingType"));
                    var blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);

                    if (blessing != null) {
                        // Create new instance and load its data
                        BlessingsInstance instance = blessing.createDefaultInstance();
                        instance.setBoundGod(this);
                        instance.deserialize(blessingTag);
                        addBlessing(instance);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load blessing: " + e.getMessage());
                }
            }
        }


        if (tag.contains("tag", 10)) {
            this.tag = tag.getCompound("tag");
        }

    }

    @ApiStatus.NonExtendable
    public static BaseGodInstance fromNBT(CompoundTag tag) {
        if (!tag.contains("God")) {
            throw new IllegalArgumentException("Missing God ID in NBT");
        }

        ResourceLocation godId = new ResourceLocation(tag.getString("God"));
        BaseGod god = GodsRegistry.GODS_REGISTRY.get().getValue(godId);
        if (god == null) {
            throw new IllegalArgumentException("Unknown god: " + godId);
        }

        BaseGodInstance instance = god.createGodDefaultInstance();
        instance.favor = tag.getInt("Favor");

        // Clear default blessings before loading saved ones
        instance.blessingsInstances.clear();

        // Load blessings
        if (tag.contains("Blessings", 9)) {
            ListTag blessingsTag = tag.getList("Blessings", 10);
            for (int i = 0; i < blessingsTag.size(); i++) {
                CompoundTag blessingTag = blessingsTag.getCompound(i);
                try {
                    String blessingIdStr = blessingTag.getString("Type");
                    if (!blessingIdStr.isEmpty()) {
                        ResourceLocation blessingId = new ResourceLocation(blessingIdStr);
                        Blessings blessing = BlessingsRegistry.BLESSINGS_REGISTRY.get().getValue(blessingId);

                        if (blessing != null) {
                            BlessingsInstance bInstance = blessing.createDefaultInstance();
                            bInstance.setBoundGod(instance);

                            // Set cooldown
                            if (blessingTag.contains("Cooldown")) {
                                bInstance.setCooldown(blessingTag.getInt("Cooldown"));
                            }

                            // Load blessing's custom data
                            if (blessingTag.contains("Data")) {
                                bInstance.deserialize(blessingTag.getCompound("Data"));
                            }

                            instance.addBlessing(bInstance);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load blessing: " + e.getMessage());
                }
            }
        }

        // If no blessings were loaded, initialize with defaults
        if (instance.blessingsInstances.isEmpty() && god.getBlessings() != null) {
            for (RegistryObject<Blessings> blessingReg : god.getBlessings()) {
                if (blessingReg != null && blessingReg.isPresent()) {
                    BlessingsInstance bInstance = blessingReg.get().createDefaultInstance();
                    bInstance.setBoundGod(instance);
                    instance.addBlessing(bInstance);
                }
            }
        }

        // Load custom data
        if (tag.contains("CustomData")) {
            instance.tag = tag.getCompound("CustomData").copy();
        }

        return instance;
    }




    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    @ApiStatus.Internal
    public void resetDirty() {
        this.dirty = false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BaseGodInstance instance = (BaseGodInstance)o;
            return this.getBaseGodId().equals(instance.getBaseGodId()) && this.godsReference.key().registry().equals(instance.godsReference.key().registry());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.godsReference});
    }

    public int getFavor() {
        return this.favor;
    }

    public void setFavor(int favor) {
        this.favor = favor;
        this.markDirty();
    }

    public void decreaseFavor(int favor) {
        this.favor -= favor;
        this.markDirty();
    }
    public void increaseFavor(int favor) {
        this.favor += favor;
        this.markDirty();
    }

    public Set<BlessingsInstance> getBlessingsInstances() {
        return blessingsInstances;
    }

    public void addBlessing(BlessingsInstance newInstance) {
        // Remove any existing instance of this blessing (matches by blessing type)
        blessingsInstances.removeIf(instance ->
                instance.getBlessing().equals(newInstance.getBlessing())
        );
        blessingsInstances.add(newInstance);
        markDirty();
    }



    public void removeBlessing(BlessingsInstance blessingsInstance) {
        for (BlessingsInstance instance : blessingsInstances) {
            if (instance.equals(blessingsInstance)) {
                blessingsInstances.remove(instance);
                markDirty();
                return;
            }
        }
    }
    public void removeBlessing(Blessings blessings) {
        for (BlessingsInstance instance : blessingsInstances) {
            if (instance.getBlessing() == blessings) {
                removeBlessing(instance);
                return;
            }
        }
    }
    public BlessingsInstance getBlessing(Blessings blessings) {
        for (BlessingsInstance instance : blessingsInstances) {
            if (instance.getBlessing() == blessings) {
                return instance;
            }
        }
        return null;
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    public CompoundTag getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new CompoundTag());
        }

        return this.tag;
    }

    public void setTag(@Nullable CompoundTag tag) {
        this.tag = tag;
        this.markDirty();
    }

    public void onPrayedTo(PrayEvent event) {
        this.getBaseGod().onPrayedTo(this,event);
    }

    public void onOfferedItems(OfferEvent.OfferItemEvent event) {
        this.getBaseGod().onOfferedItems(this,event);
    }

    public void onOfferedEntity(OfferEvent.OfferEntityEvent event) {
        this.getBaseGod().onOfferedEntity(this,event);
    }
    public void onContact(LivingEntity living, ContactGodEvent event) {
        this.getBaseGod().onContacted(living, event);
    }
    public void onLoseFaith(LivingEntity living, LoseFaithEvent event) {
        this.getBaseGod().onLoseFaith(living, event);
    }
}
