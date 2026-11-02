package com.playerstats.data;

/**
 * All user facing toggles. Stored inside playerstats.json so that a single
 * file holds both the statistics and the look and feel.
 */
public final class Settings {
	public boolean trackTotems = true;
	public boolean trackKills = true;
	public boolean chatKills = true;
	public boolean chatTotems = true;
	public boolean showIcons = true;
	public boolean compactRows = false;
	public boolean colorValues = true;
	public boolean hideEmpty = true;
	public boolean followServer = true;

	/** 0 = dark, 1 = light, 2 = neon, 3 = amber. */
	public int theme = 0;

	/** 0 = activity, 1 = name, 2 = kills, 3 = totems. */
	public int sort = 0;

	public static final String[] THEME_KEYS = {
		"playerstats.theme.dark",
		"playerstats.theme.light",
		"playerstats.theme.neon",
		"playerstats.theme.amber"
	};

	public static final String[] SORT_KEYS = {
		"playerstats.sort.activity",
		"playerstats.sort.name",
		"playerstats.sort.kills",
		"playerstats.sort.totems"
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
