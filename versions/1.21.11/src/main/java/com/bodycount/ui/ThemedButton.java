package com.bodycount.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Button used by the mod's screens. With the default theme it draws the
 * vanilla button sprites, matching the statistics screen. Every other theme
 * paints the button with its own palette.
 */
public class ThemedButton extends AbstractWidget {
	private static final Identifier NORMAL = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
	private static final Identifier HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final int KEY_ENTER = 257;
	private static final int KEY_KP_ENTER = 335;
	private static final int KEY_SPACE = 32;

	private final Runnable onPress;

	public ThemedButton(Component message, Runnable onPress) {
		this(0, 0, 0, 0, message, onPress);
	}

	public ThemedButton(int x, int y, int width, int height, Component message, Runnable onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		var font = Minecraft.getInstance().font;
		int x = this.getX();
		int y = this.getY();
		int width = this.getWidth();
		int height = this.getHeight();

		if (Theme.vanilla()) {
			Identifier sprite = !this.active ? DISABLED : (this.isHoveredOrFocused() ? HIGHLIGHTED : NORMAL);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
			graphics.drawCenteredString(font, this.getMessage(), x + width / 2, y + (height - 8) / 2,
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
		graphics.drawCenteredString(font, this.getMessage(), x + width / 2, y + (height - 8) / 2,
			this.active ? Theme.buttonText() : Theme.buttonTextDisabled());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!this.visible || !this.active || event.button() != 0 || !this.isMouseOver(event.x(), event.y())) {
			return false;
		}
		this.playDownSound(Minecraft.getInstance().getSoundManager());
		this.onPress.run();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.visible || !this.active) {
			return false;
		}
		int key = event.key();
		if (key == KEY_ENTER || key == KEY_KP_ENTER || key == KEY_SPACE) {
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.onPress.run();
			return true;
		}
		return false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, this.getMessage());
	}
}
