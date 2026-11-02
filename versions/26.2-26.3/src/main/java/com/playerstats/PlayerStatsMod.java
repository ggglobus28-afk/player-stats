package com.playerstats;

import com.playerstats.data.StatsStore;
import net.fabricmc.api.ClientModInitializer;

public class PlayerStatsMod implements ClientModInitializer {
	public static final String MOD_ID = "playerstats";

	@Override
	public void onInitializeClient() {
		StatsStore.get().load();
		Runtime.getRuntime().addShutdownHook(new Thread(StatsStore.get()::saveIfDirty, "playerstats-save"));
	}
}