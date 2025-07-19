package net.nightshade.divinity_engine.network.cap.player.gods;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import org.jetbrains.annotations.Nullable;

public interface IMainPlayerCapability extends INBTSerializable<CompoundTag> {
    
    @Nullable BlessingsInstance getBlessingSlot1();
    void setBlessingSlot1(@Nullable BlessingsInstance magicSlot1);
    @Nullable BlessingsInstance getBlessingSlot2();
    void setBlessingSlot2(@Nullable BlessingsInstance magicSlot2);
    @Nullable BlessingsInstance getBlessingSlot3();
    void setBlessingSlot3(@Nullable BlessingsInstance magicSlot3);

    @Nullable int getBlessingsPageNum();
    void setBlessingsPageNum(int num);
    void increaseBlessingsPageNum(int num);
    void decreaseBlessingsPageNum(int num);


    int getDcPageNum();
    void setDCPageNum(int num);
    void increaseDCPageNum(int num);
    void decreaseDCPageNum(int num);


    boolean hasDivineCodex();
    void setHasDivineCodex(boolean hasDivineCodex);
}
