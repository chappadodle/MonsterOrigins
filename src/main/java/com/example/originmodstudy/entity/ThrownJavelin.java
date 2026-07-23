package com.example.originmodstudy.entity;

import com.example.originmodstudy.mixin.ThrownTridentAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A dedicated entity type for a thrown Harpy Javelin, existing purely so its client renderer can
 * show the javelin's own item model in flight instead of {@code ThrownTridentRenderer}'s
 * hardcoded vanilla {@code TridentModel} + {@code textures/entity/trident.png} (confirmed by
 * decompiling that class — see CLAUDE.md). Extends {@link ThrownTrident} directly to reuse its
 * real throwing physics, loyalty/pickup handling, and on-hit damage logic unchanged (including
 * {@code ThrownTridentMixin}'s Harpy-origin bonus, which is inherited automatically since this
 * class doesn't override {@code onHitEntity}) — only the entity type and rendering differ.
 *
 * <p>{@code ThrownTrident}'s own itemstack-carrying constructor hardcodes {@code EntityType
 * .TRIDENT}, so it can't be reused here. This instead calls the (EntityType, Level) constructor
 * and replicates the position/owner setup {@code AbstractArrow}'s own (EntityType, LivingEntity,
 * Level) constructor does (read directly from decompiled source, not guessed), then sets the
 * private {@code tridentItem} field via {@link ThrownTridentAccessor} since nothing else can
 * reach it from outside {@code ThrownTrident} itself. Loyalty/foil enchantment display is not
 * replicated (those fields stay at their {@code defineSynchedData} defaults) — an accepted, minor
 * simplification, not something a thrown javelin is expected to need.
 */
public class ThrownJavelin extends ThrownTrident implements ItemSupplier {
	public ThrownJavelin(EntityType<? extends ThrownJavelin> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownJavelin(Level level, LivingEntity owner, ItemStack itemStack) {
		this(ModEntities.THROWN_HARPY_JAVELIN, level);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.setOwner(owner);
		((ThrownTridentAccessor) this).arachne$setTridentItem(itemStack.copy());
	}

	@Override
	public ItemStack getItem() {
		return ((ThrownTridentAccessor) this).arachne$getTridentItem();
	}
}
