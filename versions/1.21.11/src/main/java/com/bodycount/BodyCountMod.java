package com.bodycount;

import com.bodycount.data.StatsStore;
import net.fabricmc.api.ClientModInitializer;

public class BodyCountMod implements ClientModInitializer {
	public static final String MOD_ID = "bodycount";

	@Override
	public void onInitializeClient() {
		StatsStore.get().load();
		Runtime.getRuntime().addShutdownHook(new Thread(StatsStore.get()::saveIfDirty, "bodycount-save"));
	}
}