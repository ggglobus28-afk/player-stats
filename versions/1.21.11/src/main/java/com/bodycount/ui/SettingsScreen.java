package com.bodycount.ui;

import com.bodycount.data.Settings;
import com.bodycount.data.StatsStore;
import net.minecraft.client.gui.GuiGraphics;
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
	private record Toggle(ThemedButton button, String key, BooleanSupplier getter) {
	}

	private final Screen parent;
	private final BodyCountTab tab;
	private final List<Toggle> toggles = new ArrayList<>();
	private ThemedButton themeButton;
	private ThemedButton sortButton;
	private ThemedButton resetButton;
	private boolean confirmReset;

	public SettingsScreen(Screen parent, BodyCountTab tab) {
		super(Component.translatable("bodycount.settings.title"));
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

		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "bodycount.setting.track_totems",
			() -> StatsStore.settings().trackTotems, value -> StatsStore.settings().trackTotems = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "bodycount.setting.track_kills",
			() -> StatsStore.settings().trackKills, value -> StatsStore.settings().trackKills = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "bodycount.setting.chat_kills",
			() -> StatsStore.settings().chatKills, value -> StatsStore.settings().chatKills = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "bodycount.setting.icons",
			() -> StatsStore.settings().showIcons, value -> StatsStore.settings().showIcons = value);
		this.addToggle(leftX, top + row++ * rowHeight, columnWidth, "bodycount.setting.compact",
			() -> StatsStore.settings().compactRows, value -> StatsStore.settings().compactRows = value);

		row = 0;
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "bodycount.setting.colors",
			() -> StatsStore.settings().colorValues, value -> StatsStore.settings().colorValues = value);
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "bodycount.setting.hide_empty",
			() -> StatsStore.settings().hideEmpty, value -> StatsStore.settings().hideEmpty = value);
		this.addToggle(rightX, top + row++ * rowHeight, columnWidth, "bodycount.setting.follow",
			() -> StatsStore.settings().followServer, value -> StatsStore.settings().followServer = value);

		this.themeButton = new ThemedButton(rightX, top + row++ * rowHeight, columnWidth, 20, Component.empty(), () -> {
			StatsStore.settings().nextTheme();
			StatsStore.get().saveNow();
			this.refreshLabels();
		});
		this.addRenderableWidget(this.themeButton);

		this.sortButton = new ThemedButton(rightX, top + row++ * rowHeight, columnWidth, 20, Component.empty(), () -> {
			StatsStore.settings().nextSort();
			StatsStore.get().saveNow();
			this.refreshLabels();
		});
		this.addRenderableWidget(this.sortButton);

		int bottom = top + 6 * rowHeight + 10;
		this.resetButton = new ThemedButton(leftX, bottom, columnWidth, 20, Component.empty(), () -> {
			if (this.confirmReset) {
				StatsStore.get().displayEntry(net.minecraft.client.Minecraft.getInstance()).clearStats();
				StatsStore.get().saveNow();
				this.confirmReset = false;
				this.refreshLabels();
			} else {
				this.confirmReset = true;
				this.refreshLabels();
			}
		});
		this.addRenderableWidget(this.resetButton);

		this.addRenderableWidget(new ThemedButton(rightX, bottom, columnWidth, 20,
			CommonComponents.GUI_DONE, this::onClose));

		this.refreshLabels();
	}

	private void addToggle(int x, int y, int width, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
		ThemedButton button = new ThemedButton(x, y, width, 20, Component.empty(), () -> {
			setter.accept(!getter.getAsBoolean());
			StatsStore.get().saveNow();
			this.refreshLabels();
		});
		this.addRenderableWidget(button);
		this.toggles.add(new Toggle(button, key, getter));
	}

	private void refreshLabels() {
		for (Toggle toggle : this.toggles) {
			boolean value = toggle.getter().getAsBoolean();
			toggle.button().setMessage(Component.translatable(toggle.key())
				.append(": ")
				.append(Component.translatable(value ? "bodycount.on" : "bodycount.off")));
		}
		Settings settings = StatsStore.settings();
		if (this.themeButton != null) {
			this.themeButton.setMessage(Component.translatable("bodycount.setting.theme")
				.append(": ")
				.append(Component.translatable(Settings.THEME_KEYS[settings.theme()])));
		}
		if (this.sortButton != null) {
			this.sortButton.setMessage(Component.translatable("bodycount.setting.sort")
				.append(": ")
				.append(Component.translatable(Settings.SORT_KEYS[settings.sort()])));
		}
		if (this.resetButton != null) {
			this.resetButton.setMessage(Component.translatable(
				this.confirmReset ? "bodycount.settings.reset_confirm" : "bodycount.settings.reset"));
		}
	}

	@Override
	public void onClose() {
		this.confirmReset = false;
		if (this.tab != null) {
			this.tab.reload();
		}
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("bodycount.settings.hint"),
			this.width / 2, this.height / 2 - 100, 0xFF9E9E9E);
	}
}
