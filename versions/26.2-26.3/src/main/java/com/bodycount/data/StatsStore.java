package com.bodycount.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores the per-player counters for every server profile. The statistics
 * (totems, kills, chat pairs and server profiles) live in a plain text file
 * at config/totems.txt so they can be read and edited by hand; the toggles
 * stay in config/bodycount.json.
 */
public final class StatsStore {
	private static final StatsStore INSTANCE = new StatsStore();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long SAVE_INTERVAL_MS = 2000L;

	public enum Kind {
		TOTEM,
		KILL
	}

	public static final class Opponent {
		public String name = "";
		public int totemsDealt;
		public int totemsTaken;
		public int kills;
		public int deaths;
		public long lastSeen;

		public int total() {
			return this.totemsDealt + this.totemsTaken + this.kills + this.deaths;
		}

		public int totems() {
			return this.totemsDealt + this.totemsTaken;
		}

		public int fights() {
			return this.kills + this.deaths;
		}
	}

	/**
	 * A pair of players seen in chat. {@code b} may be empty for messages that
	 * only name one player (for example "Steve used a Totem of Undying").
	 */
	public static final class ChatPair {
		public String a = "";
		public String b = "";
		public int aKillsB;
		public int bKillsA;
		public int aTotemsB;
		public int bTotemsA;
		public long lastSeen;

		public int total() {
			return this.aKillsB + this.bKillsA + this.aTotemsB + this.bTotemsA;
		}

		public int killsA() {
			return this.aKillsB + this.aTotemsB;
		}

		public int killsB() {
			return this.bKillsA + this.bTotemsA;
		}

		public boolean single() {
			return this.a.isEmpty() || this.b.isEmpty();
		}

		public String soloName() {
			return this.a.isEmpty() ? this.b : this.a;
		}
	}

	public static final class ServerEntry {
		public String id = UUID.randomUUID().toString();
		public String name = "";
		public String address = "";
		public boolean singleplayer;
		public Map<String, Opponent> players = new LinkedHashMap<>();
		public Map<String, ChatPair> chatKills = new LinkedHashMap<>();
		public Map<String, ChatPair> chatTotems = new LinkedHashMap<>();

		public Opponent opponent(UUID uuid) {
			return this.players.computeIfAbsent(uuid.toString(), key -> new Opponent());
		}

		public int totemsDealt() {
			int total = 0;
			for (Opponent opponent : this.players.values()) {
				total += opponent.totemsDealt;
			}
			return total;
		}

		public int totemsTaken() {
			int total = 0;
			for (Opponent opponent : this.players.values()) {
				total += opponent.totemsTaken;
			}
			return total;
		}

		public int kills() {
			int total = 0;
			for (Opponent opponent : this.players.values()) {
				total += opponent.kills;
			}
			return total;
		}

		public int deaths() {
			int total = 0;
			for (Opponent opponent : this.players.values()) {
				total += opponent.deaths;
			}
			return total;
		}

		public int chatKillsTotal() {
			int total = 0;
			for (ChatPair pair : this.chatKills.values()) {
				total += pair.aKillsB + pair.bKillsA;
			}
			return total;
		}

		public void mergeFrom(ServerEntry other) {
			if (other == null) {
				return;
			}
			for (Map.Entry<String, Opponent> entry : other.players.entrySet()) {
				Opponent mine = this.players.computeIfAbsent(entry.getKey(), key -> new Opponent());
				Opponent theirs = entry.getValue();
				if (mine.name == null || mine.name.isBlank()) {
					mine.name = theirs.name;
				}
				mine.totemsDealt += theirs.totemsDealt;
				mine.totemsTaken += theirs.totemsTaken;
				mine.kills += theirs.kills;
				mine.deaths += theirs.deaths;
				mine.lastSeen = Math.max(mine.lastSeen, theirs.lastSeen);
			}
			mergePairs(this.chatKills, other.chatKills, true);
			mergePairs(this.chatTotems, other.chatTotems, false);
		}

