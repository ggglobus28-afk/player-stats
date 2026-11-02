package com.bodycount.data;

/**
 * All user facing toggles. Stored inside bodycount.json so that a single
 * file holds both the statistics and the look and feel.
 */
public final class Settings {
	public boolean trackTotems = true;
	public boolean trackKills = true;
	public boolean chatKills = true;
	public boolean showIcons = true;
	public boolean compactRows = false;
	public boolean colorValues = true;
	public boolean hideEmpty = true;
	public boolean followServer = true;

	/** 0 = vanilla, 1 = dark, 2 = light, 3 = neon, 4 = amber. */
	public int theme = 0;

	/** 0 = activity, 1 = name, 2 = kills, 3 = totems. */
	public int sort = 0;

	/** Written by current builds; its absence marks a pre-1.2.1 theme value. */
	public int themeVersion = 1;

	public static final String[] THEME_KEYS = {
		"bodycount.theme.vanilla",
		"bodycount.theme.dark",
		"bodycount.theme.light",
		"bodycount.theme.neon",
		"bodycount.theme.amber"
	};

	public static final String[] SORT_KEYS = {
		"bodycount.sort.activity",
		"bodycount.sort.name",
		"bodycount.sort.kills",
		"bodycount.sort.totems"
	};

	public int theme() {
		return this.theme < 0 || this.theme >= THEME_KEYS.length ? 0 : this.theme;
	}

	public int sort() {
		return this.sort < 0 || this.sort >= SORT_KEYS.length ? 0 : this.sort;
	}

	public void nextTheme() {
		this.theme = (this.theme() + 1) % THEME_KEYS.length;
	}

	public void nextSort() {
		this.sort = (this.sort() + 1) % SORT_KEYS.length;
	}
}
