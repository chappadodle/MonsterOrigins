package com.example.originmodstudy.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes ThrownTrident's private {@code tridentItem} field so {@code ThrownJavelin} (a
 * ThrownTrident subclass with its own EntityType) can set/read the itemstack it's carrying
 * without going through the itemstack constructor, which hardcodes {@code EntityType.TRIDENT}.
 *
 * <p>Also exposes the private static {@code ID_LOYALTY}/{@code ID_FOIL} synced-data keys
 * (confirmed via decompile: the real itemstack-carrying constructor is the only place vanilla
 * ever populates these, from {@code EnchantmentHelper.getLoyalty}/{@code itemStack.hasFoil()} —
 * since our subclasses can't call that constructor either, per the {@code tridentItem} doc above,
 * they never got populated, silently leaving Loyalty non-functional on every modded trident-style
 * weapon). Mixin accessors work the same way for a static field as an instance one — the
 * generated implementation ignores the receiver and reads/writes the class's own static slot.
 */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
	@Accessor("tridentItem")
	ItemStack arachne$getTridentItem();

	@Accessor("tridentItem")
	void arachne$setTridentItem(ItemStack stack);

	@Accessor("ID_LOYALTY")
	EntityDataAccessor<Byte> arachne$getIdLoyaltyKey();

	@Accessor("ID_FOIL")
	EntityDataAccessor<Boolean> arachne$getIdFoilKey();
}
