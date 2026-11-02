package com.playerstats.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers who last damaged whom, so a totem pop or a death can be credited
 * to the right player. Only events that reach the client are used.
 */
public final class CombatTracker {
	private static final long TOTEM_WINDOW_MS = 5_000L;
	private static final long KILL_WINDOW_MS = 20_000L;
	private static final long DEATH_DEBOUNCE_MS = 3_000L;
	private static final Map<UUID, Hit> LAST_HIT = new HashMap<>();
	private static final Map<UUID, Long> LAST_DEATH = new HashMap<>();

	private CombatTracker() {
	}

	private record Hit(UUID attacker, long time) {
	}

	public static void onDamage(UUID victim, UUID attacker) {
		if (victim == null || attacker == null || victim.equals(attacker)) {
			return;
		}
		LAST_HIT.put(victim, new Hit(attacker, System.currentTimeMillis()));
	}

	public static void onTotem(UUID victim) {
		Hit hit = recent(victim, TOTEM_WINDOW_MS);
		if (hit == null) {
			return;
		}
		StatsStore.get().record(hit.attacker(), victim, StatsStore.Kind.TOTEM);
	}

	public static void onDeath(UUID victim) {
		if (victim == null) {
			return;
		}
		long now = System.currentTimeMillis();
		Long previous = LAST_DEATH.put(victim, now);
		if (previous != null && now - previous < DEATH_DEBOUNCE_MS) {
			// Both the entity event and the combat-kill packet can report the
			// same death; only count it once.
			return;
		}
		Hit hit = LAST_HIT.remove(victim);
		if (hit == null || now - hit.time() > KILL_WINDOW_MS) {
			return;
		}
		StatsStore.get().record(hit.attacker(), victim, StatsStore.Kind.KILL);
	}

	private static Hit recent(UUID victim, long windowMs) {
		Hit hit = LAST_HIT.get(victim);
		if (hit == null) {
			return null;
		}
		if (System.currentTimeMillis() - hit.time() > windowMs) {
			LAST_HIT.remove(victim);
			return null;
		}
		return hit;
	}
}