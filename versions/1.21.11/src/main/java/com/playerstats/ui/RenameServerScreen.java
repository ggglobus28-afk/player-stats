package com.playerstats.ui;

import com.playerstats.data.StatsStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Tiny modal screen used to rename a server profile.
 */
public class RenameServerScreen extends Screen {
	private final Screen parent;
	private final StatsStore.ServerEntry entry;
	private EditBox nameBox;

	public RenameServerScreen(Screen parent, StatsStore.ServerEntry entry) {
		super(Component.translatable("playerstats.rename.title"));
		this.parent = parent;
		this.entry = entry;
	}

	@Override
	protected void init() {
		int width = 200;
		int left = (this.width - width) / 2;
		int top = this.height / 2 - 30;

		this.nameBox = new EditBox(this.font, left, top, width, 20, Component.translatable("playerstats.rename.hint"));
		this.nameBox.setMaxLength(64);
		this.nameBox.setValue(this.entry.name == null ? "" : this.entry.name);
		this.addRenderableWidget(this.nameBox);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.save())
			.bounds(left, top + 28, width, 20)
			.build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
			.bounds(left, top + 52, width, 20)
			.build());

		this.setInitialFocus(this.nameBox);
	}

	private void save() {
		String value = this.nameBox.getValue().trim();
		if (!value.isEmpty()) {
			this.entry.name = value;
			StatsStore.get().saveNow();
		}
		this.onClose();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 56, 0xFFFFFFFF);
	}
}