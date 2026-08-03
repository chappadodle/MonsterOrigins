package com.example.originmodstudy.entity;

import com.example.originmodstudy.item.ModItems;
import com.example.originmodstudy.mixin.ThrownTridentAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Medusa's Petrifying Trident own thrown entity (fixing-doc pass) — fixes it always rendering as
 * a plain vanilla trident in flight and stuck in the ground, same root cause and same fix already
 * used for {@code ThrownJavelin}/{@code ThrownMermaidTrident}: {@code ThrownTrident}'s own
 * renderer hardcodes vanilla's trident texture/model regardless of the real carried item (see
 * CLAUDE.md's decompiled explanation), so a dedicated entity type + the shared
 * {@code DirectionalThrownItemRenderer} is needed to show this item's own (still flat, reskinned)
 * texture instead. No new 3D model involved — this only fixes the render pipeline, not the
 * model's own geometry.
 */
public class ThrownPetrifyingTrident extends ThrownTrident implements ItemSupplier {
	public ThrownPetrifyingTrident(EntityType<? extends ThrownPetrifyingTrident> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownPetrifyingTrident(Level level, LivingEntity owner, ItemStack itemStack) {
		this(ModEntities.THROWN_PETRIFYING_TRIDENT, level);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.setOwner(owner);
		ThrownTridentAccessor accessor = (ThrownTridentAccessor) this;
		accessor.arachne$setTridentItem(itemStack.copy());
		this.entityData.set(accessor.arachne$getIdLoyaltyKey(), (byte) EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOYALTY), itemStack));
		this.entityData.set(accessor.arachne$getIdFoilKey(), itemStack.hasFoil());
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(ModItems.PETRIFYING_TRIDENT);
	}
}
