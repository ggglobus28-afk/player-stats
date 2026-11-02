package com.terraformersmc.modmenu.api;

import java.util.Collections;
import java.util.Map;

/**
 * Compile-time stub of the Mod Menu API. The real interface is provided by the
 * Mod Menu mod at runtime; this class is excluded from the built jar.
 */
public interface ModMenuApi {
	default ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> null;
	}

	default Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		return Collections.emptyMap();
	}
}
