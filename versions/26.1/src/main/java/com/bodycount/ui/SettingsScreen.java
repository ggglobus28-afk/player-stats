package com.bodycount.ui;

import com.bodycount.data.Settings;
import com.bodycount.data.StatsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Settings, laid out as a single column of rows: the label on the left and a
 * button with the current value on the right. This keeps the screen compact and
 * readable at every GUI scale.
 */
public class SettingsScreen extends Screen {
	private record Toggle(ThemedButton button, String key, BooleanSupplier getter) {
	}

	private record Label(Component text, int x, int y, int height) {
	}

	private final Screen parent;
	private final BodyCountTab tab;
	private final List<Toggle> toggles = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private Settings current;
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
		this.labels.clear();
		this.current = StatsStore.settings();

		int width = Math.min(360, Math.max(240, this.width - 40));
		int left = (this.width - width) / 2;
		int rows = 10;
		int top = 42;
		int bottomY = this.height - 30;
		int rowHeight = Math.max(16, Math.min(21, (bottomY - top) / rows));
		int buttonWidth = Math.min(104, Math.max(70, width / 3));
		int buttonHeight = Math.min(20, rowHeight - 1);
		int buttonX = left + width - buttonWidth;

		int row = 0;
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.track_totems", () -> this.current.trackTotems, value -> this.current.trackTotems = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.track_kills", () -> this.current.trackKills, value -> this.current.trackKills = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.chat_kills", () -> this.current.chatKills, value -> this.current.chatKills = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.icons", () -> this.current.showIcons, value -> this.current.showIcons = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.compact", () -> this.current.compactRows, value -> this.current.compactRows = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.colors", () -> this.current.colorValues, value -> this.current.colorValues = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.hide_empty", () -> this.current.hideEmpty, value -> this.current.hideEmpty = value);
		this.row(left, top, rowHeight, buttonX, buttonWidth, buttonHeight, row++,
			"bodycount.setting.follow", () -> this.current.followServer, value -> this.current.followServer = value);

		this.themeButton = this.valueButton(buttonX, top + row * rowHeight, buttonWidth, buttonHeight, () -> {
			this.current.nextTheme();
			this.save();
			this.refreshLabels();
		});
		this.labels.add(new Label(Component.translatable("bodycount.setting.theme"),
			left, top + row++ * rowHeight, rowHeight));

		this.sortButton = this.valueButton(buttonX, top + row * rowHeight, buttonWidth, buttonHeight, () -> {
			this.current.nextSort();
			this.save();
			this.refreshLabels();
		});
		this.labels.add(new Label(Component.translatable("bodycount.setting.sort"),
			left, top + row * rowHeight, rowHeight));

		int bottomButtonHeight = 20;
		this.resetButton = new ThemedButton(left, this.height - 26, width / 2 - 2, bottomButtonHeight,
			Component.empty(), () -> {
				if (this.confirmReset) {
					StatsStore.get().displayEntry(Minecraft.getInstance()).clearStats();
					StatsStore.get().saveNow();
					this.confirmReset = false;
					this.refreshLabels();
				} else {
					this.confirmReset = true;
					this.refreshLabels();
				}
			});
		this.addRenderableWidget(this.resetButton);
		this.addRenderableWidget(new ThemedButton(this.width / 2 + 2, this.height - 26,
			(width / 2) - 2 + (width % 2), bottomButtonHeight, CommonComponents.GUI_DONE, this::onClose));

		this.refreshLabels();
	}

	private ThemedButton valueButton(int x, int y, int width, int height, Runnable action) {
		ThemedButton button = new ThemedButton(x, y, width, height, Component.empty(), action);
		this.addRenderableWidget(button);
		return button;
	}

	private void row(int left, int top, int rowHeight, int buttonX, int buttonWidth, int buttonHeight, int row,
			String key, BooleanSupplier getter, Consumer<Boolean> setter) {
		StatsStore store = StatsStore.get();
		ThemedButton button = new ThemedButton(buttonX, top + row * rowHeight, buttonWidth, buttonHeight,
			Component.empty(), () -> {
				setter.accept(!getter.getAsBoolean());
				store.saveNow();
				this.refreshLabels();
			});
		this.addRenderableWidget(button);
		this.toggles.add(new Toggle(button, key, getter));
		this.labels.add(new Label(Component.translatable(key), left, top + row * rowHeight, rowHeight));
	}

	private void save() {
		StatsStore.get().saveNow();
	}

	private void refreshLabels() {
		for (Toggle toggle : this.toggles) {
			boolean value = toggle.getter().getAsBoolean();
			toggle.button().setMessage(Component.translatable(value ? "bodycount.on" : "bodycount.off"));
		}
		Settings settings = StatsStore.settings();
		if (this.themeButton != null) {
			this.themeButton.setMessage(Component.translatable(Settings.THEME_KEYS[settings.theme()]));
		}
		if (this.sortButton != null) {
			this.sortButton.setMessage(Component.translatable(Settings.SORT_KEYS[settings.sort()]));
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("bodycount.settings.hint"),
			this.width / 2, 30, 0xFF9E9E9E);
		for (Label label : this.labels) {
			int y = label.y() + (label.height() - 8) / 2;
			graphics.text(this.font, label.text(), label.x(), y, 0xFFFFFFFF, false);
		}
	}
}
