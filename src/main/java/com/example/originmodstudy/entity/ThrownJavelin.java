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
 * carried item via {@code AbstractArrow}'s own protected {@code setPickupItemStack(ItemStack)} —
 * the real field this ends up in ({@code pickupItemStack}, confirmed via decompile) moved up from
 * {@code ThrownTrident}'s own now-removed private {@code tridentItem} field in the 1.21.1 port; no
 * mixin/accessor is needed for it anymore since the setter is already protected and this class is
 * a real subclass. {@link ThrownTridentAccessor} is still needed, just narrower now — only for the
 * {@code ID_LOYALTY}/{@code ID_FOIL} synced-data keys below. Loyalty/foil enchantment display is not
 * replicated (those fields stay at their {@code defineSynchedData} defaults) — an accepted, minor
 * simplification, not something a thrown javelin is expected to need.
 *
 * <p><b>Stuck-in-ground render bug (fixed):</b> {@code AbstractArrow}'s private {@code
 * pickupItemStack} field (formerly {@code ThrownTrident}'s own {@code tridentItem} pre-1.21.1) is
 * never added to {@code SynchedEntityData} — confirmed by decompiling directly:
 * {@code defineSynchedData()} only defines the loyalty/foil accessors, and {@code
 * pickupItemStack} is otherwise only set in the throw-time constructor (never called for a
 * client-reconstructed entity) and touched by NBT save/load only, which governs world
 * persistence, not the initial spawn-packet sync the client actually uses to build its own copy
 * of this entity. The client-side entity therefore keeps {@code pickupItemStack}'s hardcoded
 * default ({@code new ItemStack(Items.TRIDENT)}) for its entire client-side lifetime, so {@code
 * getItem()} — inherited unmodified from {@code ThrownTrident} before this fix — rendered a plain
 * vanilla trident, most visibly once the javelin stuck in the ground and stopped moving. {@code
 * getItem()} is overridden below to unconditionally return a fresh javelin stack for rendering
 * purposes only; the real {@code pickupItemStack} field (server-side hit/pickup logic) is left
 * completely untouched, same pattern as the Silk Net Shooter's own in-flight render fix (see
 * {@code entity/ThrownSilkNet.java}).
 *
 * <p><b>Storm Javelin</b> (lightning + AOE damage on any thrown hit) no longer needs any
 * throw-time data captured on this entity at all — earlier versions tracked the thrower's fall
 * distance and/or absolute Y level as trigger conditions; both were dropped per the user's own
 * successive simplifications, down to just a per-player cooldown checked entirely within {@code
 * ThrownTridentMixin} itself.
 */
public class ThrownJavelin extends ThrownTrident implements ItemSupplier {

	public ThrownJavelin(EntityType<? extends ThrownJavelin> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownJavelin(Level level, LivingEntity owner, ItemStack itemStack) {
		this(ModEntities.THROWN_HARPY_JAVELIN, level);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.setOwner(owner);
		this.setPickupItemStack(itemStack.copy());
		ThrownTridentAccessor accessor = (ThrownTridentAccessor) this;
		this.entityData.set(accessor.arachne$getIdLoyaltyKey(), (byte) EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOYALTY), itemStack));
		this.entityData.set(accessor.arachne$getIdFoilKey(), itemStack.hasFoil());
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(ModItems.STORM_TRIDENT);
	}
}