		private static void mergePairs(Map<String, ChatPair> target, Map<String, ChatPair> source, boolean kills) {
			if (source == null) {
				return;
			}
			for (Map.Entry<String, ChatPair> entry : source.entrySet()) {
				ChatPair theirs = entry.getValue();
				if (theirs == null) {
					continue;
				}
				ChatPair mine = target.computeIfAbsent(entry.getKey(), key -> {
					ChatPair pair = new ChatPair();
					pair.a = theirs.a;
					pair.b = theirs.b;
					return pair;
				});
				if (kills) {
					mine.aKillsB += theirs.aKillsB;
					mine.bKillsA += theirs.bKillsA;
				} else {
					mine.aTotemsB += theirs.aTotemsB;
					mine.bTotemsA += theirs.bTotemsA;
				}
				mine.lastSeen = Math.max(mine.lastSeen, theirs.lastSeen);
			}
		}

		public void clearStats() {
			this.players.clear();
			this.chatKills.clear();
			this.chatTotems.clear();
		}
	}

	public static final class Data {
		public String active;
		public boolean pinned;
		public Settings settings = new Settings();
		public List<ServerEntry> servers = new ArrayList<>();
	}

	private final Path dataFile = FabricLoader.getInstance().getConfigDir().resolve("totems.txt");
	private final Path settingsFile = FabricLoader.getInstance().getConfigDir().resolve("bodycount.json");
	private Data data = new Data();
	private boolean dirty;
	private long lastSave;

	private StatsStore() {
	}

	public static StatsStore get() {
		return INSTANCE;
	}

	public static Settings settings() {
		return INSTANCE.data.settings;
	}

	public void load() {
		this.loadSettings();
		this.loadData();
		this.normalize();
	}

	private void loadSettings() {
		try {
			Path source = this.settingsFile;
			if (!Files.exists(source)) {
				// The mod used to be called Player Stats; keep the old settings once.
				Path legacy = FabricLoader.getInstance().getConfigDir().resolve("playerstats.json");
				if (!Files.exists(legacy)) {
					return;
				}
				source = legacy;
			}
			String json = Files.readString(source, StandardCharsets.UTF_8);
			JsonElement root = JsonParser.parseString(json);
			if (!root.isJsonObject()) {
				return;
			}
			JsonObject object = root.getAsJsonObject();
			JsonElement settingsElement = object.has("settings") ? object.get("settings") : object;
			Settings loaded = GSON.fromJson(settingsElement, Settings.class);
			if (loaded != null) {
				// Builds before the vanilla theme used 0 for Dark. Shift those
				// values once so the saved look is preserved.
				if (settingsElement.isJsonObject()) {
					JsonObject settingsObject = settingsElement.getAsJsonObject();
					if (settingsObject.has("theme") && !settingsObject.has("themeVersion")) {
						int shifted = loaded.theme + 1;
						loaded.theme = Math.max(0, Math.min(Settings.THEME_KEYS.length - 1, shifted));
					}
				}
				this.data.settings = loaded;
			}
			// Older versions kept the statistics inside this JSON file: import them once.
			if (object.has("servers") && !Files.exists(this.dataFile)) {
				Data legacy = GSON.fromJson(object, Data.class);
				if (legacy != null && legacy.servers != null && !legacy.servers.isEmpty()) {
					this.data.servers = legacy.servers;
					this.data.active = legacy.active;
					this.dirty = true;
				}
			}
		} catch (Exception ignored) {
			// A broken file must never prevent the game from starting.
		}
	}

