package net.nightshade.divinity_engine.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.blessing.solarius.RadiantStrike;
import net.nightshade.divinity_engine.registry.divinity.blessing.BlessingsRegistry;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;


public class Test extends Item {
    public Test() {
        super(new Properties().stacksTo(1));
    }

}
