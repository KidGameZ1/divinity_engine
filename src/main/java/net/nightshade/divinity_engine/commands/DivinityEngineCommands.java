package net.nightshade.divinity_engine.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nightshade.divinity_engine.commands.arg.GodsArgument;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.divinity.gods.BaseGodInstance;
import net.nightshade.divinity_engine.util.MainPlayerCapabilityHelper;
import net.nightshade.divinity_engine.util.divinity.gods.GodHelper;

import java.util.Objects;

@Mod.EventBusSubscriber
public class DivinityEngineCommands {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		contactGodsCommand(event);
		getCommands(event);
		resetCooldowns(event);
	}

	public static void contactGodsCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("contact")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("god-id", GodsArgument.god())
										.executes(arguments -> {
											Entity entityCalledOn = (new Object() {
												public Entity getEntity() {
													try {
														return EntityArgument.getEntity(arguments, "player");
													} catch (CommandSyntaxException e) {
														e.printStackTrace();
														return null;
													}
												}
											}.getEntity());
											Entity commandCaller = arguments.getSource().getEntity();
											BaseGod god = GodsArgument.getGod(arguments, "god-id");


											if (god != null) {
												if (commandCaller instanceof Player player) {
													if (GodHelper.contactGod(god, (LivingEntity) entityCalledOn)) {
														player.displayClientMessage(Component.literal(entityCalledOn.getDisplayName().getString() + " has contacted " + Component.translatable(god.getNameTranslationKey()).getString()), false);
													} else {
														player.displayClientMessage(Component.literal(entityCalledOn.getDisplayName().getString() + " is already in contact with " + Component.translatable(god.getNameTranslationKey()).getString()), false);
													}
												}else {
													GodHelper.contactGod(god, (LivingEntity) entityCalledOn);
												}
											}
											return 0;
										}))))));

		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("contact")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("all")
												.executes(arguments -> {
													Entity entityCalledOn = (new Object() {
														public Entity getEntity() {
															try {
																return EntityArgument.getEntity(arguments, "player");
															} catch (CommandSyntaxException e) {
																e.printStackTrace();
																return null;
															}
														}
													}.getEntity());
													Entity commandCaller = arguments.getSource().getEntity();
													if (entityCalledOn instanceof LivingEntity player) {
														GodHelper.contactAllGods(player);
													}

													return 0;
												}))))));

		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("lose_faith")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("god-id", GodsArgument.god())
												.executes(arguments -> {
													Entity entityCalledOn = (new Object() {
														public Entity getEntity() {
															try {
																return EntityArgument.getEntity(arguments, "player");
															} catch (CommandSyntaxException e) {
																e.printStackTrace();
																return null;
															}
														}
													}.getEntity());
													Entity commandCaller = arguments.getSource().getEntity();
													BaseGod god = GodsArgument.getGod(arguments, "god-id");


													if (god != null) {
														if (commandCaller instanceof Player player) {
															GodHelper.getContactedGodsFrom((LivingEntity) entityCalledOn).loseContactedGod(god);
														}
													}
													return 0;
												}))))));
		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("lose_faith")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("all")
												.executes(arguments -> {
													Entity entityCalledOn = (new Object() {
														public Entity getEntity() {
															try {
																return EntityArgument.getEntity(arguments, "player");
															} catch (CommandSyntaxException e) {
																e.printStackTrace();
																return null;
															}
														}
													}.getEntity());
													Entity commandCaller = arguments.getSource().getEntity();
													if (entityCalledOn instanceof LivingEntity player) {
														GodHelper.loseFaithAllGods(player);
													}
													return 0;
												}))))));

		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("favor")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("god-id", GodsArgument.god())
												.then(Commands.argument("favor", IntegerArgumentType.integer(-100, 100))
													.executes(arguments -> {
														Entity entityCalledOn = (new Object() {
															public Entity getEntity() {
																try {
																	return EntityArgument.getEntity(arguments, "player");
																} catch (CommandSyntaxException e) {
																	e.printStackTrace();
																	return null;
																}
															}
														}.getEntity());
														Entity commandCaller = arguments.getSource().getEntity();
														BaseGod god = GodsArgument.getGod(arguments, "god-id");
														int favor = IntegerArgumentType.getInteger(arguments, "favor");


														if (god != null) {
															if (commandCaller instanceof Player player) {
																if (GodHelper.hasContactedGod(entityCalledOn, god)){
																	GodHelper.getGodOrNull(entityCalledOn, god).setFavor(favor);
																	player.displayClientMessage(Component.literal(entityCalledOn.getDisplayName().getString() + " has favor set to " + favor), false);
																}
															}
														}
														return 0;
												})))))));
		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("favor")
						.then(Commands.literal("god")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("all")
												.then(Commands.argument("favor", IntegerArgumentType.integer(-100, 100))
														.executes(arguments -> {
															Entity entityCalledOn = (new Object() {
																public Entity getEntity() {
																	try {
																		return EntityArgument.getEntity(arguments, "player");
																	} catch (CommandSyntaxException e) {
																		e.printStackTrace();
																		return null;
																	}
																}
															}.getEntity());
															Entity commandCaller = arguments.getSource().getEntity();
															int favor = IntegerArgumentType.getInteger(arguments, "favor");


															if (entityCalledOn instanceof Player player) {
																for (BaseGodInstance instance : GodHelper.getAllContactedGods(player)) {
																	instance.setFavor(favor);
																}
																player.displayClientMessage(Component.literal(entityCalledOn.getDisplayName().getString() + " has favor set to " + favor), false);
															}
															return 0;
														})))))));
	}

	public static void getCommands(RegisterCommandsEvent event){
		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("get")
						.then(Commands.literal("gods")
								.then(Commands.argument("player", EntityArgument.player()).executes(arguments -> {
									Entity entityCalledOn = (new Object() {
										public Entity getEntity() {
											try {
												return EntityArgument.getEntity(arguments, "player");
											} catch (CommandSyntaxException e) {
												e.printStackTrace();
												return null;
											}
										}
									}.getEntity());
									Entity commandCaller = arguments.getSource().getEntity();

									if (commandCaller instanceof Player player){
										if (!GodHelper.getContactedGodsFrom(entityCalledOn).getContactedGods().isEmpty()){
											for(int i = 0; i < GodHelper.getContactedGodsFrom(entityCalledOn).getContactedGods().size(); ++i) {
												player.displayClientMessage(Component.translatable((GodHelper.getContactedGodsFrom(entityCalledOn).getContactedGods().stream().toList().get(i).getBaseGod().getNameTranslationKey())), false);
											}
										} else {
											player.displayClientMessage(Component.literal(entityCalledOn.getDisplayName().getString()+" has no gods"), false);
										}
									}

									return 0;
								})))));
	}

	public static void resetCooldowns(RegisterCommandsEvent event){
		event.getDispatcher().register(Commands.literal("divinity_engine").requires(s -> s.hasPermission(4))
				.then(Commands.literal("reset_cooldowns")
					.then(Commands.argument("player", EntityArgument.player()).executes(arguments -> {
						Entity entityCalledOn = (new Object() {
							public Entity getEntity() {
								try {
									return EntityArgument.getEntity(arguments, "player");
								} catch (CommandSyntaxException e) {
									e.printStackTrace();
									return null;
								}
							}
						}.getEntity());
						Entity commandCaller = arguments.getSource().getEntity();

							if (!GodHelper.getAllBlessingsInstances(entityCalledOn).isEmpty()){
								GodHelper.getAllBlessingsInstances(entityCalledOn).forEach((blessing) -> {
									blessing.setCooldown(0);
								});
							}
						if (MainPlayerCapabilityHelper.getBlessingSlot1(entityCalledOn) != null) {
							BlessingsInstance instance = Objects.requireNonNull(MainPlayerCapabilityHelper.getBlessingSlot1(entityCalledOn));
							instance.setCooldown(0);
							MainPlayerCapabilityHelper.setBlessingSlot1(entityCalledOn, instance);
						}
						if (MainPlayerCapabilityHelper.getBlessingSlot2(entityCalledOn) != null) {
							BlessingsInstance instance = Objects.requireNonNull(MainPlayerCapabilityHelper.getBlessingSlot2(entityCalledOn));
							instance.setCooldown(0);
							MainPlayerCapabilityHelper.setBlessingSlot2(entityCalledOn, instance);
						}
						if (MainPlayerCapabilityHelper.getBlessingSlot3(entityCalledOn) != null) {
							BlessingsInstance instance = Objects.requireNonNull(MainPlayerCapabilityHelper.getBlessingSlot3(entityCalledOn));
							instance.setCooldown(0);
							MainPlayerCapabilityHelper.setBlessingSlot3(entityCalledOn, instance);
						}

						return 0;
					}))));
	}
}
