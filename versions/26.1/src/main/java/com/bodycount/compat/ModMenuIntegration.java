package com.bodycount.compat;

import com.bodycount.ui.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional Mod Menu integration. The entrypoint is only instantiated when the
 * Mod Menu mod is installed, so BodyCount keeps working without it.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new SettingsScreen(parent, null);
	}
}
