package com.bodycount.data;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads kill messages out of the chat. Everything is matched locally with a
 * handful of permissive regular expressions, so it works on vanilla servers and
 * on most PvP servers without any configuration.
 */
public final class ChatParser {
	/** A recognised chat message. */
	public record Match(String killer, String victim) {
	}

	private record Rule(Pattern pattern, int victimGroup, int killerGroup) {
	}

	private static final String NAME = "([A-Za-z0-9_]{3,16})";

	private static final List<Rule> KILLS = new ArrayList<>();

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
		"was", "has", "had", "been", "the", "and", "for", "you", "not", "but", "all", "any",
		"can", "her", "his", "how", "its", "our", "out", "she", "who", "why", "use", "used",
		"too", "get", "got", "are", "were", "from", "with", "this", "that", "they", "them",
		"then", "than", "into", "over", "under", "again", "killed", "slain", "died", "death",
		"dead", "player", "someone", "using", "magic", "void", "world", "game", "server",
		"chat", "team", "fire", "lava", "fall", "fell", "drown", "drowned", "starved",
		"burned", "withered", "poison", "explosion", "arrow", "trident", "sword", "axe",
		"bow", "crossbow", "totem", "undying", "mob", "pvp", "kill", "kills"
	));

	private static final Set<String> MOBS = new HashSet<>(Arrays.asList(
		"zombie", "skeleton", "creeper", "spider", "enderman", "witch", "blaze", "ghast",
		"slime", "silverfish", "endermite", "guardian", "shulker", "phantom", "drowned",
		"husk", "stray", "pillager", "vindicator", "evoker", "ravager", "vex", "illusioner",
		"wither", "piglin", "hoglin", "zoglin", "strider", "golem", "wolf", "fox", "bee",
		"panda", "llama", "horse", "donkey", "mule", "bat", "squid", "dolphin", "turtle",
		"cod", "salmon", "pufferfish", "axolotl", "tadpole", "frog", "allay", "warden",
		"breeze", "bogged", "creaking", "armadillo", "goat", "camel", "sniffer", "villager",
		"iron", "snow", "polar", "bear", "trader", "elder", "cave", "magma", "withering",
		"zombified", "skeletonhorse", "happy", "copper", "nautilus", "glow"
	));

	static {
		// Vanilla death messages: victim first, killer second.
		kill("\\b" + NAME + "\\s+was\\s+(?:slain|killed|murdered|eliminated|destroyed|sniped|obliterated|struck\\s+down)\\s+by\\s+" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+was\\s+(?:shot|fireballed|impaled|pummeled|blown\\s+up|doomed\\s+to\\s+fall|struck)\\s+by\\s+" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+drowned\\s+whilst\\s+trying\\s+to\\s+escape\\s+" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+has\\s+been\\s+(?:slain|killed)\\s+by\\s+" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+was\\s+(?:killed|slain|shot)\\s+by\\s+" + NAME + "\\s+using", 1, 2);

		// Server plugins: "A killed B", "[Rank] A killed B".
		kill("\\b" + NAME + "\\s+(?:killed|slew|eliminated|destroyed)\\s+" + NAME, 2, 1);

		// Russian vanilla death messages.
		kill("\\b" + NAME + "\\s+был\\s+(?:убит|застрелен|заколот|подорван|забит|сраж[её]н)\\s+(?:игроком\\s+)?" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+убит\\s+(?:игроком\\s+)?" + NAME, 1, 2);
		kill("\\b" + NAME + "\\s+проиграл\\s+в\\s+бою\\s+с\\s+" + NAME, 1, 2);
	}

	private ChatParser() {
	}

	private static void kill(String regex, int victimGroup, int killerGroup) {
		KILLS.add(new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), victimGroup, killerGroup));
	}

	/**
	 * Pure text matching. Returns null when the line is not a kill message.
	 * Never touches the game, which also makes it easy to test.
	 */
	public static Match parse(String raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.trim();
		if (text.isEmpty() || text.length() > 512) {
			return null;
		}
		return parseKill(text);
	}

	/** Feeds one chat line into the parser. */
	public static void handle(String raw) {
		Match match = parse(raw);
		if (match == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}
		if (StatsStore.settings().chatKills) {
			StatsStore.get().recordChatKill(match.killer(), match.victim());
		}
	}

	private static Match parseKill(String text) {
		for (Rule rule : KILLS) {
			Matcher matcher = rule.pattern().matcher(text);
			if (!matcher.find()) {
				continue;
			}
			String victim = matcher.group(rule.victimGroup());
			String killer = matcher.group(rule.killerGroup());
			if (!valid(victim) || !valid(killer) || victim.equalsIgnoreCase(killer)) {
				continue;
			}
			return new Match(killer, victim);
		}
		return null;
	}

	private static boolean valid(String name) {
		if (name == null || name.length() < 3 || name.length() > 16) {
			return false;
		}
		String lower = name.toLowerCase(Locale.ROOT);
		return !STOP_WORDS.contains(lower) && !MOBS.contains(lower);
	}
}
