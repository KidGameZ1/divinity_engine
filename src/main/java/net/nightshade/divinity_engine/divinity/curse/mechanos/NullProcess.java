package net.nightshade.divinity_engine.divinity.curse.mechanos;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.nightshade.divinity_engine.divinity.curse.Curse;
import net.nightshade.divinity_engine.divinity.curse.CurseInstance;

import java.util.Random;

public class NullProcess extends Curse {

    @Override
    public void onItemCrafted(CurseInstance instance, PlayerEvent.ItemCraftedEvent event) {
        Random random = new Random();
        if (random.nextFloat() < 0.1f){
            if (event.getCrafting().isDamageableItem()){
                event.getCrafting().shrink(event.getCrafting().getCount());
            }
        }
        super.onItemCrafted(instance, event);
    }

    @Override
    public void onRightClickBlock(CurseInstance instance, PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().getBlockState(event.getHitVec().getBlockPos()).is(Blocks.CRAFTING_TABLE) || player.level().getBlockState(event.getHitVec().getBlockPos()).is(Blocks.SMITHING_TABLE)){
            player.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 1.5f);
            player.displayClientMessage(Component.literal("“You feel the spark leave your tools...”"), true);
        }

        super.onRightClickBlock(instance, event);
    }
}
