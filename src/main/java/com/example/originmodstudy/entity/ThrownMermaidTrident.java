package com.example.originmodstudy.entity;

import com.example.originmodstudy.item.ModItems;
import com.example.originmodstudy.mixin.ThrownTridentAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * The Living Coral Trident's own thrown entity — same reasoning and same
 * {@code ThrownTridentAccessor}-based construction as {@code ThrownJavelin} (see that class's own
 * doc for the full decompiled explanation of why {@code ThrownTrident}'s itemstack-carrying
 * constructor can't be reused, and why the private {@code tridentItem} field needs an accessor
 * mixin rather than a real synced-data field). Extending {@link ThrownTrident} directly (instead
 * of a lighter base like {@code ThrownSilkNet} does) keeps riptide/loyalty/pickup handling and
 * on-hit damage logic for free, including {@code ThrownTridentMixin}'s Mermaid-gated Barbed Tip/
 * Bleeding Current/Symbiosis additions, inherited automatically since this class never overrides
 * {@code onHitEntity}.
 *
 * <p><b>Bubble trail (new, not present on {@code ThrownJavelin}):</b> the source doc asked for a
 * trail of bubble particles while the Living Coral Trident flies through water. {@code tick()}
 * spawns a small burst of vanilla {@code ParticleTypes.BUBBLE} every tick this entity is in water,
 * via {@code ServerLevel#sendParticles} (the standard server-authoritative way to broadcast
 * particles to nearby clients, same as any vanilla server-side particle effect) rather than a
 * client-only spawn, so it's visible to every nearby player, not just the thrower.
 */
public class ThrownMermaidTrident extends ThrownTrident implements ItemSupplier {
	public ThrownMermaidTrident(EntityType<? extends ThrownMermaidTrident> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownMermaidTrident(Level level, LivingEntity owner, ItemStack itemStack) {
		this(ModEntities.THROWN_MERMAID_TRIDENT, level);
		this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
		this.setOwner(owner);
		ThrownTridentAccessor accessor = (ThrownTridentAccessor) this;
		accessor.arachne$setTridentItem(itemStack.copy());
		this.entityData.set(accessor.arachne$getIdLoyaltyKey(), (byte) EnchantmentHelper.getLoyalty(itemStack));
		this.entityData.set(accessor.arachne$getIdFoilKey(), itemStack.hasFoil());
	}

	/** In-flight/stuck render item only, same fix as {@code ThrownJavelin#getItem()} — the real
	 * {@code tridentItem} field (server-side hit/pickup logic) is untouched. */
	@Override
	public ItemStack getItem() {
		return new ItemStack(ModItems.MERMAID_TRIDENT);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel && this.isInWater()) {
			serverLevel.sendParticles(ParticleTypes.BUBBLE,
					this.getX(), this.getY(), this.getZ(), 2, 0.05, 0.05, 0.05, 0.01);
		}
	}
}
