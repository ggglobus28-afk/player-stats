package com.playerstats.ui;

import com.playerstats.data.Settings;
import com.playerstats.data.StatsStore;

/**
 * Colour palette used by the custom widgets. Four built in themes keep the tab
 * readable on top of the vanilla background.
 */
public final class Theme {
	private static final int[] BACKGROUND = {0x40000000, 0x66FFFFFF, 0x55001020, 0x66000000};
	private static final int[] HEADER = {0xFFAAAAAA, 0xFF404040, 0xFF00FFC8, 0xFFFFC400};
	private static final int[] TEXT = {0xFFFFFFFF, 0xFF101010, 0xFFE0FFFF, 0xFFFFFFFF};
	private static final int[] VALUE = {0xFFE0E0E0, 0xFF303030, 0xFF00FFC8, 0xFFFFD54F};
	private static final int[] SELECTED = {0x5000C8FF, 0x500078D7, 0x7000FF88, 0x70FF9800};
	private static final int[] HOVER = {0x30FFFFFF, 0x20000000, 0x3000FF88, 0x30FFC400};
	private static final int[] DIVIDER = {0x60FFFFFF, 0x40000000, 0x6000FFC8, 0x60FFC400};
	private static final int[] ACCENT = {0xFF00C8FF, 0xFF0078D7, 0xFF00FF88, 0xFFFFC400};
	private static final int[] GOOD = {0xFF66FF88, 0xFF2E7D32, 0xFF00FF88, 0xFFFFD54F};
	private static final int[] BAD = {0xFFFF6666, 0xFFC62828, 0xFFFF4466, 0xFFFF7043};
	private static final int[] TOOLTIP_BG = {0xF0101010, 0xF0F5F5F5, 0xF0000C12, 0xF0140E00};
	private static final int[] TOOLTIP_TEXT = {0xFFFFFFFF, 0xFF101010, 0xFFE0FFFF, 0xFFFFF3D0};

	private Theme() {
	}

	private static int index() {
		Settings settings = StatsStore.settings();
		return settings == null ? 0 : settings.theme();
	}

	private static int pick(int[] palette) {
		int index = index();
		return index < 0 || index >= palette.length ? palette[0] : palette[index];
	}

	public static int background() {
		return pick(BACKGROUND);
	}

	public static int header() {
		return pick(HEADER);
	}

	public static int text() {
		return pick(TEXT);
	}

	public static int value() {
		return pick(VALUE);
	}

	public static int selected() {
		return pick(SELECTED);
	}

	public static int hover() {
		return pick(HOVER);
	}

	public static int divider() {
		return pick(DIVIDER);
	}

	public static int accent() {
		return pick(ACCENT);
	}

	public static int good() {
		return pick(GOOD);
	}

	public static int bad() {
		return pick(BAD);
	}

	public static int tooltipBackground() {
		return pick(TOOLTIP_BG);
	}

	public static int tooltipText() {
		return pick(TOOLTIP_TEXT);
	}
}
