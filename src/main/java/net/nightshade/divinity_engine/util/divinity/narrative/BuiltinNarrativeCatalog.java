package net.nightshade.divinity_engine.util.divinity.narrative;

import java.util.List;

/** Plain data kept independent of registries so every built-in path can be tested without a game. */
public final class BuiltinNarrativeCatalog {
    public record Path(String name, String type, String condition, int target, String item, String deed, String reward, String ending) {}
    public record God(String id, String title, String blessing, String request, Path a, Path b) {}
    private static Path path(String name, String type, String condition, int target, String item, String deed, String reward, String ending) {
        return new Path(name, type, condition, target, item, deed, reward, ending);
    }
    public static List<God> entries() {
        return List.of(
            new God("solarius", "Dawn's Judgment", "solar_ward", "A dawn can expose the wicked or shelter the weary. Bring an offering, then choose what your light will mean.",
                path("Judgment", "kill", "undead_day", 8, "", "Defeat eight undead in daylight", "Solar Ward lasts 15 seconds instead of 10", "You faced what others feared to name. Let your light endure: judgment need not be cruelty."),
                path("Sanctuary", "donation", "", 16, "bread", "Contribute 16 bread", "Solar Ward can activate below 65% health instead of 50%", "You made room beneath the dawn for the hungry. Your shelter, too, is justice.")),
            new God("nashara", "The Serpent's Answer", "serpents_bite", "Power can announce itself with venom or pass unseen. Bring your offering, and decide which answer belongs to you.",
                path("Venom", "kill", "poisoned", 4, "", "Defeat four poisoned hostile mobs", "Serpent's Bite poison lasts two seconds longer", "Your venom speaks precisely. You have learned that a single touch may finish what a hundred blows cannot."),
                path("Subtlety", "hidden", "", 600, "", "Remain invisible and sneaking near this shrine for 30 continuous seconds", "Successful Serpent's Bite poison also applies Weakness I for two seconds", "You left no footprint upon my silence. Let uncertainty become your sharpest fang.")),
            new God("grond", "What Endures", "stoneform", "A thing may endure because it was well made, or because it refuses to break. Bring a gift to the forge and choose your answer.",
                path("Craft", "smelt", "", 16, "", "Collect 16 iron ingots you have smelted from a furnace", "Stoneform resistance lasts 12 seconds instead of 10", "Your hands gave the ore a purpose. What you build will remember the patience of its maker."),
                path("Endurance", "mine", "", 32, "", "Mine 32 stone blocks below Y=0", "Stoneform begins a 20-second cooldown instead of 25 seconds", "Beneath the weight of the world, you continued. The mountain knows one of its own.")),
            new God("varun", "The Pack's Claim", "savage_hunger", "A pack lives by the hunter and the keeper alike. Bring an offering, then show me whom your strength will feed.",
                path("Nurture", "breed", "", 4, "", "Breed four animals", "Savage Hunger additionally heals one HP on successful activation", "New lives answer your care. The pack's future is a victory worth defending."),
                path("Hunt", "kill", "forest", 8, "", "Defeat eight hostile mobs in forest biomes", "Savage Hunger kill buffs last seven seconds instead of five", "The forest breathes easier behind your tracks. Hunt for your pack, not merely for yourself.")),
            new God("dravak", "The Meaning of Valor", "battle_rush", "Valor may strike first or hold the line. Offer freely, then prove the courage you intend to carry.",
                path("Conquest", "kill", "melee", 8, "", "Defeat eight hostile mobs with melee attacks", "Battle Rush Strength lasts seven seconds instead of five", "You met the enemy within arm's reach. Remember the cost of every victory you claim."),
                path("Resolve", "shield", "", 8, "", "Successfully shield-block eight attacks from hostile mobs", "Battle Rush also grants Resistance I for two seconds", "Eight times you held. Not all courage advances; some courage lets everyone else survive.")),
            new God("elaris", "Two Roads to Wisdom", "foresight", "The page and the lived moment both teach. Bring your offering, then choose the teacher whose lessons you will honor.",
                path("Study", "donation", "", 3, "enchanted_book", "Contribute three enchanted books", "Foresight permits four dodges per cycle instead of three", "You preserved wisdom rather than hoarding it. A shared insight can shelter many lives."),
                path("Experience", "xp", "", 100, "", "Gain 100 experience points", "Foresight begins a 16-second cooldown instead of 20 seconds", "No page could have lived those moments for you. Let experience become understanding, not certainty.")),
            new God("voltira", "The Storm's Will", "stormstep", "Lightning may chase the horizon or seek a worthy conduit. Bring your gift, then give the storm direction.",
                path("Pursuit", "kill", "projectile", 8, "", "Defeat eight hostile mobs with projectiles", "Stormstep horizontal dash power increases from 2.5 to 3.0", "Your aim outran hesitation. Carry the thunder forward, but choose where it lands."),
                path("Conduit", "donation", "", 2, "lightning_rod", "Contribute two lightning rods", "Stormstep vertical-dash Slow Falling lasts eight seconds instead of five", "You gave the storm a place to rest. Even lightning needs a hand that can guide it.")),
            new God("vokar", "The Last Farewell", "soul_leech", "The dead need release; the living need remembrance. Bring an offering, and tell me how you will keep that balance.",
                path("Release", "kill", "undead_night", 8, "", "Defeat eight undead at night", "Soul Leech successful healing restores three HP instead of two", "Their unrest has ended. In the space they leave, life is permitted to continue."),
                path("Remembrance", "donation", "", 8, "soul_sand", "Contribute eight soul sand", "Soul Leech kills also grant Speed I for three seconds", "You carried what others hurried past. The forgotten will walk beside you, not behind you.")),
            new God("nythea", "Beneath the Dream", "twilight_veil", "Some dreams ask us to listen; others ask us to wake and resist. Bring your offering, then enter the dream by your own road.",
                path("Stillness", "pray", "night_safe", 1200, "", "Pray for 60 continuous seconds at night without taking damage", "Twilight Veil grants Night Vision while its nighttime condition holds", "You heard the dream without tearing it apart. Let the dark reveal what daylight overlooks."),
                path("Defiance", "kill", "phantom", 4, "", "Defeat four phantoms", "Twilight Veil nighttime speed bonus increases from 10% to 15%", "You refused the nightmare's claim. Your dreams need never be cages.")),
            new God("kairoth", "The Moment Chosen", "second_chance", "One moment asks patience; another will never return. Offer a token of your time, then decide how you will spend it.",
                path("Patience", "pray", "still", 1200, "", "Pray motionless for 60 continuous seconds", "Second Chance restores 60% maximum health instead of 50%", "You let the moment unfold without seizing it. There is strength in knowing when to wait."),
                path("Decisiveness", "timed_kill", "any", 8, "", "Defeat eight hostile mobs within 120 seconds of the first qualifying kill", "Second Chance also grants Absorption I for five seconds", "When the moment came, you answered. May the next chance find you just as ready.")),
            new God("cryonis", "Winter's Testament", "icy_pulse", "Winter shelters the seed and tests the unprepared. Bring your offering, then choose what your stillness will preserve.",
                path("Preservation", "donation", "", 32, "snowball", "Contribute 32 snowballs", "Icy Pulse radius increases from three to four blocks", "You gathered winter without shattering it. Let your calm reach those who cannot find their own."),
                path("Severity", "kill", "stray", 6, "", "Defeat six strays", "Icy Pulse slowing lasts five seconds instead of three", "You endured the cruelest cold and answered it. Be exacting, but never empty of purpose.")),
            new God("nerai", "The Water's Promise", "aquatic_grace", "The sea welcomes life and carries its guardians. Bring a gift to the tide, then decide which promise you will keep.",
                path("Harmony", "submerged", "", 2400, "", "Remain submerged for 120 continuous seconds without drowning damage", "Aquatic Grace heals one HP every ten seconds while submerged", "You listened until the water no longer felt foreign. The tide now remembers your breath."),
                path("Protection", "kill", "drowned", 6, "", "Defeat six drowned", "Aquatic Grace grants an additional 20% swim speed in water", "You made room for the living beneath the waves. Let the currents carry you to those in need.")),
            new God("zephra", "An Unbound Horizon", "gale_step", "Freedom can follow a thousand footsteps or a single impossible height. Bring your offering, then show me your horizon.",
                path("Journey", "sprint", "", 1000, "", "Sprint 1,000 blocks", "Gale Step grants an additional 5% movement speed", "Every step made the world a little wider. Never mistake a familiar road for the only road."),
                path("Heights", "height", "", 1, "", "Reach Y=160 and return to this shrine", "Gale Step reduces fall damage by 25%", "You touched the high wind and returned to tell of it. Freedom includes knowing how to come home.")),
            new God("terra", "Roots Remember", "thornskin", "The seed needs a beginning; the field needs a keeper. Offer to the soil, then choose the care you will give it.",
                path("Renewal", "plant", "", 12, "", "Plant 12 saplings at distinct positions", "Thornskin retaliation also applies Slowness I for two seconds", "Twelve beginnings have taken root. The earth will remember your hands long after their work is hidden."),
                path("Guardianship", "harvest", "", 16, "", "Harvest 16 mature crops", "Thornskin retaliation deals one additional HP of damage", "You waited for ripeness and gathered without waste. Life is defended by those who understand its seasons.")),
            new God("lumen", "A Light Kept", "beacons_grace", "A light may offer sanctuary or drive corruption away. Bring your gift, then choose the promise it will keep.",
                path("Sanctuary", "donation", "", 8, "lantern", "Contribute eight lanterns", "Beacon's Grace qualifying block-light threshold decreases from 12 to nine", "You set light where fear once settled. A refuge need not be grand to be sacred."),
                path("Purification", "kill", "witch", 4, "", "Defeat four witches", "Beacon's Grace grants Resistance I while its existing light condition holds", "You faced corruption without surrendering your purpose. Let purity mean care, not contempt.")),
            new God("mechanos", "The Worth of a Machine", "clockwork_punch", "A machine may excel through precision or through force honestly applied. Bring your offering, then demonstrate the worth of your design.",
                path("Precision", "donation", "", 4, "piston", "Contribute four pistons", "Clockwork Punch knockback chance increases from 30% to 40%", "Every part has a place, and every motion a reason. Your precision is a form of respect."),
                path("Force", "donation", "", 16, "redstone", "Contribute 16 redstone dust", "Clockwork Punch knockback strength increases by 25%", "Power without waste: a worthy answer. May every force you apply move something that needed moving.")),
            new God("ignar", "What the Fire Leaves", "blazing_hands", "Fire can consume a threat or prepare a useful ember. Bring your offering, then show me what should remain after the flame.",
                path("Fury", "kill", "burning", 8, "", "Defeat eight burning hostile mobs", "Blazing Hands ignition chance increases from 15% to 25%", "Your fury found its enemy. Keep it a chosen flame, never a master you cannot dismiss."),
                path("Purpose", "donation", "", 16, "charcoal", "Contribute 16 charcoal", "Blazing Hands ignition lasts five seconds instead of three", "You saved the heat for work yet unfinished. Fire with purpose outlasts fire without restraint.")),
            new God("aethon", "Reading the Silence", "celestial_ward", "The stars invite both a journey outward and a patient gaze inward. Bring your offering, then tell me how you will read their silence.",
                path("Discovery", "donation", "", 4, "ender_pearl", "Contribute four ender pearls", "Celestial Ward successful-block cooldown decreases from 30 to 24 seconds", "You crossed the distance between a question and its answer. There will always be another star to seek."),
                path("Contemplation", "pray", "sky_night", 1200, "", "Pray under visible night sky above Y=100 for 60 continuous seconds", "A successful Celestial Ward block also heals one HP", "You allowed the silence to become a conversation. The cosmos has room for the small, steady light you carry."))
        );
    }
}
