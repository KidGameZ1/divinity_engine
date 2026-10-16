package net.nightshade.divinity_engine.util.divinity.narrative;

import net.nightshade.divinity_engine.api.narrative.GodPresenceDefinition;
import net.minecraft.resources.ResourceLocation;

final class BuiltinPresenceDefinitions {
    static GodPresenceDefinition create(BuiltinNarrativeCatalog.God god) {
        if (!god.id().equals("solarius")) return themed(god);
        return GodPresenceDefinition.builder(DivineNarrativeRegistry.id(god.id())).color(0xFFD36A)
            .lines("greeting", "The dawn finds you again.", "Stand in the light; you need hide nothing here.", "What will you illuminate today?")
            .lines("prayer", "Let your silence be honest.", "I hear the question beneath your words.", "Carry this warmth to someone who needs it.")
            .lines("offering", "A gift freely given shines brightest.", "Your offering feeds more than flame.", "I accept. Let generosity follow you beyond this shrine.")
            .lines("return", "Even after absence, a new dawn is possible. I remember you.")
            .lines("failure", "One eclipse does not end the sun. Rise and try again.")
            .lines("victory", "Your courage has brought another dawn.")
            .lines("recognition", "This shrine remembers the light you chose to become.")
            .lines("Champion", "My champion, let power never outgrow compassion.")
            .lines("Devoted", "Your devotion is a steady flame.")
            .lines("Favorable", "Your first light has not gone unnoticed.")
            .lines("Neutral", "A dawn is a beginning, not a promise.")
            .lines("Displeased", "Do not mistake brightness for righteousness.")
            .lines("Enemy", "Even here, you must answer for the shadows you cast.").build();
    }

