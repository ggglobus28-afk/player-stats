package com.bodycount.ui;

import com.bodycount.data.StatsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Small vanilla-looking scrolling list: one icon, a name on the left and a
 * value on the right per row. Used for players, chat pairs and server profiles.
 */
public class StatsListWidget extends AbstractWidget {
	public static final int ROW_HEIGHT = 22;
	public static final int COMPACT_ROW_HEIGHT = 18;
	private static final int HEADER_HEIGHT = 16;
	private static final int PADDING = 4;

	public static final class Row {
		public final Component name;
		public final Component value;
		public final ItemStack icon;
		public final List<Component> tooltip;
		public final int valueColor;

		public Row(Component name, Component value, ItemStack icon) {
			this(name, value, icon, List.of(), 0);
		}

		public Row(Component name, Component value, ItemStack icon, List<Component> tooltip) {
			this(name, value, icon, tooltip, 0);
		}

		public Row(Component name, Component value, ItemStack icon, List<Component> tooltip, int valueColor) {
			this.name = name;
			this.value = value;
			this.icon = icon;
			this.tooltip = tooltip == null ? List.of() : tooltip;
			this.valueColor = valueColor;
		}
	}

	private final List<Row> rows = new ArrayList<>();
	private Component headerLeft = Component.empty();
	private Component headerRight = Component.empty();
	private Component emptyText = Component.empty();
	private int scroll;
	private int selected = -1;
	private IntConsumer onSelect;

	public StatsListWidget(int x, int y, int width, int height) {
		super(x, y, width, height, Component.empty());
	}

	private static int rowHeight() {
		return StatsStore.settings() != null && StatsStore.settings().compactRows
			? COMPACT_ROW_HEIGHT
			: ROW_HEIGHT;
	}

	public void setRows(List<Row> newRows) {
		this.rows.clear();
		this.rows.addAll(newRows);
		if (this.selected >= this.rows.size()) {
			this.selected = -1;
		}
		this.clampScroll();
	}

	public void setHeader(Component left, Component right) {
		this.headerLeft = left;
		this.headerRight = right;
	}

	public void setEmptyText(Component text) {
		this.emptyText = text;
	}

	public void setOnSelect(IntConsumer onSelect) {
		this.onSelect = onSelect;
	}

	public int getSelected() {
		return this.selected;
	}

	public void setSelected(int index) {
		this.selected = index;
	}

	public int getScroll() {
		return this.scroll;
	}

	private int contentHeight() {
		return this.rows.size() * rowHeight();
	}

	private int visibleHeight() {
		return Math.max(0, this.getHeight() - HEADER_HEIGHT);
	}

	private int maxScroll() {
		return Math.max(0, this.contentHeight() - this.visibleHeight());
	}

