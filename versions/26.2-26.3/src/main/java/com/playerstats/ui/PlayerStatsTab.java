package com.playerstats.ui;

import com.playerstats.data.StatsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The fourth statistics tab. It has five views: packet based totems and kills,
 * chat based kill and totem pairs, and the server profile manager.
 */
public class PlayerStatsTab extends GridLayoutTab {
	private enum View {
		TOTEMS,
		KILLS,
		CHAT_KILLS,
		CHAT_TOTEMS,
		SERVERS
	}

	private final Screen parent;
	private final StatsListWidget playerList;
	private final StatsListWidget serverList;
	private final Button totemButton;
	private final Button killButton;
	private final Button chatKillButton;
	private final Button chatTotemButton;
	private final Button serversButton;
	private final Button settingsButton;
	private final Button setActiveButton;
	private final Button renameButton;
	private final Button mergeButton;
	private final Button deleteButton;
	private final Button addCurrentButton;
	private final List<StatsStore.ServerEntry> shownServers = new ArrayList<>();

	private View view = View.TOTEMS;
	private ScreenRectangle area = new ScreenRectangle(0, 0, 320, 240);

	public PlayerStatsTab(Screen parent) {
		super(Component.translatable("playerstats.tab"));
		this.parent = parent;

		this.playerList = new StatsListWidget(0, 0, 300, 200);
		this.serverList = new StatsListWidget(0, 0, 300, 200);
		this.serverList.setOnSelect(index -> this.updateServerButtons());

		this.totemButton = this.modeButton("playerstats.mode.totems", "playerstats.mode.totems.tip", () -> View.TOTEMS);
		this.killButton = this.modeButton("playerstats.mode.kills", "playerstats.mode.kills.tip", () -> View.KILLS);
		this.chatKillButton = this.modeButton("playerstats.mode.chat_kills", "playerstats.mode.chat_kills.tip", () -> View.CHAT_KILLS);
		this.chatTotemButton = this.modeButton("playerstats.mode.chat_totems", "playerstats.mode.chat_totems.tip", () -> View.CHAT_TOTEMS);
		this.serversButton = this.modeButton("playerstats.mode.servers", "playerstats.mode.servers.tip", () -> View.SERVERS);

		this.settingsButton = Button.builder(Component.translatable("playerstats.mode.settings"), button ->
			Minecraft.getInstance().gui.setScreen(new SettingsScreen(this.parent, this)))
			.tooltip(Tooltip.create(Component.translatable("playerstats.mode.settings.tip")))
			.build();

		this.setActiveButton = Button.builder(Component.translatable("playerstats.server.set_active"), button -> {
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null) {
				StatsStore.get().setActive(entry);
				this.refresh();
			}
		}).tooltip(Tooltip.create(Component.translatable("playerstats.server.set_active.tip"))).build();

