package com.bodycount.ui;

import com.bodycount.data.StatsStore;
import net.minecraft.client.gui.GuiGraphics;
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
		super(Component.translatable("bodycount.rename.title"));
		this.parent = parent;
		this.entry = entry;
	}

	@Override
	protected void init() {
		int width = 200;
		int left = (this.width - width) / 2;
		int top = this.height / 2 - 30;

		this.nameBox = new EditBox(this.font, left, top, width, 20, Component.translatable("bodycount.rename.hint"));
		this.nameBox.setMaxLength(64);
		this.nameBox.setValue(this.entry.name == null ? "" : this.entry.name);
		this.addRenderableWidget(this.nameBox);

		this.addRenderableWidget(new ThemedButton(left, top + 28, width, 20,
			CommonComponents.GUI_DONE, this::save));
		this.addRenderableWidget(new ThemedButton(left, top + 52, width, 20,
			CommonComponents.GUI_CANCEL, this::onClose));

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