    private record Voice(int color, String texture, String sound, String motif, String greetings, String prayers, String offerings, String failure, String returning) {}
    private static GodPresenceDefinition themed(BuiltinNarrativeCatalog.God god) {
        Voice v = switch (god.id()) {
            case "nashara" -> new Voice(0x94CF62, "spell_0", "block.brewing_stand.brew", "the patient serpent",
                "A familiar footstep in my shadows.|Come closer. Secrets are wasted at a distance.|You need not tell me everything. Only what matters.",
                "Even silence has a scent.|Breathe slowly; the truth need not be hurried.|I hear what you carefully leave unsaid.",
                "A gift with no hidden barb? How interesting.|Your offering tells its own small secret.|Accepted. Let this remain between us.",
                "A missed strike teaches the serpent where to wait. Your failures are not forgotten, nor are they final.", "You have returned to the shadow you once left. I noticed both departures.");
            case "grond" -> new Voice(0xBFA17D, "generic_0", "block.anvil.use", "the enduring mountain",
                "Stone remembers the weight of your steps.|The forge has room for your hands.|Rest if you must. The mountain is not going anywhere.",
                "Let the noise settle like dust.|Patience is work that cannot be seen.|Your resolve sounds steadier today.",
                "Honest material. Honest work.|This gift will not be wasted.|The forge accepts what your hands have brought.",
                "Cracks show where the next reinforcement belongs. We build again.", "The stone did not forget you while you were away. Have your foundations changed?");
            case "varun" -> new Voice(0xC3A170, "critical_hit", "entity.wolf.ambient", "the living pack",
                "I know your scent. Walk beside me.|What tracks brought you here today?|The pack listens when you arrive.",
                "Listen beyond your own heartbeat.|No hunter survives by strength alone.|May your hunger never forget those who share it.",
                "The pack will share this gift.|You remembered the mouths beyond your own.|A good offering leaves no part wasted.",
                "Even the best hunter loses a trail. Breathe, listen, and begin again.", "You wandered far from the pack. There is still a place at its edge for your return.");
            case "dravak" -> new Voice(0xD85555, "critical_hit", "item.shield.block", "the unbroken line",
                "Stand straight. Your story is not finished.|Have your battles taught you their cost?|I recognize the one who returns to the line.",
                "Courage begins before the first blow.|Remember who stands behind your shield.|Let this quiet sharpen your purpose, not your anger.",
                "A warrior who can give knows more than conquest.|I accept your tribute. Now honor its cost.|This is a promise made without a weapon.",
                "Defeat is an event, not your name. Learn why the line broke.", "You left the line. You returned to it. Tell me what changed between those moments.");
            case "elaris" -> new Voice(0x80B8E5, "sga_a", "block.enchantment_table.use", "the unfinished question",
                "What question followed you here?|A familiar reader, and a new page.|I hope you have brought uncertainty; it is useful.",
                "An honest question is already progress.|Leave room in your answer for what you do not know.|Silence, too, belongs in the library.",
                "Knowledge gains value when shared.|A thoughtful gift. Let us learn from it.|I accept the work behind this offering.",
                "Record what happened without shame. A mistake understood is no longer only a mistake.", "The book remained open at your place. Have you found a different reading?");
            case "voltira" -> new Voice(0x87DDF0, "spark_0", "block.amethyst_block.chime", "the answering storm",
                "There you are. I felt the air change.|Another horizon is waiting for us.|Tell me you did not come here to stand still forever.",
                "Even thunder begins in quiet air.|Hold the charge. Choose the moment.|Let your next step have a direction.",
                "A spark freely given can start a storm.|Accepted. I like a gift with possibility.|The air hums around your offering.",
                "The first strike missed. That is why storms have more than one.", "You vanished beyond my horizon. Now the pressure is changing again.");
            case "vokar" -> new Voice(0x80BCB3, "soul_0", "block.soul_sand.step", "the last farewell",
                "You are still among the living. Cherish that.|There is no need to whisper for my sake.|Who do you carry in your memory today?",
                "A name remembered is not wholly lost.|Let grief breathe; it need not answer quickly.|I will sit with what you cannot yet say.",
                "I receive this in remembrance.|What you give has a story, too.|Your offering will not pass unnoticed into silence.",
                "Not every ending is yours to control. This trial still allows another beginning.", "Absence has a voice. I heard yours, and now I hear your return.");
            case "nythea" -> new Voice(0xB6A5E0, "spell_0", "block.amethyst_block.resonate", "the remembered dream",
                "Have we met here, or only dreamed it?|Your shadow arrived a heartbeat before you.|Come. The dream has not decided what to become.",
                "Let the image come without naming it.|A quiet mind can hear the seams of a dream.|You need not wake every sleeping fear.",
                "A gift from the waking world. How curious.|I shall keep the dream inside this offering.|Accepted. It means more than it appears to.",
                "The nightmare ended badly this time. We may dream it differently.", "I dreamed of an empty place at this shrine. Now you have changed the ending.");
            case "kairoth" -> new Voice(0xD8C38B, "glitter_0", "block.note_block.hat", "the chosen moment",
                "You are here precisely now.|We have not had this conversation before. Not quite.|What did you make of the time between visits?",
                "You cannot hold a moment, only attend to it.|Waiting is also a choice.|Let one breath finish before you borrow the next.",
                "I accept the time this gift represents.|A small part of your life, offered freely.|May what you give make the next moment kinder.",
                "That attempt is past. It may teach the next without owning it.", "Much happened between leaving and returning. None of it makes this moment less real.");
            case "cryonis" -> new Voice(0xB7E7FF, "glitter_0", "block.powder_snow.step", "the patient winter",
                "Let the cold clear your thoughts.|I heard your steps beneath the snow.|There is peace in what has not yet thawed.",
                "Be still enough to notice what survives.|Not all silence is emptiness.|Rest your restless thoughts upon the snow.",
                "Preserved with care. I accept.|A little of winter, held without breaking.|Your offering settles softly here.",
                "The frost did not defeat the seed forever. Gather yourself for another season.", "Winter held your place in silence. You have returned before all the snow is gone.");
            case "nerai" -> new Voice(0x66C6E6, "bubble", "block.water.ambient", "the returning tide",
                "Every tide brings something familiar.|What has the current carried to you?|Come ashore for a moment. Tell me of your crossing.",
                "Breathe with the rhythm you can hear.|The sea does not ask every wave to be mighty.|Let what troubles you loosen its grip.",
                "The current receives your kindness.|A gift to the sea returns in ways unseen.|Thank you for remembering what lives below.",
                "One wave broke before the shore. The sea has another ready.", "Even a wandering river can find the sea again. I remember your waters.");
            case "zephra" -> new Voice(0xB4ECD5, "generic_0", "entity.phantom.flap", "the open horizon",
                "You brought the road in with you.|Which horizon caught your eye today?|Welcome back. Do not stay only because a wall tells you to.",
                "Give your thoughts a little room to move.|No cage is improved by forgetting the sky.|Rest is not the opposite of freedom.",
                "A gift, not a tether. I accept.|You carried this all the way here for me?|Let your generosity travel farther than these stones.",
                "A headwind is not the end of the journey. Find another angle.", "You were free to leave, and free to return. Tell me what the distance taught you.");
            case "terra" -> new Voice(0x98CF74, "glitter_0", "block.grass.step", "the remembering root",
                "The roots knew your footfall.|Something has grown since your last visit.|Come gently. Small lives are listening.",
                "Growth does not become faster because we demand it.|Let your care reach below what you can see.|The soil receives even a quiet promise.",
                "A gift that can nourish what follows.|The earth wastes little; I will honor your offering.|Your care has reached this patch of ground.",
                "Not every planting takes. We learn the soil and plant again.", "Roots remember both drought and rain. I remember your absence, and welcome your care again.");
            case "lumen" -> new Voice(0xFFF0A8, "glitter_0", "block.beacon.ambient", "the sheltered light",
                "There is room for you within this light.|What darkness needs kindness today?|You need not be flawless to stand here.",
                "Let clarity make you kinder, not colder.|A small light honestly kept is enough.|You may lay down what you no longer wish to carry.",
                "Your gift will help keep a light burning.|I accept the care within this offering.|May this kindness reach beyond its giver.",
                "A dimmed light can be tended. Do not mistake a failed trial for a failed soul.", "The lamp was kept for your return. We need not pretend you were never away.");
            case "mechanos" -> new Voice(0xC4B9A1, "spark_0", "block.piston.extend", "the purposeful machine",
                "A familiar component, carrying new experience.|What have you made work better today?|Welcome. The design is still open to improvement.",
                "Listen for the unnecessary friction.|Precision begins with noticing.|A pause for inspection is productive work.",
                "Useful material. A considerate offering.|Accepted. Nothing here need be wasted.|I appreciate the effort behind these parts.",
                "A failed test is useful data. Change the design, not the value you place on yourself.", "The system recorded your absence. Your return is a new input, not an error.");
            case "ignar" -> new Voice(0xFF9850, "flame", "block.fire.ambient", "the purposeful flame",
                "There is still a spark in you.|Come closer; I dislike cold introductions.|What do you intend to leave unburned today?",
                "Let your anger become heat you can use.|An ember needs breath, not a shouted command.|Know why you kindle the next flame.",
                "Good fuel, freely given.|I accept. Let us make something of this heat.|Your offering has an honest warmth.",
                "The flame guttered. Shield the ember and try again.", "You left the fire untended. Yet here you are, carrying a spark back to it.");
            default -> new Voice(0xA9C2EF, "glitter_0", "block.amethyst_block.chime", "the listening stars",
                "The sky has changed since we last spoke.|A small life beneath so many lights. Still worth hearing.|What did the silence tell you on your way here?",
                "You need not fill the whole sky with an answer.|Attend to one distant light at a time.|A question can be a kind of constellation.",
                "A fragment of distance, brought close.|I accept the journey inside this gift.|Even a small offering changes the pattern.",
                "One star is hidden, not extinguished. Look again when you are ready.", "The constellations moved while you were away. There is still a place for your light among them.");
        };
        return GodPresenceDefinition.builder(DivineNarrativeRegistry.id(god.id())).color(v.color)
            .texture(new ResourceLocation("minecraft", "textures/particle/" + v.texture + ".png"))
            .sound(new ResourceLocation("minecraft", v.sound))
            .lines("greeting", v.greetings.split("\\|"))
            .lines("prayer", v.prayers.split("\\|"))
            .lines("offering", v.offerings.split("\\|"))
            .lines("return", v.returning).lines("failure", v.failure)
            .lines("victory", "Your trial is won. You have added your own answer to " + v.motif + ".")
            .lines("recognition", "This shrine remembers your chosen ending. Your place beside " + v.motif + " is not forgotten.")
            .lines("Champion", "My champion, carry the trust of " + v.motif + " with care.")
            .lines("Devoted", "Your devotion has become part of " + v.motif + ".")
            .lines("Favorable", "Your kindness has reached " + v.motif + ". There is a story I would share with you.")
            .lines("Neutral", "We have more to learn of one another. Listen to " + v.motif + ".")
            .lines("Displeased", "Your choices trouble " + v.motif + ". Consider what they leave behind.")
            .lines("Enemy", "Trust with " + v.motif + " is broken. It is your actions, not your words, that may mend it.").build();
    }
}
