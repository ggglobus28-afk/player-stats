package com.bodycount.mixin;

import com.bodycount.data.ChatParser;
import com.bodycount.data.CombatTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reads the combat and chat packets the vanilla client already receives and
 * feeds them into {@link CombatTracker} and {@link ChatParser}.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "handleDamageEvent", at = @At("TAIL"))
	private void bodycount$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		Entity victim = level.getEntity(packet.entityId());
		if (!(victim instanceof Player)) {
			return;
		}
		Entity attacker = level.getEntity(packet.sourceCauseId());
		if (!(attacker instanceof Player)) {
			attacker = level.getEntity(packet.sourceDirectId());
		}
		if (attacker instanceof Player player && player != victim) {
			CombatTracker.onDamage(victim.getUUID(), player.getUUID());
		}
	}

	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void bodycount$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		Entity entity = packet.getEntity(level);
		if (!(entity instanceof Player)) {
			return;
		}
		byte event = packet.getEventId();
		if (event == EntityEvent.PROTECTED_FROM_DEATH) {
			CombatTracker.onTotem(entity.getUUID());
		} else if (event == EntityEvent.DEATH) {
			CombatTracker.onDeath(entity.getUUID());
		}
	}

	@Inject(method = "handlePlayerCombatKill", at = @At("TAIL"))
	private void bodycount$combatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		Entity entity = level.getEntity(packet.playerId());
		if (entity instanceof Player) {
			CombatTracker.onDeath(entity.getUUID());
		}
	}

	@Inject(method = "handleSystemChat", at = @At("TAIL"))
	private void bodycount$systemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (packet.overlay()) {
			return;
		}
		ChatParser.handle(packet.content().getString());
	}

	@Inject(method = "handleDisguisedChat", at = @At("TAIL"))
	private void bodycount$disguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
		ChatParser.handle(packet.message().getString());
	}
}
