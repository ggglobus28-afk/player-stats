package com.bodycount.ui;

import com.bodycount.data.Settings;
import com.bodycount.data.StatsStore;

/**
 * Colour palette used by the custom widgets. Index 0 is the vanilla style used
 * by the statistics screen; the other four themes restyle the mod's widgets,
 * including its buttons.
 */
public final class Theme {
	// 0 = vanilla, 1 = dark, 2 = light, 3 = neon, 4 = amber.
	private static final int[] BACKGROUND = {0x00000000, 0x40000000, 0x66FFFFFF, 0x55001020, 0x66000000};
	private static final int[] HEADER = {0xFFA0A0A0, 0xFFAAAAAA, 0xFF404040, 0xFF00FFC8, 0xFFFFC400};
	private static final int[] TEXT = {0xFFFFFFFF, 0xFFFFFFFF, 0xFF101010, 0xFFE0FFFF, 0xFFFFFFFF};
	private static final int[] VALUE = {0xFFFFFFFF, 0xFFE0E0E0, 0xFF303030, 0xFF00FFC8, 0xFFFFD54F};
	private static final int[] SELECTED = {0x30FFFFFF, 0x5000C8FF, 0x500078D7, 0x7000FF88, 0x70FF9800};
	private static final int[] HOVER = {0x20FFFFFF, 0x30FFFFFF, 0x20000000, 0x3000FF88, 0x30FFC400};
	private static final int[] DIVIDER = {0x30808080, 0x60FFFFFF, 0x40000000, 0x6000FFC8, 0x60FFC400};
	private static final int[] GOOD = {0xFFFFFFFF, 0xFF66FF88, 0xFF2E7D32, 0xFF00FF88, 0xFFFFD54F};
	private static final int[] BAD = {0xFFFFFFFF, 0xFFFF6666, 0xFFC62828, 0xFFFF4466, 0xFFFF7043};
	private static final int[] TOOLTIP_BG = {0xF0100010, 0xF0101010, 0xF0F5F5F5, 0xF0000C12, 0xF0140E00};
	private static final int[] TOOLTIP_TEXT = {0xFFFFFFFF, 0xFFFFFFFF, 0xFF101010, 0xFFE0FFFF, 0xFFFFF3D0};

	private static final int[] BUTTON = {0xFF000000, 0xFF2E3440, 0xFFE8E8E8, 0xFF001A14, 0xFF241A00};
	private static final int[] BUTTON_HOVER = {0xFF000000, 0xFF3B4353, 0xFFD8D8D8, 0xFF003026, 0xFF3A2A00};
	private static final int[] BUTTON_DISABLED = {0xFF000000, 0xFF242A33, 0xFFDDDDDD, 0xFF00100C, 0xFF1A1200};
	private static final int[] BUTTON_BORDER = {0xFF000000, 0xFF00C8FF, 0xFF0078D7, 0xFF00FF88, 0xFFFFC400};
	private static final int[] BUTTON_TEXT = {0xFFFFFFFF, 0xFFECEFF4, 0xFF101010, 0xFFE0FFFF, 0xFFFFF3D0};
	private static final int[] BUTTON_TEXT_DISABLED = {0xFFA0A0A0, 0xFF777777, 0xFF888888, 0xFF4A7A6A, 0xFF8A7A50};

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

	/** True for the default style, which mirrors the vanilla statistics screen. */
	public static boolean vanilla() {
		return index() == 0;
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

	public static int button() {
		return pick(BUTTON);
	}

	public static int buttonHover() {
		return pick(BUTTON_HOVER);
	}

	public static int buttonDisabled() {
		return pick(BUTTON_DISABLED);
	}

	public static int buttonBorder() {
		return pick(BUTTON_BORDER);
	}

	public static int buttonText() {
		return pick(BUTTON_TEXT);
	}

	public static int buttonTextDisabled() {
		return pick(BUTTON_TEXT_DISABLED);
	}
}