	private void clampScroll() {
		this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		var font = Minecraft.getInstance().font;
		int left = this.getX();
		int right = left + this.getWidth();
		int top = this.getY();
		int bottom = top + this.getHeight();

		graphics.fill(left, top, right, bottom, Theme.background());
		graphics.drawString(font, this.headerLeft, left + PADDING, top + 4, Theme.header(), false);
		if (this.headerRight != null && !this.headerRight.getString().isEmpty()) {
			int width = font.width(this.headerRight);
			graphics.drawString(font, this.headerRight, right - PADDING - width, top + 4, Theme.header(), false);
		}
		graphics.fill(left, top + HEADER_HEIGHT - 1, right, top + HEADER_HEIGHT, Theme.divider());

		int listTop = top + HEADER_HEIGHT;
		graphics.enableScissor(left, listTop, right, bottom);

		Row hoveredRow = null;
		int rowHeight = rowHeight();
		boolean icons = StatsStore.settings() == null || StatsStore.settings().showIcons;

		if (this.rows.isEmpty()) {
			graphics.drawCenteredString(font, this.emptyText, (left + right) / 2, listTop + 14, Theme.header());
		} else {
			for (int i = 0; i < this.rows.size(); i++) {
				int rowTop = listTop + i * rowHeight - this.scroll;
				if (rowTop + rowHeight <= listTop || rowTop >= bottom) {
					continue;
				}
				boolean hovered = mouseX >= left && mouseX < right && mouseY >= rowTop && mouseY < rowTop + rowHeight
					&& mouseY >= listTop && mouseY < bottom;
				if (i == this.selected) {
					graphics.fill(left, rowTop, right, rowTop + rowHeight - 1, Theme.selected());
				} else if (hovered) {
					graphics.fill(left, rowTop, right, rowTop + rowHeight - 1, Theme.hover());
				}

				Row row = this.rows.get(i);
				int textLeft = left + PADDING;
				if (icons && !row.icon.isEmpty()) {
					graphics.renderItem(row.icon, left + PADDING, rowTop + (rowHeight - 16) / 2);
					textLeft += 20;
				}
				int textY = rowTop + (rowHeight - 8) / 2;
				graphics.drawString(font, row.name, textLeft, textY, Theme.text(), false);
				int width = font.width(row.value);
				int valueColor = row.valueColor == 0 ? Theme.value() : row.valueColor;
				graphics.drawString(font, row.value, right - PADDING - width, textY, valueColor, false);
				if (hovered) {
					hoveredRow = row;
				}
			}
		}

		graphics.disableScissor();

		if (this.maxScroll() > 0) {
			int trackTop = listTop;
			int trackBottom = bottom;
			int thumbHeight = Math.max(12, (int) (this.visibleHeight() / (float) this.contentHeight() * (trackBottom - trackTop)));
			int thumbTop = trackTop + (int) (this.scroll / (float) this.maxScroll() * (trackBottom - trackTop - thumbHeight));
			graphics.fill(right - 4, trackTop, right - 2, trackBottom, 0x40000000);
			graphics.fill(right - 4, thumbTop, right - 2, thumbTop + thumbHeight, Theme.divider());
		}

		if (hoveredRow != null && !hoveredRow.tooltip.isEmpty()) {
			this.drawTooltip(graphics, font, hoveredRow.tooltip, mouseX, mouseY);
		}
	}

	private void drawTooltip(GuiGraphics graphics, net.minecraft.client.gui.Font font,
			List<Component> lines, int mouseX, int mouseY) {
		int width = 0;
		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}
		int height = lines.size() * 10;
		var window = Minecraft.getInstance().getWindow();
		int screenWidth = window.getGuiScaledWidth();
		int screenHeight = window.getGuiScaledHeight();
		int x = mouseX + 10;
		int y = mouseY - 4;
		if (x + width + 6 > screenWidth) {
			x = screenWidth - width - 6;
		}
		if (y + height + 6 > screenHeight) {
			y = screenHeight - height - 6;
		}
		x = Math.max(2, x);
		y = Math.max(2, y);

		graphics.fill(x - 3, y - 3, x + width + 5, y + height + 5, 0xFF000000);
		graphics.fill(x - 2, y - 2, x + width + 4, y + height + 4, Theme.tooltipBackground());
		int lineY = y;
		for (Component line : lines) {
			graphics.drawString(font, line, x, lineY, Theme.tooltipText(), false);
			lineY += 10;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!this.visible || !this.active || event.button() != 0) {
			return false;
		}
		int listTop = this.getY() + HEADER_HEIGHT;
		int left = this.getX();
		int right = left + this.getWidth();
		int bottom = this.getY() + this.getHeight();
		if (event.x() < left || event.x() >= right || event.y() < listTop || event.y() >= bottom) {
			return false;
		}
		int index = (int) ((event.y() - listTop + this.scroll) / rowHeight());
		if (index >= 0 && index < this.rows.size()) {
			this.selected = index;
			if (this.onSelect != null) {
				this.onSelect.accept(index);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY) || this.maxScroll() <= 0) {
			return false;
		}
		this.scroll = (int) Math.max(0, Math.min(this.maxScroll(), this.scroll - scrollY * rowHeight()));
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
