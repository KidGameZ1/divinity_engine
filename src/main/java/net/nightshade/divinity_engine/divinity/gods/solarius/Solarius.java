package net.nightshade.divinity_engine.divinity.gods.solarius;

import net.minecraftforge.registries.RegistryObject;
import net.nightshade.divinity_engine.divinity.blessing.Blessings;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;

import java.awt.*;
import java.util.Set;

public class Solarius extends BaseGod {
    public Solarius(Set<String> domains, Set<RegistryObject<Blessings>> blessings) {
        super(domains, blessings);
    }

    @Override
    public Color getColor() {
        return Color.red;
    }
}
