package com.example.originmodstudy.mixin;

import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes ThrownTrident's private {@code tridentItem} field so {@code ThrownJavelin} (a
 * ThrownTrident subclass with its own EntityType) can set/read the itemstack it's carrying
 * without going through the itemstack constructor, which hardcodes {@code EntityType.TRIDENT}.
 */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
	@Accessor("tridentItem")
	ItemStack arachne$getTridentItem();

	@Accessor("tridentItem")
	void arachne$setTridentItem(ItemStack stack);
}
