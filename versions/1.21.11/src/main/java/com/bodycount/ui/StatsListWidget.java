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
 * Scrolling list used for players, chat pairs and server profiles. It follows
 * the vanilla statistics list: a small grey header, thin separator, plain rows
 * with an icon, a name on the left and one value on the right. Rows can carry
 * an optional second line, for example the totem counters under a name.
 */
public class StatsListWidget extends AbstractWidget {
	public static final int ROW_HEIGHT = 20;
	public static final int TALL_ROW_HEIGHT = 26;
	public static final int COMPACT_ROW_HEIGHT = 16;
	private static final int HEADER_HEIGHT = 14;
	private static final int PADDING = 4;
	private static final int SCROLLBAR_WIDTH = 6;

	public static final class Row {
		public final Component name;
		public final Component value;
		public final ItemStack icon;
		public final List<Component> tooltip;
		public final int valueColor;
		public final Component subtitle;

		public Row(Component name, Component value, ItemStack icon) {
			this(name, value, icon, List.of(), 0, null);
		}

		public Row(Component name, Component value, ItemStack icon, List<Component> tooltip) {
			this(name, value, icon, tooltip, 0, null);
		}

		public Row(Component name, Component value, ItemStack icon, List<Component> tooltip, int valueColor) {
			this(name, value, icon, tooltip, valueColor, null);
		}

		public Row(Component name, Component value, ItemStack icon, List<Component> tooltip, int valueColor,
				Component subtitle) {
			this.name = name;
			this.value = value;
			this.icon = icon;
			this.tooltip = tooltip == null ? List.of() : tooltip;
			this.valueColor = valueColor;
			this.subtitle = subtitle;
		}

		private boolean hasSubtitle() {
			return this.subtitle != null && !this.subtitle.getString().isEmpty();
		}
	}

	private final List<Row> rows = new ArrayList<>();
	private Component headerLeft = Component.empty();
	private Component headerRight = Component.empty();
	private Component emptyText = Component.empty();
	private boolean tall;
	private int scroll;
	private int selected = -1;
	private IntConsumer onSelect;

	public StatsListWidget(int x, int y, int width, int height) {
		super(x, y, width, height, Component.empty());
	}

	private static boolean compact() {
		return StatsStore.settings() != null && StatsStore.settings().compactRows;
	}

	private int rowHeight() {
		if (compact()) {
			return COMPACT_ROW_HEIGHT;
		}
		return this.tall ? TALL_ROW_HEIGHT : ROW_HEIGHT;
	}

	public void setRows(List<Row> newRows) {
		this.rows.clear();
		this.rows.addAll(newRows);
		this.tall = false;
		for (Row row : newRows) {
			if (row.hasSubtitle()) {
				this.tall = true;
				break;
			}
		}
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
		return this.rows.size() * this.rowHeight();
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

		// Header sits on one line and is separated by a thin rule, like the
		// vanilla statistics list.
		graphics.drawString(font, this.headerLeft, left + PADDING, top + 3, Theme.header(), false);
		if (this.headerRight != null && !this.headerRight.getString().isEmpty()) {
			int width = font.width(this.headerRight);
			graphics.drawString(font, this.headerRight, right - PADDING - width, top + 3, Theme.header(), false);
		}
		graphics.fill(left, top + HEADER_HEIGHT - 1, right, top + HEADER_HEIGHT, Theme.divider());

		int listTop = top + HEADER_HEIGHT;
		int valueRight = right - PADDING - (this.maxScroll() > 0 ? SCROLLBAR_WIDTH : 0);
		graphics.enableScissor(left, listTop, right, bottom);

		Row hoveredRow = null;
		int rowHeight = this.rowHeight();
		boolean icons = StatsStore.settings() == null || StatsStore.settings().showIcons;
		boolean twoLines = this.tall && !compact();

		if (this.rows.isEmpty()) {
			graphics.drawCenteredString(font, this.emptyText, (left + right) / 2, listTop + 16, Theme.header());
		} else {
			for (int i = 0; i < this.rows.size(); i++) {
				int rowTop = listTop + i * rowHeight - this.scroll;
				if (rowTop + rowHeight <= listTop || rowTop >= bottom) {
					continue;
				}
				boolean hovered = mouseX >= left && mouseX < right && mouseY >= rowTop && mouseY < rowTop + rowHeight
					&& mouseY >= listTop && mouseY < bottom;
				if (i == this.selected) {
					graphics.fill(left + 1, rowTop, right - 1, rowTop + rowHeight - 1, Theme.selected());
				} else if (hovered) {
					graphics.fill(left + 1, rowTop, right - 1, rowTop + rowHeight - 1, Theme.hover());
				}

				Row row = this.rows.get(i);
				int textLeft = left + PADDING;
				if (icons && row.icon != null && !row.icon.isEmpty()) {
					graphics.renderItem(row.icon, left + PADDING, rowTop + (rowHeight - 16) / 2);
					textLeft += 20;
				}
				int valueColor = row.valueColor == 0 ? Theme.value() : row.valueColor;
				int valueWidth = font.width(row.value);

				if (twoLines && row.hasSubtitle()) {
					graphics.drawString(font, row.name, textLeft, rowTop + 4, Theme.text(), false);
					graphics.drawString(font, row.subtitle, textLeft, rowTop + 15, Theme.header(), false);
					graphics.drawString(font, row.value, valueRight - valueWidth, rowTop + 8, valueColor, false);
				} else {
					int textY = rowTop + (rowHeight - 8) / 2;
					graphics.drawString(font, row.name, textLeft, textY, Theme.text(), false);
					graphics.drawString(font, row.value, valueRight - valueWidth, textY, valueColor, false);
				}
				if (hovered) {
					hoveredRow = row;
				}
			}
		}

		graphics.disableScissor();

		if (this.maxScroll() > 0) {
			int trackTop = listTop + 1;
			int trackBottom = bottom - 1;
			int trackHeight = trackBottom - trackTop;
			int thumbHeight = Math.max(10, (int) (this.visibleHeight() / (float) this.contentHeight() * trackHeight));
			int thumbTop = trackTop + (int) (this.scroll / (float) this.maxScroll() * (trackHeight - thumbHeight));
			graphics.fill(right - SCROLLBAR_WIDTH, trackTop, right - SCROLLBAR_WIDTH + 2, trackBottom,
				Theme.vanilla() ? 0x30000000 : Theme.divider());
			graphics.fill(right - SCROLLBAR_WIDTH, thumbTop, right - SCROLLBAR_WIDTH + 2, thumbTop + thumbHeight,
				Theme.vanilla() ? 0x80808080 : Theme.divider());
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
		int index = (int) ((event.y() - listTop + this.scroll) / this.rowHeight());
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
		this.scroll = (int) Math.max(0, Math.min(this.maxScroll(), this.scroll - scrollY * this.rowHeight()));
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
