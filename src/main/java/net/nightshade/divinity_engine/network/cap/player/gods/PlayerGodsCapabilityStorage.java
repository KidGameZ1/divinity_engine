package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.network.events.divinity.curse.CursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.curse.UncursePlayerEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.ContactGodEvent;
import net.nightshade.divinity_engine.network.events.divinity.gods.LoseFaithEvent;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;


@ApiStatus.Internal
public class PlayerGodsCapabilityStorage implements InternalGodsStorage {
    private static final Logger log = LogManager.getLogger(PlayerGodsCapabilityStorage.class);
    private final HashMap<ResourceLocation, BaseGodInstance> godsInstances = new HashMap();
    private final HashMap<ResourceLocation, CurseInstance> cursesInstances = new HashMap();
    private @Nullable Entity owner;

    public Collection<BaseGodInstance> getContactedGods() {
        return this.godsInstances.values();
    }

    public Collection<CurseInstance> getCurses() {
        return this.cursesInstances.values();
    }

    public void updateGodInstance(BaseGodInstance updatedInstance, boolean sync) {
        updatedInstance.markDirty();
        this.godsInstances.put(updatedInstance.getBaseGodId(), updatedInstance);
        if (sync) {
            this.syncChanges();
        }

    }

    public void updateBlessingInstance(BaseGodInstance instance, BlessingsInstance updateInstance, boolean sync) {
        instance.markDirty();
        updateInstance.markDirty();
        instance.addBlessing(updateInstance);
        if (sync) {
            this.syncChanges();
        }

    }

    public void updateCurseInstance(CurseInstance updatedInstance, boolean sync) {
        updatedInstance.markDirty();
        this.cursesInstances.put(updatedInstance.getCurseID(), updatedInstance);
        if (sync) {
            this.syncChanges();
        }

    }

    public boolean contactGod(BaseGodInstance instance) {
        if (this.owner == null) {
            log.error("Cannot contact god: no owner set");
            return false;
        } else if (this.godsInstances.containsKey(instance.getBaseGodId())) {
            log.debug("Tried to register a duplicate of {} to {}.", instance.getBaseGodId(), this.owner.getName().getString());
            return false;
        } else if (!MinecraftForge.EVENT_BUS.post(new ContactGodEvent(instance, this.owner))) {
            log.info("Adding god {} to player {}", instance.getBaseGodId(), this.owner.getName().getString());
            instance.markDirty();
            this.godsInstances.put(instance.getBaseGodId(), instance);
            log.info("Current gods count: {}", this.godsInstances.size());
            this.syncChanges();
            return true;
        } else {
            log.debug("Contact god event was cancelled");
            return false;
        }
    }

    public boolean curse(CurseInstance instance, @Nullable BaseGod boundGod) {
        if (boundGod != null) {
            instance.setBoundGod(getContactedGod(boundGod).get());
        }else {
            instance.setBoundGod(null);
        }
        if (this.owner == null) {
            log.error("Cannot curse player: no owner set");
            return false;
        } else if (this.cursesInstances.containsKey(instance.getCurseID())) {
//            log.debug("Tried to register a duplicate of {} to {}.", instance.getCurseID(), this.owner.getName().getString());
            return false;
        } else if (!MinecraftForge.EVENT_BUS.post(new CursePlayerEvent(instance, this.owner))) {
            log.info("Adding curse {} to player {}", instance.getCurseID(), this.owner.getName().getString());
            instance.markDirty();
            if (owner instanceof Player player && !getCurses().contains(instance)) {
                if (instance.getBoundGod() != null)
                    if (getContactedGod(instance.getBoundGod()).isPresent())
                        player.displayClientMessage(Component.translatable(instance.getBoundGod().getCurseMessageTranslationKey()).withStyle(ChatFormatting.GRAY), false);
            }
            this.cursesInstances.put(instance.getCurseID(), instance);
            log.info("Current curse count: {}", this.godsInstances.size());
            this.syncChanges();
            return true;
        } else {
            log.debug("Curse player event was cancelled");
            return false;
        }
    }



    public Optional<BaseGodInstance> getContactedGod(BaseGod god) {
        return this.godsInstances.values().parallelStream().filter((godInstance) -> godInstance.getBaseGod().equals(god)).findFirst();
    }

