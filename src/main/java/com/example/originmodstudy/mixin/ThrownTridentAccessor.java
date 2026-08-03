package com.example.originmodstudy.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ThrownTrident}'s private static {@code ID_LOYALTY}/{@code ID_FOIL} synced-data
 * keys (confirmed via decompile: the real itemstack-carrying constructor is the only place
 * vanilla ever populates these, from {@code EnchantmentHelper.getItemEnchantmentLevel(
 * Holder<Enchantment>, ItemStack)} (resolved via the registry-holder pattern established
 * elsewhere in this port, not the old, now-removed {@code EnchantmentHelper.getLoyalty})/
 * {@code itemStack.hasFoil()} — since our subclasses can't call that constructor either (it
 * hardcodes {@code EntityType.TRIDENT}), they never got populated, silently leaving Loyalty
 * non-functional on every modded trident-style weapon). Mixin accessors work the same way for a
 * static field as an instance one — the generated implementation ignores the receiver and
 * reads/writes the class's own static slot.
 *
 * <p><b>1.21.1 update:</b> {@code ThrownTrident}'s own private {@code tridentItem} field (the
 * thing this accessor used to also expose) no longer exists at all — confirmed via {@code javap}
 * that the itemstack this thrown entity carries moved up to {@code AbstractArrow}'s own private
 * {@code pickupItemStack} field, which already has a real {@code protected
 * setPickupItemStack(ItemStack)} setter every subclass can call directly with no mixin needed
 * (see {@code ThrownJavelin}/{@code ThrownMermaidTrident}/{@code ThrownPetrifyingTrident}, which
 * now call {@code this.setPickupItemStack(...)} instead of going through this accessor). Trying
 * to keep a {@code tridentItem} accessor on this interface after the field was removed is not a
 * compile error (Mixin annotations aren't checked by javac) — it's a launch-time
 * {@code InvalidMixinException} ("was not located in the target class"), exactly the failure mode
 * this project's own CLAUDE.md already warns mixin work is prone to.
 */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
	@Accessor("ID_LOYALTY")
	EntityDataAccessor<Byte> arachne$getIdLoyaltyKey();

	@Accessor("ID_FOIL")
	EntityDataAccessor<Boolean> arachne$getIdFoilKey();
}
