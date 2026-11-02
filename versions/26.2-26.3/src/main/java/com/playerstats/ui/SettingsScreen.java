package com.playerstats.ui;

import com.playerstats.data.Settings;
import com.playerstats.data.StatsStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * All user facing toggles: what is tracked, how the list looks and which
 * profile the tab follows.
 */
public class SettingsScreen extends Screen {
	private record Toggle(Button button, String key, BooleanSupplier getter) {
	}

	private final Screen parent;
	private final PlayerStatsTab tab;
	private final List<Toggle> toggles = new ArrayList<>();
	private Button themeButton;
	private Button sortButton;
	private Button resetButton;
	private boolean confirmReset;

	public SettingsScreen(Screen parent, PlayerStatsTab tab) {
		super(Component.translatable("playerstats.settings.title"));
		this.parent = parent;
		this.tab = tab;
	}

	@Override
	protected void init() {
		this.toggles.clear();

		int columnWidth = Math.min(170, Math.max(140, (this.width - 40) / 2));
		int gap = 6;
		int leftX = this.width / 2 - columnWidth - gap / 2;
		int rightX = this.width / 2 + gap / 2;
		int top = this.height / 2 - 88;
		int rowHeight = 24;
		int row = 0;

		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.track_totems",
			() -> StatsStore.settings().trackTotems, value -> StatsStore.settings().trackTotems = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.track_kills",
			() -> StatsStore.settings().trackKills, value -> StatsStore.settings().trackKills = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.chat_kills",
			() -> StatsStore.settings().chatKills, value -> StatsStore.settings().chatKills = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.chat_totems",
			() -> StatsStore.settings().chatTotems, value -> StatsStore.settings().chatTotems = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.icons",
			() -> StatsStore.settings().showIcons, value -> StatsStore.settings().showIcons = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "playerstats.setting.compact",
			() -> StatsStore.settings().compactRows, value -> StatsStore.settings().compactRows = value);

		row = 0;
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "playerstats.setting.colors",
			() -> StatsStore.settings().colorValues, value -> StatsStore.settings().colorValues = value);
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "playerstats.setting.hide_empty",
			() -> StatsStore.settings().hideEmpty, value -> StatsStore.settings().hideEmpty = value);
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "playerstats.setting.follow",
			() -> StatsStore.settings().followServer, value -> StatsStore.settings().followServer = value);

		this.themeButton = Button.builder(Component.empty(), button -> {
			StatsStore.settings().nextTheme();
			StatsStore.get().saveNow();
			this.refreshLabels();
		}).bounds(rightX, top + row++ * rowHeight, columnWidth, 20).build();
		this.addRenderableWidget(this.themeButton);

		this.sortButton = Button.builder(Component.empty(), button -> {
			StatsStore.settings().nextSort();
			StatsStore.get().saveNow();
			this.refreshLabels();
		}).bounds(rightX, top + row++ * rowHeight, columnWidth, 20).build();
		this.addRenderableWidget(this.sortButton);

		int bottom = top + 6 * rowHeight + 10;
		this.resetButton = Button.builder(Component.empty(), button -> {
			if (this.confirmReset) {
				StatsStore.get().displayEntry(net.minecraft.client.Minecraft.getInstance()).clearStats();
				StatsStore.get().saveNow();
				this.confirmReset = false;
				this.refreshLabels();
			} else {
				this.confirmReset = true;
				this.refreshLabels();
			}
		}).bounds(leftX, bottom, columnWidth, 20).build();
		this.addRenderableWidget(this.resetButton);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(rightX, bottom, columnWidth, 20)
			.build());

		this.refreshLabels();
	}

	private void addToggle(int x, int y, int width, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
		Button button = Button.builder(Component.empty(), pressed -> {
			setter.accept(!getter.getAsBoolean());
			StatsStore.get().saveNow();
			this.refreshLabels();
		}).bounds(x, y, width, 20).build();
		this.addRenderableWidget(button);
		this.toggles.add(new Toggle(button, key, getter));
	}

	private void refreshLabels() {
		for (Toggle toggle : this.toggles) {
			boolean value = toggle.getter().getAsBoolean();
			toggle.button().setMessage(Component.translatable(toggle.key())
				.append(": ")
				.append(Component.translatable(value ? "playerstats.on" : "playerstats.off")));
		}
		Settings settings = StatsStore.settings();
		if (this.themeButton != null) {
			this.themeButton.setMessage(Component.translatable("playerstats.setting.theme")
				.append(": ")
				.append(Component.translatable(Settings.THEME_KEYS[settings.theme()])));
		}
		if (this.sortButton != null) {
			this.sortButton.setMessage(Component.translatable("playerstats.setting.sort")
				.append(": ")
				.append(Component.translatable(Settings.SORT_KEYS[settings.sort()])));
		}
		if (this.resetButton != null) {
			this.resetButton.setMessage(Component.translatable(
				this.confirmReset ? "playerstats.settings.reset_confirm" : "playerstats.settings.reset"));
		}
	}

	@Override
	public void onClose() {
		this.confirmReset = false;
		if (this.tab != null) {
			this.tab.reload();
		}
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("playerstats.settings.hint"),
			this.width / 2, this.height / 2 - 100, 0xFF9E9E9E);
	}
}
