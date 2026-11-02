package com.bodycount.ui;

import com.bodycount.data.StatsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The fourth statistics tab. It has three views: one merged per-player table
 * (kills/deaths plus totems popped/lost), one chat pair table, and the server
 * profile manager. Settings open in their own screen.
 */
public class BodyCountTab extends GridLayoutTab {
	private enum View {
		PLAYERS,
		CHAT,
		SERVERS
	}

	/** One row of the chat view: kills in both directions for a pair. */
	private static final class MergedPair {
		private final String a;
		private final String b;
		private int killsAB;
		private int killsBA;
		private long lastSeen;

		private MergedPair(String a, String b) {
			this.a = a;
			this.b = b;
		}

		private String label() {
			return this.a + " \u21c4 " + this.b;
		}

		private int kills() {
			return this.killsAB + this.killsBA;
		}
	}

	private final Screen parent;
	private final StatsListWidget playerList;
	private final StatsListWidget serverList;
	private final ThemedButton playersButton;
	private final ThemedButton chatButton;
	private final ThemedButton serversButton;
	private final ThemedButton settingsButton;
	private final ThemedButton setActiveButton;
	private final ThemedButton renameButton;
	private final ThemedButton mergeButton;
	private final ThemedButton deleteButton;
	private final ThemedButton addCurrentButton;
	private final List<StatsStore.ServerEntry> shownServers = new ArrayList<>();

	private View view = View.PLAYERS;
	private ScreenRectangle area = new ScreenRectangle(0, 0, 320, 240);

	public BodyCountTab(Screen parent) {
		super(Component.translatable("bodycount.tab"));
		this.parent = parent;

		this.playerList = new StatsListWidget(0, 0, 300, 200);
		this.serverList = new StatsListWidget(0, 0, 300, 200);
		this.serverList.setOnSelect(index -> this.updateServerButtons());

		this.playersButton = this.modeButton("bodycount.mode.players", "bodycount.mode.players.tip", View.PLAYERS);
		this.chatButton = this.modeButton("bodycount.mode.chat", "bodycount.mode.chat.tip", View.CHAT);
		this.serversButton = this.modeButton("bodycount.mode.servers", "bodycount.mode.servers.tip", View.SERVERS);

		this.settingsButton = this.button("bodycount.mode.settings", "bodycount.mode.settings.tip", () ->
			Minecraft.getInstance().setScreen(new SettingsScreen(this.parent, this)));

		this.setActiveButton = this.button("bodycount.server.set_active", "bodycount.server.set_active.tip", () -> {
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null) {
				StatsStore.get().setActive(entry);
				this.refresh();
			}
		});

