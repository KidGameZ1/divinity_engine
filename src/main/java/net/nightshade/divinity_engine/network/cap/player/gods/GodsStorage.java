package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NonExtendable
public interface GodsStorage extends INBTSerializable<CompoundTag> {
    Collection<BaseGodInstance> getContactedGods();

    Collection<CurseInstance> getCurses();


    void updateGodInstance(BaseGodInstance var1);

    void updateGodInstances(List<BaseGodInstance> var1);

    void updateGodInstances(BaseGodInstance... var1);

    boolean contactGod(BaseGod var1);

    boolean contactGod(BaseGodInstance var1);

    void loseContactedGod(BaseGodInstance var1);

    void loseContactedGod(BaseGod var1);

    void updateCurseInstances(CurseInstance... var1);

    boolean curse(Curse var1, BaseGod var2);

    boolean curse(CurseInstance var1, BaseGod var2);

    void uncurse(CurseInstance var1);

    void uncurse(Curse var1);

    Optional<BaseGodInstance> getContactedGod(BaseGod var1);
    Optional<CurseInstance> getCurse(Curse var1);

    void syncChanges();

    void syncAll();

    void syncPlayer(Player var1);
}
