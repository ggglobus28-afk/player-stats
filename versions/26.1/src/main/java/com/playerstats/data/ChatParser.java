package com.playerstats.data;

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
 * Reads kill feed and totem messages out of the chat. Everything is matched
 * locally with a handful of permissive regular expressions, so it works on
 * vanilla servers and on most PvP servers without any configuration.
 */
public final class ChatParser {
	/** A recognised chat message. {@code attacker} may be null for solo totems. */
	public record Match(boolean totem, String attacker, String victim) {
	}

	private record Rule(Pattern pattern, int victimGroup, int killerGroup) {
	}

	private record TotemRule(Pattern pattern, int playerGroup, int otherGroup) {
	}

	private static final String NAME = "([A-Za-z0-9_]{3,16})";

	private static final List<Rule> KILLS = new ArrayList<>();
	private static final List<TotemRule> TOTEM_PAIRS = new ArrayList<>();
	private static final List<TotemRule> TOTEM_SOLO = new ArrayList<>();

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

		// Totem messages that name both players.
		totemPair("\\b" + NAME + "'s\\s+totem\\s+(?:was\\s+)?(?:popped|broken|destroyed)\\s+by\\s+" + NAME, 1, 2);
		totemPair("\\b" + NAME + "\\s+popped\\s+" + NAME + "'s\\s+totem", 2, 1);
		totemPair("\\b" + NAME + "\\s+снял\\s+тотем\\s+с\\s+" + NAME, 2, 1);

		// Totem messages with a single player.
		totemSolo("\\b" + NAME + "\\s+used\\s+a\\s+totem\\s+of\\s+undying", 1);
		totemSolo("\\b" + NAME + "\\s+popped\\s+a\\s+totem", 1);
		totemSolo("\\b" + NAME + "'s\\s+totem\\s+(?:of\\s+undying\\s+)?(?:activated|popped|saved)", 1);
		totemSolo("\\b" + NAME + "\\s+activated\\s+(?:a\\s+|their\\s+)?totem", 1);
		totemSolo("\\b" + NAME + "\\s+использовал\\s+тотем", 1);
		totemSolo("\\b" + NAME + "\\s+активировал\\s+тотем", 1);
		totemSolo("\\b" + NAME + "\\s+спас(?:ся|лась)?\\s+(?:с\\s+помощью\\s+)?тотем", 1);
	}

	private ChatParser() {
	}

	private static void kill(String regex, int victimGroup, int killerGroup) {
		KILLS.add(new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), victimGroup, killerGroup));
	}

	private static void totemPair(String regex, int playerGroup, int otherGroup) {
		TOTEM_PAIRS.add(new TotemRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), playerGroup, otherGroup));
	}

	private static void totemSolo(String regex, int playerGroup) {
		TOTEM_SOLO.add(new TotemRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), playerGroup, -1));
	}

	/**
	 * Pure text matching. Returns null when the line is not a kill or totem
	 * message. Never touches the game, which also makes it easy to test.
	 */
	public static Match parse(String raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.trim();
		if (text.isEmpty() || text.length() > 512) {
			return null;
		}
		Match totem = parseTotem(text);
		return totem != null ? totem : parseKill(text);
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
		Settings settings = StatsStore.settings();
		if (match.totem()) {
			if (settings.chatTotems) {
				StatsStore.get().recordChatTotem(match.attacker(), match.victim());
			}
		} else if (settings.chatKills) {
			StatsStore.get().recordChatKill(match.attacker(), match.victim());
		}
	}

	private static Match parseTotem(String text) {
		for (TotemRule rule : TOTEM_PAIRS) {
			Matcher matcher = rule.pattern().matcher(text);
			if (!matcher.find()) {
				continue;
			}
			String player = matcher.group(rule.playerGroup());
			String other = matcher.group(rule.otherGroup());
			if (!valid(player) || !valid(other) || player.equalsIgnoreCase(other)) {
				continue;
			}
			return new Match(true, other, player);
		}
		for (TotemRule rule : TOTEM_SOLO) {
			Matcher matcher = rule.pattern().matcher(text);
			if (!matcher.find()) {
				continue;
			}
			String player = matcher.group(rule.playerGroup());
			if (!valid(player)) {
				continue;
			}
			return new Match(true, null, player);
		}
		return null;
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
			return new Match(false, killer, victim);
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
