package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.MermaidTridentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The source doc's requested "+1 block reach" for the Living Coral Trident. Vanilla 1.20.1 has no
 * reach attribute at all (that was only added in Minecraft 1.21) — confirmed by decompiling
 * {@code MultiPlayerGameMode.getPickRange()} directly: {@code return
 * localPlayerMode.isCreative() ? 5.0F : 4.5F}, a plain hardcoded per-gamemode constant, not an
 * attribute lookup. This is the real client-side raycast range {@code GameRenderer#pick} uses to
 * build the player's hit result every frame, driving both block interaction and entity attacks —
 * confirmed via {@code ServerGamePacketListenerImpl}'s own {@code MAX_INTERACTION_DISTANCE}
 * (a loose 6-block anti-cheat sanity bound, not the real per-gamemode reach limit), so extending
 * this client-side value is what actually changes the player's felt reach, not a server-side
 * change.
 *
 * <p>Origins' own {@code extra_reach.json} power (checked directly via {@code gh api} before
 * choosing this approach) achieves its own reach bonus entirely through the third-party
 * Reach Entity Attributes mod's two attributes — a mod this project's build.gradle already
 * deliberately excludes as a transitive Apoli dependency, and one that isn't in the user's
 * installed mod list (see README). Rather than ask the player to install a whole new mod for one
 * weapon's minor stat, this mixin reproduces the same felt effect directly on vanilla's own
 * hardcoded constant, gated on the local player's mainhand holding the Living Coral Trident.
 *
 * <p><b>Creative mode completely bypasses this.</b> Confirmed by decompiling
 * {@code GameRenderer.pick()} directly: it reads {@code getPickRange()} into a local reach value,
 * but then checks {@code MultiPlayerGameMode.hasFarPickRange()} (itself just
 * {@code return localPlayerMode.isCreative();}) — if true, that reach value is unconditionally
 * overwritten to a flat {@code 6.0}, discarding whatever {@code getPickRange()} (and therefore
 * this mixin) returned entirely. In creative, base reach is already {@code 5.0}, so this mixin's
 * intended result (`5.0 + 1 = 6.0`) happens to numerically equal creative's own unrelated
 * hardcoded override — meaning the trident's bonus is real and correctly wired, but genuinely
 * imperceptible in creative mode specifically, by coincidence, not because it's broken. In
 * survival (where {@code hasFarPickRange()} is false), the extended value from this mixin is used
 * as-is: {@code 4.5 + 1 = 5.5}, a real, noticeable difference from vanilla's {@code 4.5}. If this
 * still doesn't feel extended in **survival**, that's a genuine bug worth digging into further —
 * the creative-mode case above is not that bug.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MermaidTridentReachMixin {
	private static final float REACH_BONUS = 1.0F;

	@Inject(method = "getPickRange", at = @At("RETURN"), cancellable = true)
	private void mermaidTrident$extendReach(CallbackInfoReturnable<Float> cir) {
		Player player = Minecraft.getInstance().player;
		if (player != null && player.getMainHandItem().getItem() instanceof MermaidTridentItem) {
			cir.setReturnValue(cir.getReturnValue() + REACH_BONUS);
		}
	}
}