	private void loadData() {
		if (!Files.exists(this.dataFile)) {
			return;
		}
		try {
			List<ServerEntry> servers = new ArrayList<>();
			ServerEntry current = null;
			String active = null;
			for (String raw : Files.readAllLines(this.dataFile, StandardCharsets.UTF_8)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
					continue;
				}
				if (line.equalsIgnoreCase("[server]")) {
					current = new ServerEntry();
					servers.add(current);
					continue;
				}
				int equals = line.indexOf('=');
				if (equals < 0) {
					continue;
				}
				String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
				String value = line.substring(equals + 1).trim();
				if (key.equals("active")) {
					active = value;
					continue;
				}
				if (current == null) {
					continue;
				}
				switch (key) {
					case "id" -> current.id = unescape(value);
					case "name" -> current.name = unescape(value);
					case "address" -> current.address = unescape(value);
					case "singleplayer" -> current.singleplayer = Boolean.parseBoolean(value);
					case "player" -> readPlayer(current, value);
					case "chatkill" -> readChatKill(current, value);
					case "chattotem" -> readChatTotem(current, value);
					default -> {
						// Unknown keys are ignored so hand edits cannot break the file.
					}
				}
			}
			if (!servers.isEmpty()) {
				this.data.servers = servers;
			}
			if (active != null && !active.isBlank()) {
				this.data.active = active;
			}
		} catch (Exception ignored) {
			// Keep the in-memory data when the text file cannot be read.
		}
	}

	private static void readPlayer(ServerEntry entry, String value) {
		List<String> parts = split(value);
		if (parts.size() < 2 || parts.get(0).isBlank()) {
			return;
		}
		Opponent opponent = new Opponent();
		opponent.name = parts.get(1);
		opponent.totemsDealt = parseInt(parts, 2);
		opponent.totemsTaken = parseInt(parts, 3);
		opponent.kills = parseInt(parts, 4);
		opponent.deaths = parseInt(parts, 5);
		opponent.lastSeen = parseLong(parts, 6);
		entry.players.put(parts.get(0), opponent);
	}

	private static void readChatKill(ServerEntry entry, String value) {
		List<String> parts = split(value);
		if (parts.size() < 2 || parts.get(0).isBlank() || parts.get(1).isBlank()) {
			return;
		}
		ChatPair pair = pair(entry.chatKills, parts.get(0), parts.get(1));
		pair.aKillsB += parseInt(parts, 2);
		pair.bKillsA += parseInt(parts, 3);
		pair.lastSeen = Math.max(pair.lastSeen, parseLong(parts, 4));
	}

	private static void readChatTotem(ServerEntry entry, String value) {
		List<String> parts = split(value);
		if (parts.isEmpty()) {
			return;
		}
		String first = parts.get(0);
		String second = parts.size() > 1 ? parts.get(1) : "";
		if (first.isBlank() && second.isBlank()) {
			return;
		}
		ChatPair pair = pair(entry.chatTotems, first, second);
		pair.aTotemsB += parseInt(parts, 2);
		pair.bTotemsA += parseInt(parts, 3);
		pair.lastSeen = Math.max(pair.lastSeen, parseLong(parts, 4));
	}

	private void normalize() {
		if (this.data == null) {
			this.data = new Data();
		}
		if (this.data.settings == null) {
			this.data.settings = new Settings();
		}
		if (this.data.servers == null) {
			this.data.servers = new ArrayList<>();
		}
		for (ServerEntry entry : this.data.servers) {
			if (entry.id == null || entry.id.isBlank()) {
				entry.id = UUID.randomUUID().toString();
			}
			if (entry.name == null) {
				entry.name = "";
			}
			if (entry.address == null) {
				entry.address = "";
			}
			if (entry.players == null) {
				entry.players = new LinkedHashMap<>();
			}
			if (entry.chatKills == null) {
				entry.chatKills = new LinkedHashMap<>();
			}
			if (entry.chatTotems == null) {
				entry.chatTotems = new LinkedHashMap<>();
			}
		}
	}

	public Data data() {
		return this.data;
	}

	public List<ServerEntry> servers() {
		return this.data.servers;
	}

	public ServerEntry byId(String id) {
		if (id != null) {
			for (ServerEntry entry : this.data.servers) {
				if (entry.id.equals(id)) {
					return entry;
				}
			}
		}
		return null;
	}

	public ServerEntry active() {
		ServerEntry entry = this.byId(this.data.active);
		if (entry == null) {
			if (this.data.servers.isEmpty()) {
				entry = new ServerEntry();
				entry.name = "Local";
				this.data.servers.add(entry);
			} else {
				entry = this.data.servers.get(0);
			}
			this.data.active = entry.id;
		}
		return entry;
	}

	public void setActive(ServerEntry entry) {
		if (entry == null) {
			return;
		}
		this.data.active = entry.id;
		this.data.pinned = true;
		this.data.settings.followServer = false;
		this.markDirty();
	}

	/** Makes the tab follow the server the player is currently on again. */
	public void followCurrent() {
		this.data.pinned = false;
		this.data.settings.followServer = true;
		this.markDirty();
	}

	private static String normalize(String address) {
		return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
	}

	public ServerEntry byAddress(String address) {
		String key = normalize(address);
		for (ServerEntry entry : this.data.servers) {
			if (normalize(entry.address).equals(key)) {
				return entry;
			}
		}
		return null;
	}

	/** Makes sure the server or world the player is currently in has a profile. */
	public ServerEntry ensureCurrent(Minecraft mc) {
		ServerData current = mc.getCurrentServer();
		if (current != null) {
			String ip = current.ip == null ? "" : current.ip;
			ServerEntry entry = this.byAddress(ip);
			if (entry == null) {
				entry = new ServerEntry();
				entry.address = ip;
				entry.name = current.name == null || current.name.isBlank() ? ip : current.name;
				this.data.servers.add(entry);
				this.markDirty();
			}
			return entry;
		}
		if (mc.hasSingleplayerServer()) {
			ServerEntry entry = this.byAddress("singleplayer");
			if (entry == null) {
				entry = new ServerEntry();
				entry.address = "singleplayer";
				entry.singleplayer = true;
				entry.name = "Singleplayer";
				this.data.servers.add(entry);
				this.markDirty();
			}
			return entry;
		}
		return this.active();
	}

	/** The profile the stats tab should display. */
	public ServerEntry displayEntry(Minecraft mc) {
		ServerEntry current = this.ensureCurrent(mc);
		if (this.data.settings.followServer) {
			this.data.pinned = false;
			this.data.active = current.id;
			return current;
		}
		ServerEntry pinned = this.byId(this.data.active);
		if (pinned == null) {
			this.data.pinned = false;
			this.data.active = current.id;
			return current;
		}
		return pinned;
	}

	/** Adds every server from the vanilla multiplayer list. */
	public void importServerList(Minecraft mc) {
		try {
			ServerList list = new ServerList(mc);
			list.load();
			boolean changed = false;
			for (int i = 0; i < list.size(); i++) {
				ServerData server = list.get(i);
				if (server == null || server.ip == null || this.byAddress(server.ip) != null) {
					continue;
				}
				ServerEntry entry = new ServerEntry();
				entry.address = server.ip;
				entry.name = server.name == null ? server.ip : server.name;
				this.data.servers.add(entry);
				changed = true;
			}
			if (changed) {
				this.markDirty();
			}
		} catch (Exception ignored) {
			// The vanilla server list may be unreadable; that is not fatal.
		}
	}

	public void record(UUID attacker, UUID victim, Kind kind) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || attacker == null || victim == null) {
			return;
		}
		Settings settings = this.data.settings;
		if (kind == Kind.TOTEM && !settings.trackTotems) {
			return;
		}
		if (kind == Kind.KILL && !settings.trackKills) {
			return;
		}
		UUID me = mc.player.getUUID();
		if (attacker.equals(victim)) {
			return;
		}
		boolean mine = attacker.equals(me);
		boolean againstMe = victim.equals(me);
		if (!mine && !againstMe) {
			return;
		}

		UUID other = mine ? victim : attacker;
		ServerEntry entry = this.ensureCurrent(mc);
		Opponent opponent = entry.opponent(other);
		String name = this.resolveName(mc, other);
		if (name != null && !name.isBlank()) {
			opponent.name = name;
		}
		opponent.lastSeen = System.currentTimeMillis();

		if (kind == Kind.TOTEM) {
			if (mine) {
				opponent.totemsDealt++;
			} else {
				opponent.totemsTaken++;
			}
		} else {
			if (mine) {
				opponent.kills++;
			} else {
				opponent.deaths++;
			}
		}

		this.markDirty();
	}

	public static String pairKey(String first, String second) {
		String a = first == null ? "" : first;
		String b = second == null ? "" : second;
		return a.compareToIgnoreCase(b) <= 0 ? a + "|" + b : b + "|" + a;
	}

	private static ChatPair pair(Map<String, ChatPair> map, String first, String second) {
		String key = pairKey(first, second);
		return map.computeIfAbsent(key, ignored -> {
			ChatPair created = new ChatPair();
			String a = first == null ? "" : first;
			String b = second == null ? "" : second;
			if (a.compareToIgnoreCase(b) <= 0) {
				created.a = a;
				created.b = b;
			} else {
				created.a = b;
				created.b = a;
			}
			return created;
		});
	}

	/** A kill message from chat: {@code killer} killed {@code victim}. */
	public void recordChatKill(String killer, String victim) {
		if (!this.data.settings.chatKills) {
			return;
		}
		if (killer == null || victim == null || killer.isBlank() || victim.isBlank()) {
			return;
		}
		if (killer.equalsIgnoreCase(victim)) {
			return;
		}
		ServerEntry entry = this.ensureCurrent(Minecraft.getInstance());
		ChatPair pair = pair(entry.chatKills, killer, victim);
		if (pair.a.equalsIgnoreCase(killer)) {
			pair.aKillsB++;
		} else {
			pair.bKillsA++;
		}
		pair.lastSeen = System.currentTimeMillis();
		this.markDirty();
	}

	/** A totem message from chat. {@code attacker} may be null. */
	public void recordChatTotem(String attacker, String victim) {
		if (!this.data.settings.chatTotems) {
			return;
		}
		if (victim == null || victim.isBlank()) {
			return;
		}
		String other = attacker == null || attacker.isBlank() || attacker.equalsIgnoreCase(victim) ? "" : attacker;
		ServerEntry entry = this.ensureCurrent(Minecraft.getInstance());
		ChatPair pair = pair(entry.chatTotems, victim, other);
		// aTotemsB counts "a popped a totem from b", so credit the attacker.
		if (pair.a.equalsIgnoreCase(other)) {
			pair.aTotemsB++;
		} else {
			pair.bTotemsA++;
		}
		pair.lastSeen = System.currentTimeMillis();
		this.markDirty();
	}

	private String resolveName(Minecraft mc, UUID uuid) {
		if (mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
			if (info != null && info.getProfile() != null && info.getProfile().name() != null) {
				return info.getProfile().name();
			}
		}
		if (mc.level != null) {
			Player player = mc.level.getPlayerByUUID(uuid);
			if (player != null) {
				return player.getName().getString();
			}
		}
		return null;
	}

	public static String displayName(ServerEntry entry) {
		boolean custom = entry.name != null && !entry.name.isBlank()
			&& !"Singleplayer".equals(entry.name) && !"Local".equals(entry.name);
		if (custom) {
			return entry.name;
		}
		if (entry.singleplayer) {
			return Component.translatable("bodycount.server.singleplayer").getString();
		}
		if (entry.address != null && !entry.address.isBlank()) {
			return entry.address;
		}
		if (entry.name != null && !entry.name.isBlank()) {
			return entry.name;
		}
		return "Server";
	}

	public void markDirty() {
		this.dirty = true;
		long now = System.currentTimeMillis();
		if (now - this.lastSave >= SAVE_INTERVAL_MS) {
			this.saveNow();
		}
	}

	public void saveIfDirty() {
		if (this.dirty) {
			this.saveNow();
		}
	}

	public void saveNow() {
		try {
			Path parent = this.dataFile.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(this.dataFile, this.toText(), StandardCharsets.UTF_8);
			Files.writeString(this.settingsFile, GSON.toJson(this.data.settings), StandardCharsets.UTF_8);
			this.dirty = false;
			this.lastSave = System.currentTimeMillis();
		} catch (IOException ignored) {
			// Keep playing even when the config directory is read-only.
		}
	}

	/** Renders the whole statistics store as a readable, editable text file. */
	private String toText() {
		StringBuilder out = new StringBuilder(4096);
		out.append("# BodyCount - totems, kills and server profiles\n");
		out.append("# Plain text on purpose: you can read and edit it.\n");
		out.append("# Close Minecraft before editing, otherwise changes are overwritten.\n");
		out.append("# Fields are separated by '|'. A backslash escapes '\\' and '|'.\n");
		out.append("#   player=UUID|NAME|TOTEMS_DEALT|TOTEMS_TAKEN|KILLS|DEATHS|LAST_SEEN\n");
		out.append("#   chatkill=A|B|A_KILLS_B|B_KILLS_A|LAST_SEEN\n");
		out.append("#   chattotem=A|B|A_TOTEMS_B|B_TOTEMS_A|LAST_SEEN\n");
		out.append("# TOTEMS_DEALT = you popped their totem, TOTEMS_TAKEN = they popped yours.\n");
		out.append("# LAST_SEEN is a unix timestamp in milliseconds (0 = unknown).\n");
		out.append('\n');
		out.append("active=").append(escape(this.data.active)).append('\n');
		for (ServerEntry entry : this.data.servers) {
			out.append('\n').append("[server]\n");
			out.append("id=").append(escape(entry.id)).append('\n');
			out.append("name=").append(escape(entry.name)).append('\n');
			out.append("address=").append(escape(entry.address)).append('\n');
			out.append("singleplayer=").append(entry.singleplayer).append('\n');
			for (Map.Entry<String, Opponent> player : entry.players.entrySet()) {
				Opponent opponent = player.getValue();
				if (opponent == null) {
					continue;
				}
				out.append("player=")
					.append(escape(player.getKey())).append('|')
					.append(escape(opponent.name)).append('|')
					.append(opponent.totemsDealt).append('|')
					.append(opponent.totemsTaken).append('|')
					.append(opponent.kills).append('|')
					.append(opponent.deaths).append('|')
					.append(opponent.lastSeen).append('\n');
			}
			for (ChatPair pair : entry.chatKills.values()) {
				if (pair == null) {
					continue;
				}
				out.append("chatkill=")
					.append(escape(pair.a)).append('|')
					.append(escape(pair.b)).append('|')
					.append(pair.aKillsB).append('|')
					.append(pair.bKillsA).append('|')
					.append(pair.lastSeen).append('\n');
			}
			for (ChatPair pair : entry.chatTotems.values()) {
				if (pair == null) {
					continue;
				}
				out.append("chattotem=")
					.append(escape(pair.a)).append('|')
					.append(escape(pair.b)).append('|')
					.append(pair.aTotemsB).append('|')
					.append(pair.bTotemsA).append('|')
					.append(pair.lastSeen).append('\n');
			}
		}
		return out.toString();
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", " ");
	}

	private static String unescape(String value) {
		StringBuilder out = new StringBuilder(value.length());
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (escaped) {
				out.append(c);
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private static List<String> split(String value) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (escaped) {
				current.append(c);
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else if (c == '|') {
				parts.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		parts.add(current.toString());
		return parts;
	}

	private static int parseInt(List<String> parts, int index) {
		return index >= 0 && index < parts.size() ? (int) parseNumber(parts.get(index)) : 0;
	}

	private static long parseLong(List<String> parts, int index) {
		return index >= 0 && index < parts.size() ? parseNumber(parts.get(index)) : 0L;
	}

	private static long parseNumber(String value) {
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ignored) {
			return 0L;
		}
	}
}
