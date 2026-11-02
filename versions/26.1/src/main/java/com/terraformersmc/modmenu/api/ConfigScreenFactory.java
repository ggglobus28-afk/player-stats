package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * Compile-time stub of the Mod Menu API. The real interface is provided by the
 * Mod Menu mod at runtime; this class is excluded from the built jar.
 */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
	S create(Screen parent);
}
