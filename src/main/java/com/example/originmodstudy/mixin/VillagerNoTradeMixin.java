package com.example.originmodstudy.mixin;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Villagers refuse to trade with Medusa, unconditionally — decompiled {@code Villager
 * .mobInteract} directly to confirm it's a single method that both opens the trade screen and
 * returns the interaction result, so a HEAD injection cancelling with {@code InteractionResult
 * .sidedSuccess(level.isClientSide)} (mirroring the exact return vanilla itself uses for
 * "interaction handled, no further vanilla logic should run" elsewhere in the same method) is
 * sufficient to block {@code startTrading} from ever being reached, without needing to touch that
 * private method directly. Also plays {@code SoundEvents.VILLAGER_NO} (vanilla's own "I don't
 * want to do that" sound) so the rejection reads as a deliberate refusal, not a silent no-op.
 *
 * <p>Previously gated behind Medusa's since-removed Innocent Form toggle (a disguise mechanic the
 * user called "quite useless and was a bad idea"); now applies to any Medusa-origin player at all
 * times, alongside the always-on iron-golem hostility in {@code MedusaFearHooks}.
 */
@Mixin(Villager.class)
public abstract class VillagerNoTradeMixin {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = new ResourceLocation("monster_origins", "medusa");

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void medusaFear$refuseTrade(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!OriginUtil.hasOrigin(player, MEDUSA_ORIGIN_ID)) {
			return;
		}
		Villager villager = (Villager) (Object) this;
		if (!villager.level().isClientSide) {
			villager.level().playSound(null, villager, SoundEvents.VILLAGER_NO, villager.getSoundSource(), 1.0F, 1.0F);
		}
		cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide));
	}
}