		this.renameButton = this.button("bodycount.server.rename", null, () -> {
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null) {
				Minecraft.getInstance().setScreen(new RenameServerScreen(this.parent, entry));
			}
		});

		this.mergeButton = this.button("bodycount.server.merge", "bodycount.server.merge.tip", () -> {
			StatsStore store = StatsStore.get();
			StatsStore.ServerEntry source = this.selectedServer();
			StatsStore.ServerEntry target = store.active();
			if (source != null && source != target) {
				target.mergeFrom(source);
				store.servers().remove(source);
				store.saveNow();
				this.refresh();
			}
		});

		this.deleteButton = this.button("bodycount.server.delete", "bodycount.server.delete.tip", () -> {
			StatsStore store = StatsStore.get();
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null && store.servers().size() > 1 && !entry.id.equals(store.data().active)) {
				store.servers().remove(entry);
				store.saveNow();
				this.refresh();
			}
		});

		this.addCurrentButton = this.button("bodycount.server.add_current", "bodycount.server.add_current.tip", () -> {
			StatsStore.get().ensureCurrent(Minecraft.getInstance());
			StatsStore.get().saveNow();
			this.refresh();
		});

		this.add(this.playersButton, 0, 0);
		this.add(this.chatButton, 0, 1);
		this.add(this.serversButton, 1, 0);
		this.add(this.settingsButton, 1, 1);
		this.add(this.playerList, 2, 0);
		this.add(this.serverList, 2, 1);
		this.add(this.setActiveButton, 3, 0);
		this.add(this.renameButton, 3, 1);
		this.add(this.mergeButton, 4, 0);
		this.add(this.deleteButton, 4, 1);
		this.add(this.addCurrentButton, 5, 0);

		this.refresh();
		this.layoutWidgets();
	}

	private ThemedButton button(String key, String tipKey, Runnable action) {
		ThemedButton button = new ThemedButton(Component.translatable(key), action);
		if (tipKey != null) {
			button.setTooltip(Tooltip.create(Component.translatable(tipKey)));
		}
		return button;
	}

	private ThemedButton modeButton(String key, String tipKey, View target) {
		return this.button(key, tipKey, () -> {
			this.view = target;
			if (this.view == View.SERVERS) {
				StatsStore.get().importServerList(Minecraft.getInstance());
				StatsStore.get().saveIfDirty();
			}
			this.refresh();
			this.layoutWidgets();
		});
	}

	private void add(AbstractWidget widget, int row, int column) {
		this.layout.addChild(widget, row, column);
	}

	/** Called by the settings screen when it closes. */
	public void reload() {
		this.refresh();
		this.layoutWidgets();
	}

	private void refresh() {
		if (this.view == View.SERVERS) {
			this.refreshServers();
		} else if (this.view == View.CHAT) {
			this.refreshChat();
		} else {
			this.refreshPlayers();
		}
		this.updateModeButtons();
		this.updateServerButtons();
	}

	private void refreshPlayers() {
		StatsStore store = StatsStore.get();
		StatsStore.ServerEntry entry = store.displayEntry(Minecraft.getInstance());
		boolean hideEmpty = StatsStore.settings().hideEmpty;
		List<StatsListWidget.Row> rows = new ArrayList<>();

		List<Map.Entry<String, StatsStore.Opponent>> opponents = new ArrayList<>(entry.players.entrySet());
		opponents.removeIf(opponent -> hideEmpty && opponent.getValue().total() == 0);
		opponents.sort((first, second) -> this.compareOpponents(first.getValue(), second.getValue()));

		ItemStack icon = new ItemStack(Items.TOTEM_OF_UNDYING);
		for (Map.Entry<String, StatsStore.Opponent> opponentEntry : opponents) {
			StatsStore.Opponent opponent = opponentEntry.getValue();
			String name = opponent.name == null || opponent.name.isBlank()
				? shortId(opponentEntry.getKey())
				: opponent.name;

			Component subtitle = Component.translatable("bodycount.sub.totems",
				opponent.totemsDealt, opponent.totemsTaken);
			Component value = Component.literal(opponent.kills + " / " + opponent.deaths);

			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable("bodycount.tip.kills", opponent.kills));
			tooltip.add(Component.translatable("bodycount.tip.deaths", opponent.deaths));
			tooltip.add(Component.translatable("bodycount.tip.kd", ratio(opponent.kills, opponent.deaths)));
			tooltip.add(Component.translatable("bodycount.tip.totems_dealt", opponent.totemsDealt));
			tooltip.add(Component.translatable("bodycount.tip.totems_taken", opponent.totemsTaken));
			tooltip.add(Component.translatable("bodycount.tip.total", opponent.total()));

			rows.add(new StatsListWidget.Row(Component.literal(name), value, icon, tooltip,
				this.colorFor(opponent.kills, opponent.deaths), subtitle));
		}

		this.playerList.setHeader(Component.translatable("bodycount.header.player_n", rows.size()),
			Component.translatable("bodycount.header.kills"));
		this.playerList.setEmptyText(Component.translatable("bodycount.empty"));
		this.playerList.setRows(rows);
	}

	private void refreshChat() {
		StatsStore store = StatsStore.get();
		StatsStore.ServerEntry entry = store.displayEntry(Minecraft.getInstance());
		boolean hideEmpty = StatsStore.settings().hideEmpty;

		Map<String, MergedPair> merged = new LinkedHashMap<>();
		for (StatsStore.ChatPair pair : entry.chatKills.values()) {
			if (pair == null) {
				continue;
			}
			MergedPair target = merged.computeIfAbsent(StatsStore.pairKey(pair.a, pair.b),
				key -> new MergedPair(safe(pair.a), safe(pair.b)));
			target.killsAB += pair.aKillsB;
			target.killsBA += pair.bKillsA;
			target.lastSeen = Math.max(target.lastSeen, pair.lastSeen);
		}

		List<MergedPair> pairs = new ArrayList<>(merged.values());
		pairs.removeIf(pair -> hideEmpty && pair.kills() == 0);
		pairs.sort((first, second) -> this.comparePairs(first, second));

		ItemStack icon = new ItemStack(Items.DIAMOND_SWORD);
		List<StatsListWidget.Row> rows = new ArrayList<>();
		for (MergedPair pair : pairs) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable("bodycount.tip.pair_kills", pair.a, pair.b, pair.killsAB));
			tooltip.add(Component.translatable("bodycount.tip.pair_kills", pair.b, pair.a, pair.killsBA));

			rows.add(new StatsListWidget.Row(Component.literal(pair.label()),
				Component.literal(pair.killsAB + " / " + pair.killsBA), icon, tooltip,
				this.colorFor(pair.killsAB, pair.killsBA), null));
		}

		this.playerList.setHeader(Component.translatable("bodycount.header.pair_n", rows.size()),
			Component.translatable("bodycount.header.pair_kills"));
		this.playerList.setEmptyText(Component.translatable("bodycount.empty.chat"));
		this.playerList.setRows(rows);
	}

	private void refreshServers() {
		StatsStore store = StatsStore.get();
		this.shownServers.clear();
		this.shownServers.addAll(store.servers());
		this.shownServers.sort((first, second) -> {
			boolean firstActive = first.id.equals(store.data().active);
			boolean secondActive = second.id.equals(store.data().active);
			if (firstActive != secondActive) {
				return firstActive ? -1 : 1;
			}
			return StatsStore.displayName(first).compareToIgnoreCase(StatsStore.displayName(second));
		});

		ItemStack icon = new ItemStack(Items.COMPASS);
		List<StatsListWidget.Row> rows = new ArrayList<>();
		for (StatsStore.ServerEntry entry : this.shownServers) {
			boolean active = entry.id.equals(store.data().active);
			Component value = active
				? Component.translatable("bodycount.server.active")
				: Component.literal(entry.singleplayer ? "" : entry.address);
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable("bodycount.tip.server_players", entry.players.size()));
			tooltip.add(Component.translatable("bodycount.tip.server_kills", entry.kills(), entry.deaths()));
			tooltip.add(Component.translatable("bodycount.tip.server_totems", entry.totemsDealt(), entry.totemsTaken()));
			tooltip.add(Component.translatable("bodycount.tip.server_chat", entry.chatKillsTotal()));
			rows.add(new StatsListWidget.Row(
				Component.literal(StatsStore.displayName(entry)), value, icon, tooltip,
				active ? Theme.good() : 0, null));
		}

		this.serverList.setHeader(Component.translatable("bodycount.header.server_n", rows.size()),
			Component.translatable("bodycount.header.address"));
		this.serverList.setEmptyText(Component.translatable("bodycount.empty"));
		this.serverList.setRows(rows);
		if (this.serverList.getSelected() < 0 && !rows.isEmpty()) {
			this.serverList.setSelected(0);
		}
	}

	private StatsStore.ServerEntry selectedServer() {
		int index = this.serverList.getSelected();
		return index >= 0 && index < this.shownServers.size() ? this.shownServers.get(index) : null;
	}

	private void updateModeButtons() {
		this.label(this.playersButton, "bodycount.mode.players", this.view == View.PLAYERS);
		this.label(this.chatButton, "bodycount.mode.chat", this.view == View.CHAT);
		this.label(this.serversButton, "bodycount.mode.servers", this.view == View.SERVERS);
	}

	private void label(ThemedButton button, String key, boolean active) {
		Component text = Component.translatable(key);
		button.setMessage(active ? Component.literal("\u2022 ").append(text) : text);
	}

	private void updateServerButtons() {
		StatsStore store = StatsStore.get();
		StatsStore.ServerEntry entry = this.selectedServer();
		boolean hasEntry = entry != null;
		boolean isActive = hasEntry && entry.id.equals(store.data().active);

		this.setActiveButton.active = hasEntry && !isActive;
		this.renameButton.active = hasEntry;
		this.mergeButton.active = hasEntry && !isActive && store.servers().size() > 1;
		this.deleteButton.active = hasEntry && !isActive && store.servers().size() > 1;
		this.addCurrentButton.active = true;
	}

	private int compareOpponents(StatsStore.Opponent a, StatsStore.Opponent b) {
		int sort = StatsStore.settings().sort();
		if (sort == 1) {
			return safe(a.name).compareToIgnoreCase(safe(b.name));
		}
		int firstValue;
		int secondValue;
		if (sort == 2) {
			firstValue = a.fights();
			secondValue = b.fights();
		} else if (sort == 3) {
			firstValue = a.totems();
			secondValue = b.totems();
		} else {
			firstValue = a.total();
			secondValue = b.total();
		}
		int compared = Integer.compare(secondValue, firstValue);
		return compared != 0 ? compared : safe(a.name).compareToIgnoreCase(safe(b.name));
	}

	private int comparePairs(MergedPair first, MergedPair second) {
		int sort = StatsStore.settings().sort();
		if (sort == 1) {
			return first.label().compareToIgnoreCase(second.label());
		}
		int compared = Integer.compare(second.kills(), first.kills());
		return compared != 0 ? compared : first.label().compareToIgnoreCase(second.label());
	}

	private int colorFor(int mine, int theirs) {
		if (!StatsStore.settings().colorValues) {
			return 0;
		}
		if (mine > theirs) {
			return Theme.good();
		}
		if (mine < theirs) {
			return Theme.bad();
		}
		return 0;
	}

	private void layoutWidgets() {
		int width = Math.min(400, Math.max(220, this.area.width() - 16));
		int left = this.area.left() + (this.area.width() - width) / 2;
		int top = this.area.top() + 4;
		int gap = 4;
		int buttonWidth = (width - gap) / 2;

		this.place(this.playersButton, left, top, buttonWidth);
		this.place(this.chatButton, left + buttonWidth + gap, top, width - buttonWidth - gap);

		int secondRow = top + 22;
		this.place(this.serversButton, left, secondRow, buttonWidth);
		this.place(this.settingsButton, left + buttonWidth + gap, secondRow, width - buttonWidth - gap);

		int listTop = secondRow + 24;
		int listHeight = Math.max(40, this.area.bottom() - listTop - 4);

		boolean servers = this.view == View.SERVERS;
		if (servers) {
			listHeight = Math.max(40, listHeight - 48);
			int firstRow = this.area.bottom() - 44;
			int secondActionRow = this.area.bottom() - 22;

			this.place(this.setActiveButton, left, firstRow, buttonWidth);
			this.place(this.renameButton, left + buttonWidth + gap, firstRow, width - buttonWidth - gap);
			this.place(this.mergeButton, left, secondActionRow, buttonWidth);
			this.place(this.addCurrentButton, left + buttonWidth + gap, secondActionRow, buttonWidth);
			this.place(this.deleteButton, left + (buttonWidth + gap) * 2, secondActionRow, width - (buttonWidth + gap) * 2);
		}

		this.playerList.setX(left);
		this.playerList.setY(listTop);
		this.playerList.setWidth(width);
		this.playerList.setHeight(listHeight);

		this.serverList.setX(left);
		this.serverList.setY(listTop);
		this.serverList.setWidth(width);
		this.serverList.setHeight(listHeight);

		this.playerList.visible = !servers;
		this.serverList.visible = servers;
		this.setActiveButton.visible = servers;
		this.renameButton.visible = servers;
		this.mergeButton.visible = servers;
		this.deleteButton.visible = servers;
		this.addCurrentButton.visible = servers;
	}

	private void place(AbstractWidget widget, int x, int y, int width) {
		widget.setX(x);
		widget.setY(y);
		widget.setWidth(width);
		widget.setHeight(20);
	}

	@Override
	public void doLayout(ScreenRectangle rectangle) {
		this.area = rectangle;
		this.layoutWidgets();
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private static String ratio(int kills, int deaths) {
		if (deaths == 0) {
			return kills == 0 ? "0.00" : String.valueOf(kills);
		}
		return String.format(java.util.Locale.ROOT, "%.2f", kills / (float) deaths);
	}

	private static String shortId(String id) {
		return id == null || id.isBlank() ? "?" : (id.length() > 8 ? id.substring(0, 8) : id);
	}
}
