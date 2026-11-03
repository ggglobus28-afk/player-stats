package com.bodycount.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Button used by the mod's screens. Input handling, focus and narration are
 * inherited from the vanilla button; only the drawing differs. The default
 * theme draws the vanilla sprites, the other themes use their own palette.
 */
public class ThemedButton extends Button {
	private static final Identifier NORMAL = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
	private static final Identifier HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");

	public ThemedButton(Component message, Runnable action) {
		this(0, 0, 0, 0, message, action);
	}

	public ThemedButton(int x, int y, int width, int height, Component message, Runnable action) {
		super(x, y, width, height, message, button -> action.run(), DEFAULT_NARRATION);
	}

	/**
	 * The statistics screen stretches every tab child to the full content
	 * height. Buttons must keep their own height, otherwise they overlap the
	 * list and swallow each other's clicks.
	 */
	@Override
	public void setHeight(int height) {
		super.setHeight(20);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		var font = Minecraft.getInstance().font;
		int x = this.getX();
		int y = this.getY();
		int width = this.getWidth();
		int height = this.getHeight();

		if (Theme.vanilla()) {
			Identifier sprite = !this.active ? DISABLED : (this.isHoveredOrFocused() ? HIGHLIGHTED : NORMAL);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
			graphics.centeredText(font, this.getMessage(), x + width / 2, y + (height - 8) / 2,
				this.active ? 0xFFFFFFFF : 0xFFA0A0A0);
			return;
		}

		int background = this.active
			? (this.isHoveredOrFocused() ? Theme.buttonHover() : Theme.button())
			: Theme.buttonDisabled();
		graphics.fill(x, y, x + width, y + height, background);
		int border = this.active ? Theme.buttonBorder() : Theme.divider();
		graphics.fill(x, y, x + width, y + 1, border);
		graphics.fill(x, y + height - 1, x + width, y + height, border);
		graphics.fill(x, y, x + 1, y + height, border);
		graphics.fill(x + width - 1, y, x + width, y + height, border);
		graphics.centeredText(font, this.getMessage(), x + width / 2, y + (height - 8) / 2,
			this.active ? Theme.buttonText() : Theme.buttonTextDisabled());
	}
}
