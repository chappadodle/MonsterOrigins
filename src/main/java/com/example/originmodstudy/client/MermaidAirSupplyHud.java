package com.example.originmodstudy.client;

import com.example.originmodstudy.util.OriginUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side HUD bar showing how much of Mermaid's out-of-water grace period is left before
 * {@code powers/mermaid/dehydration.json} (an {@code origins:damage_over_time} power) starts
 * dealing damage.
 *
 * <p>Checked for a built-in Origins/Apoli mechanism first, per this project's own convention
 * (the same way {@code merling.json} was checked before Mermaid's aquatic powers were designed):
 * Apoli's {@code InGameHudMixin}/{@code OverrideHudTexturePower} only let a power reskin
 * vanilla's own status bar sprites (hearts, food, armor, the vanilla air/bubble bar, etc.), they
 * do not add a new bar or expose one driven by a custom countdown. No such power exists.
 *
 * <p>The fallback the brief describes is a client-side {@link HudRenderCallback}, but reading
 * the actual server-tracked countdown turned out not to be possible: decompiling Apoli
 * {@code 2.11.11}'s real {@code DamageOverTimePower.java} shows its onset-delay counters
 * ({@code inDamageTicks}/{@code outOfDamageTicks}) are private fields with no getter, and
 * {@code PowerHolderComponent.syncPower(...)} — the call every other stateful power type in
 * Apoli's own source uses to push its state to the client (confirmed by grepping every call site
 * in the decompiled sources, e.g. {@code CooldownPower}, {@code TogglePower}) — is never called
 * for this power type. The real countdown genuinely is not readable from client code, and adding
 * a synced accessor would mean patching Apoli's own class or building new packet-syncing
 * infrastructure, a bigger architectural change than this task should make.
 *
 * <p>Instead of that, this mirrors {@code dehydration.json}'s own condition and timing
 * (onset_delay 6000 ticks, the 20-tick reset-grace window {@code DamageOverTimePower.resetDamage}
 * really uses) with an independent counter driven by the local player's own wet/dry state via
 * {@code Entity#isInWaterOrRain()} (confirmed via {@code javap} while building this HUD). Since
 * both this counter and the server's real one observe the exact same input (the local player's
 * own water/rain contact)
 * every tick, they track each other under normal play; the only place they can visibly differ is
 * the same 20-tick grace window either side is already willing to tolerate, and a resync (the
 * player rejoining, or switching origin) simply resets this counter to zero, erring toward
 * showing "safe" rather than a stale countdown. This is an approximation of the real timer, not
 * a synced mirror of it — accepted here since it needs no new networking and gets a real,
 * player-facing bar without one.
 */
public final class MermaidAirSupplyHud {
	private static final ResourceLocation MERMAID_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "mermaid");
	private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");

	/** Matches dehydration.json's onset_delay (6000 ticks, 5 minutes). */
	private static final int ONSET_DELAY_TICKS = 6000;
	/** Matches DamageOverTimePower.resetDamage's real reset-grace window (confirmed via
	 * decompile): the countdown only resets after 20 continuous ticks of being wet again, not
	 * the instant the player touches water. */
	private static final int GRACE_TICKS = 20;
	private static final int BUBBLE_COUNT = 10;

	private static int dryTicks = 0;
	private static int wetTicks = 0;

	private MermaidAirSupplyHud() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(MermaidAirSupplyHud::tick);
		HudRenderCallback.EVENT.register(MermaidAirSupplyHud::render);
	}

	private static void tick(Minecraft client) {
		Player player = client.player;
		if (player == null || !OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			dryTicks = 0;
			wetTicks = 0;
			return;
		}
		if (player.isInWaterOrRain()) {
			wetTicks++;
			if (wetTicks >= GRACE_TICKS) {
				dryTicks = 0;
			}
		} else {
			wetTicks = 0;
			if (dryTicks < ONSET_DELAY_TICKS) {
				dryTicks++;
			}
		}
	}

	// 1.21.1 port: HudRenderCallback#onHudRender's second parameter changed from a plain float
	// partial-tick to a DeltaTracker (confirmed via javap: real method is now
	// onHudRender(GuiGraphics, DeltaTracker)) — this HUD never actually used the interpolation
	// value (the bubble bar just draws a snapped integer count each frame), so the parameter is
	// accepted and otherwise ignored, same as before.
	private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		Player player = Minecraft.getInstance().player;
		if (player == null || dryTicks <= 0 || !OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			return;
		}
		int filledBubbles = BUBBLE_COUNT - Math.round((float) dryTicks / ONSET_DELAY_TICKS * BUBBLE_COUNT);
		filledBubbles = Math.max(0, Math.min(BUBBLE_COUNT, filledBubbles));
		int right = guiGraphics.guiWidth() / 2 + 91;
		int top = guiGraphics.guiHeight() - 49;
		for (int i = 0; i < BUBBLE_COUNT; i++) {
			int x = right - i * 8 - 9;
			if (i < filledBubbles) {
				guiGraphics.blit(ICONS_TEXTURE, x, top, 16, 18, 9, 9);
			} else {
				guiGraphics.blit(ICONS_TEXTURE, x, top, 25, 18, 9, 9);
			}
		}
	}
}
