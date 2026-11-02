package com.playerstats.mixin;

import com.playerstats.ui.PlayerStatsTab;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the "Players" tab to the vanilla statistics screen. Vanilla rebuilds the
 * tab bar once the stats packet arrives; this hook runs right after that and
 * rebuilds it with the four tabs.
 */
@Mixin(StatsScreen.class)
public class StatsScreenMixin {

	@Shadow
	private TabNavigationBar tabNavigationBar;

	@Shadow
	private TabManager tabManager;

	@Inject(method = "onStatsUpdated", at = @At("TAIL"))
	private void playerstats$addPlayerTab(CallbackInfo ci) {
		TabNavigationBar oldBar = this.tabNavigationBar;
		if (oldBar == null) {
			return;
		}

		List<Tab> tabs = oldBar.getTabs();
		for (Tab tab : tabs) {
			if (tab instanceof PlayerStatsTab) {
				return;
			}
		}

		StatsScreen screen = (StatsScreen) (Object) this;
		List<Tab> allTabs = new ArrayList<>(tabs);
		allTabs.add(new PlayerStatsTab(screen));

		TabNavigationBar newBar = TabNavigationBar.builder(this.tabManager, screen.width)
			.addTabs(allTabs.toArray(new Tab[0]))
			.build();

		StatsScreenAccessor accessor = (StatsScreenAccessor) (Object) this;
		accessor.psRemoveWidget(oldBar);
		this.tabNavigationBar = newBar;
		accessor.psAddWidget(newBar);
		screen.setFocused(newBar);
		accessor.psReposition();
	}
}