    public Optional<CurseInstance> getCurse(Curse curse) {
        return this.cursesInstances.values().parallelStream().filter((curseInstance) -> curseInstance.getCurseID().equals(curse)).findFirst();
    }

    public void loseContactedGod(BaseGodInstance instance) {
        if (this.owner != null) {
            if (this.godsInstances.containsKey(instance.getBaseGodId())) {
                if (!MinecraftForge.EVENT_BUS.post(new LoseFaithEvent(instance, this.owner))) {

                    instance.markDirty();
                    this.getContactedGods().remove(instance);
                    this.syncChanges();
                }

            }
        }
    }

    public void uncurse(CurseInstance instance) {
        if (this.owner != null) {
            if (this.cursesInstances.containsKey(instance.getCurseID())) {
                if (!MinecraftForge.EVENT_BUS.post(new UncursePlayerEvent(instance, this.owner))) {
                    if (owner instanceof Player player && getCurses().contains(instance)) {
                        if (instance.getBoundGod() != null)
                            if (getContactedGod(instance.getBoundGod()).isPresent())
                                player.displayClientMessage(Component.translatable(instance.getBoundGod().getUncurseMessageTranslationKey()).withStyle(ChatFormatting.GRAY), false);
                    }
                    instance.markDirty();
                    this.getCurses().remove(instance);
                    this.syncChanges();
                }
            }
        }
    }

    public void loseContactedGod(BaseGod god) {
        if (this.owner != null) {
            Optional<BaseGodInstance> optional = this.getContactedGod(god);
            if (!optional.isEmpty()) {
                if (!MinecraftForge.EVENT_BUS.post(new LoseFaithEvent((BaseGodInstance)optional.get(), this.owner))) {
                    ((BaseGodInstance)optional.get()).markDirty();
                    this.getContactedGods().remove(optional.get());
                    this.syncChanges();
                }

            }
        }
    }
    public void loseContactAllGods() {
        if (this.owner != null) {
            Collection<BaseGodInstance> list = this.getContactedGods();
            if (!list.isEmpty()) {
                this.getContactedGods().clear();
                syncAll();
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag godsList = new ListTag();
        ListTag curseList = new ListTag();
        if (this.godsInstances.isEmpty()) {
            tag.putString("contacted_gods", "none");
        }else {
            this.godsInstances.values().forEach((instance) -> godsList.add(instance.toNBT()));
        }

        if (this.cursesInstances.isEmpty()) {
            tag.putString("curses", "none");
        }else {
            this.cursesInstances.values().forEach((instance) -> curseList.add(instance.toNBT()));
        }

        tag.put("contacted_gods", godsList);
        tag.put("curses", curseList);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("resetExistingData")) {
            this.godsInstances.clear();
            this.cursesInstances.clear();
        }

        ListTag godsList = nbt.getList("contacted_gods", 10);
        ListTag cursesList = nbt.getList("curses", 10);

        if (nbt.getString("contacted_gods").equals("none")){
            this.godsInstances.clear();
        }else {
            for(Tag tag : godsList) {
                if (tag instanceof CompoundTag compoundTag) {
                    try {
                        BaseGodInstance instance = BaseGodInstance.fromNBT(compoundTag);

                        this.godsInstances.put(instance.getBaseGodId(), instance);
                    } catch (Exception exception) {
                        log.error("Exception while deserializing tag {}.\n{}", tag, exception);
                    }
                } else {
                    log.error("Tag is not a Compound! Exception while deserializing tag {}.", tag);
                }
            }
        }
        if (nbt.getString("curses").equals("none")){
            this.cursesInstances.clear();
        }else {
            for(Tag tag : cursesList) {
                if (tag instanceof CompoundTag compoundTag) {
                    try {
                        CurseInstance instance = CurseInstance.fromNBT(compoundTag);
                        this.cursesInstances.put(instance.getCurseID(), instance);
                    } catch (Exception exception) {
                        log.error("Exception while deserializing tag {}.\n{}", tag, exception);
                    }
                }
            }
        }
    }


    public @Nullable Entity getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
    }
}