		this.renameButton = Button.builder(Component.translatable("playerstats.server.rename"), button -> {
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null) {
				Minecraft.getInstance().gui.setScreen(new RenameServerScreen(this.parent, entry));
			}
		}).build();

		this.mergeButton = Button.builder(Component.translatable("playerstats.server.merge"), button -> {
			StatsStore store = StatsStore.get();
			StatsStore.ServerEntry source = this.selectedServer();
			StatsStore.ServerEntry target = store.active();
			if (source != null && source != target) {
				target.mergeFrom(source);
				store.servers().remove(source);
				store.saveNow();
				this.refresh();
			}
		}).tooltip(Tooltip.create(Component.translatable("playerstats.server.merge.tip"))).build();

		this.deleteButton = Button.builder(Component.translatable("playerstats.server.delete"), button -> {
			StatsStore store = StatsStore.get();
			StatsStore.ServerEntry entry = this.selectedServer();
			if (entry != null && store.servers().size() > 1 && !entry.id.equals(store.data().active)) {
				store.servers().remove(entry);
				store.saveNow();
				this.refresh();
			}
		}).tooltip(Tooltip.create(Component.translatable("playerstats.server.delete.tip"))).build();

		this.addCurrentButton = Button.builder(Component.translatable("playerstats.server.add_current"), button -> {
			StatsStore.get().ensureCurrent(Minecraft.getInstance());
			StatsStore.get().saveNow();
			this.refresh();
		}).tooltip(Tooltip.create(Component.translatable("playerstats.server.add_current.tip"))).build();

		this.add(this.totemButton, 0, 0);
		this.add(this.killButton, 0, 1);
		this.add(this.serversButton, 0, 2);
		this.add(this.chatKillButton, 1, 0);
		this.add(this.chatTotemButton, 1, 1);
		this.add(this.settingsButton, 1, 2);
		this.add(this.playerList, 2, 0);
		this.add(this.serverList, 2, 1);
		this.add(this.setActiveButton, 3, 0);
		this.add(this.renameButton, 3, 1);
		this.add(this.mergeButton, 3, 2);
		this.add(this.deleteButton, 4, 0);
		this.add(this.addCurrentButton, 4, 1);

		this.refresh();
		this.layoutWidgets();
	}

	private Button modeButton(String key, String tipKey, java.util.function.Supplier<View> target) {
		return Button.builder(Component.translatable(key), button -> {
			this.view = target.get();
			if (this.view == View.SERVERS) {
				StatsStore.get().importServerList(Minecraft.getInstance());
				StatsStore.get().saveIfDirty();
			}
			this.refresh();
			this.layoutWidgets();
		}).tooltip(Tooltip.create(Component.translatable(tipKey))).build();
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

		if (this.view == View.TOTEMS || this.view == View.KILLS) {
			boolean totems = this.view == View.TOTEMS;
			List<Map.Entry<String, StatsStore.Opponent>> opponents = new ArrayList<>(entry.players.entrySet());
			opponents.removeIf(opponent -> hideEmpty && (totems ? opponent.getValue().totems() : opponent.getValue().fights()) == 0);
			opponents.sort((first, second) -> {
				StatsStore.Opponent a = first.getValue();
				StatsStore.Opponent b = second.getValue();
				return this.compare(a, b, totems);
			});

			ItemStack icon = new ItemStack(totems ? Items.TOTEM_OF_UNDYING : Items.IRON_SWORD);
			for (Map.Entry<String, StatsStore.Opponent> opponentEntry : opponents) {
				StatsStore.Opponent opponent = opponentEntry.getValue();
				String name = opponent.name == null || opponent.name.isBlank()
					? shortId(opponentEntry.getKey())
					: opponent.name;
				int mine = totems ? opponent.totemsDealt : opponent.kills;
				int theirs = totems ? opponent.totemsTaken : opponent.deaths;
				Component value = Component.literal(mine + " / " + theirs);
				List<Component> tooltip = new ArrayList<>();
				if (totems) {
					tooltip.add(Component.translatable("playerstats.tip.totems_dealt", opponent.totemsDealt));
					tooltip.add(Component.translatable("playerstats.tip.totems_taken", opponent.totemsTaken));
				} else {
					tooltip.add(Component.translatable("playerstats.tip.kills", opponent.kills));
					tooltip.add(Component.translatable("playerstats.tip.deaths", opponent.deaths));
					tooltip.add(Component.translatable("playerstats.tip.kd", ratio(opponent.kills, opponent.deaths)));
				}
				rows.add(new StatsListWidget.Row(Component.literal(name), value, icon, tooltip, this.colorFor(mine, theirs)));
			}

			this.playerList.setHeader(Component.translatable("playerstats.header.player_n", rows.size()),
				Component.translatable(totems ? "playerstats.header.totems" : "playerstats.header.kills"));
			this.playerList.setEmptyText(Component.translatable("playerstats.empty"));
		} else if (this.view == View.CHAT_KILLS) {
			List<StatsStore.ChatPair> pairs = new ArrayList<>(entry.chatKills.values());
			pairs.removeIf(pair -> hideEmpty && pair.total() == 0);
			pairs.sort((first, second) -> this.comparePairs(first, second, false));
			ItemStack icon = new ItemStack(Items.DIAMOND_SWORD);
			for (StatsStore.ChatPair pair : pairs) {
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable("playerstats.tip.pair_kills", pair.a, pair.b, pair.aKillsB));
				tooltip.add(Component.translatable("playerstats.tip.pair_kills", pair.b, pair.a, pair.bKillsA));
				rows.add(new StatsListWidget.Row(
					Component.literal(pair.a + " \u21c4 " + pair.b),
					Component.literal(pair.aKillsB + " / " + pair.bKillsA),
					icon, tooltip, this.colorFor(pair.aKillsB, pair.bKillsA)));
			}
			this.playerList.setHeader(Component.translatable("playerstats.header.pair_n", rows.size()),
				Component.translatable("playerstats.header.pair_kills"));
			this.playerList.setEmptyText(Component.translatable("playerstats.empty.chat"));
		} else {
			List<StatsStore.ChatPair> pairs = new ArrayList<>(entry.chatTotems.values());
			pairs.removeIf(pair -> hideEmpty && pair.total() == 0);
			pairs.sort((first, second) -> this.comparePairs(first, second, true));
			ItemStack icon = new ItemStack(Items.TOTEM_OF_UNDYING);
			for (StatsStore.ChatPair pair : pairs) {
				List<Component> tooltip = new ArrayList<>();
				Component name;
				Component value;
				int mine;
				int theirs;
				if (pair.single()) {
					String solo = pair.soloName();
					int total = pair.aTotemsB + pair.bTotemsA;
					name = Component.literal(solo);
					value = Component.literal(String.valueOf(total));
					mine = total;
					theirs = 0;
					tooltip.add(Component.translatable("playerstats.tip.totem_solo", solo, total));
				} else {
					name = Component.literal(pair.a + " \u21c4 " + pair.b);
					value = Component.literal(pair.aTotemsB + " / " + pair.bTotemsA);
					mine = pair.aTotemsB;
					theirs = pair.bTotemsA;
					tooltip.add(Component.translatable("playerstats.tip.pair_totems", pair.a, pair.b, pair.aTotemsB));
					tooltip.add(Component.translatable("playerstats.tip.pair_totems", pair.b, pair.a, pair.bTotemsA));
				}
				rows.add(new StatsListWidget.Row(name, value, icon, tooltip, this.colorFor(mine, theirs)));
			}
			this.playerList.setHeader(Component.translatable("playerstats.header.pair_n", rows.size()),
				Component.translatable("playerstats.header.pair_totems"));
			this.playerList.setEmptyText(Component.translatable("playerstats.empty.chat"));
		}

		this.playerList.setRows(rows);
	}

	private int compare(StatsStore.Opponent a, StatsStore.Opponent b, boolean totems) {
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
			firstValue = totems ? a.totems() : a.fights();
			secondValue = totems ? b.totems() : b.fights();
		}
		int compared = Integer.compare(secondValue, firstValue);
		if (compared != 0) {
			return compared;
		}
		return safe(a.name).compareToIgnoreCase(safe(b.name));
	}

	private int comparePairs(StatsStore.ChatPair first, StatsStore.ChatPair second, boolean totems) {
		int sort = StatsStore.settings().sort();
		String firstName = first.single() ? first.soloName() : first.a + " " + first.b;
		String secondName = second.single() ? second.soloName() : second.a + " " + second.b;
		if (sort == 1) {
			return firstName.compareToIgnoreCase(secondName);
		}
		int firstValue;
		int secondValue;
		if (totems) {
			firstValue = sort == 2 ? first.aKillsB + first.bKillsA : first.aTotemsB + first.bTotemsA;
			secondValue = sort == 2 ? second.aKillsB + second.bKillsA : second.aTotemsB + second.bTotemsA;
		} else {
			firstValue = sort == 3 ? first.aTotemsB + first.bTotemsA : first.aKillsB + first.bKillsA;
			secondValue = sort == 3 ? second.aTotemsB + second.bTotemsA : second.aKillsB + second.bKillsA;
		}
		int compared = Integer.compare(secondValue, firstValue);
		if (compared != 0) {
			return compared;
		}
		return firstName.compareToIgnoreCase(secondName);
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
				? Component.translatable("playerstats.server.active")
				: Component.literal(entry.singleplayer ? "" : entry.address);
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable("playerstats.tip.server_players", entry.players.size()));
			tooltip.add(Component.translatable("playerstats.tip.server_kills", entry.kills(), entry.deaths()));
			tooltip.add(Component.translatable("playerstats.tip.server_totems", entry.totemsDealt(), entry.totemsTaken()));
			tooltip.add(Component.translatable("playerstats.tip.server_chat", entry.chatKillsTotal(), entry.chatTotemsTotal()));
			rows.add(new StatsListWidget.Row(
				Component.literal(StatsStore.displayName(entry)), value, icon, tooltip,
				active ? Theme.good() : 0));
		}

		this.serverList.setHeader(Component.translatable("playerstats.header.server_n", rows.size()),
			Component.translatable("playerstats.header.address"));
		this.serverList.setEmptyText(Component.translatable("playerstats.empty"));
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
		this.label(this.totemButton, "playerstats.mode.totems", this.view == View.TOTEMS);
		this.label(this.killButton, "playerstats.mode.kills", this.view == View.KILLS);
		this.label(this.chatKillButton, "playerstats.mode.chat_kills", this.view == View.CHAT_KILLS);
		this.label(this.chatTotemButton, "playerstats.mode.chat_totems", this.view == View.CHAT_TOTEMS);
		this.label(this.serversButton, "playerstats.mode.servers", this.view == View.SERVERS);
	}

	private void label(Button button, String key, boolean active) {
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
		int buttonWidth = (width - gap * 2) / 3;

		this.place(this.totemButton, left, top, buttonWidth);
		this.place(this.killButton, left + buttonWidth + gap, top, buttonWidth);
		this.place(this.serversButton, left + (buttonWidth + gap) * 2, top, width - (buttonWidth + gap) * 2);

		int secondRow = top + 22;
		this.place(this.chatKillButton, left, secondRow, buttonWidth);
		this.place(this.chatTotemButton, left + buttonWidth + gap, secondRow, buttonWidth);
		this.place(this.settingsButton, left + (buttonWidth + gap) * 2, secondRow, width - (buttonWidth + gap) * 2);

		int listTop = secondRow + 24;
		int listHeight = Math.max(40, this.area.bottom() - listTop - 4);

		boolean servers = this.view == View.SERVERS;
		if (servers) {
			listHeight = Math.max(40, listHeight - 48);
			int firstRow = this.area.bottom() - 44;
			int secondActionRow = this.area.bottom() - 22;

			this.place(this.setActiveButton, left, firstRow, buttonWidth);
			this.place(this.renameButton, left + buttonWidth + gap, firstRow, buttonWidth);
			this.place(this.mergeButton, left + (buttonWidth + gap) * 2, firstRow, width - (buttonWidth + gap) * 2);
			this.place(this.deleteButton, left, secondActionRow, buttonWidth);
			this.place(this.addCurrentButton, left + buttonWidth + gap, secondActionRow, width - buttonWidth - gap);
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
