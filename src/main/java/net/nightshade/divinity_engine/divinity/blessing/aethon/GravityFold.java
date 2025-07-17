package net.nightshade.divinity_engine.divinity.blessing.aethon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.nightshade.divinity_engine.DivinityEngineMod;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.blessing.BlessingsInstance;
import net.nightshade.divinity_engine.util.DivinityEngineHelper;
import net.nightshade.nightshade_core.util.MiscHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Random;

public class GravityFold extends Blessings {
    /**
     * Creates a new blessing with the specified parameters.
     *
     * @param neededFavor Amount of favor required to activate
     * @param cooldown    Cooldown period in seconds
     * @param isPassive   Is a passive blessing or not
     * @param isActive
     * @param canToggle   Whether blessing can be toggled
     * @param textColor   Color for blessing text
     */
    public GravityFold(int neededFavor, int cooldown, boolean isPassive, boolean isActive, boolean canToggle, Color textColor) {
        super(neededFavor, cooldown, isPassive, isActive, canToggle, textColor);
    }

    @Override
    public <T extends BlessingsInstance> T createDefaultInstance() {
        T instance = super.createDefaultInstance();

        if (instance.getOrCreateTag().contains("grav_tick_counter")) {
            instance.getOrCreateTag().putInt("grav_tick_counter", 0);
        }


        return instance;
    }

    @Override
    public boolean onTick(BlessingsInstance instance, LivingEntity living) {
        if (instance.getOrCreateTag().getInt("grav_tick_counter") >= MiscHelper.secondsToTick(2)) {
            living.setNoGravity(false);
            if (living.onGround()){
                instance.getOrCreateTag().putInt("grav_tick_counter", 0);
                return true;
            }
        }
        if (!living.onGround()){
            if (DivinityEngineHelper.isKeyPressed(GLFW.GLFW_KEY_SPACE)){
                if (instance.getOrCreateTag().getInt("grav_tick_counter") <= MiscHelper.secondsToTick(2)) {
                    living.setNoGravity(true);
                    if (living.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 10; i++) {
                            Random r = new Random();
                            double angle = serverLevel.getRandom().nextDouble() * 2 * Math.PI;
                            double radius = serverLevel.getRandom().nextDouble() * 1.5;
                            double x = living.getX() + Math.cos(angle) * radius;
                            double y = living.getY() + 0.02;
                            double z = living.getZ() + Math.sin(angle) * radius;

                            serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0, r.nextDouble(0, 0.5), 0, r.nextDouble(0, 0.2));
                        }
                    }
                    instance.getOrCreateTag().putInt("grav_tick_counter", instance.getOrCreateTag().getInt("grav_tick_counter") + 2);
                    System.out.println("grav tick counter: " + instance.getOrCreateTag().getInt("grav_tick_counter"));
                }
            }else {
                living.setNoGravity(false);
                if (!living.onGround()) return true;
            }
        }else {
            living.setNoGravity(false);
        }
        return super.onTick(instance, living);
    }
}
