package com.playerstats.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface StatsScreenAccessor {

	@Invoker("addRenderableWidget")
	GuiEventListener psAddWidget(GuiEventListener widget);

	@Invoker("removeWidget")
	void psRemoveWidget(GuiEventListener widget);

	@Invoker("repositionElements")
	void psReposition();
}