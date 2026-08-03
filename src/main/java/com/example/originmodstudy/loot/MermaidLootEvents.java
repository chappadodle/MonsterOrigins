package com.example.originmodstudy.loot;

import com.example.originmodstudy.util.OriginUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;

import java.util.List;

/**
 * The Living Coral Trident's two loot-related traits (Task 13) — real Java event hooks rather
 * than data, since neither has a data-driven path: Origins/Apoli powers can't touch loot tables
 * or block-break resolution at all.
 *
 * <p><b>Bare-hand coral mining (playtest fix, 2026-08-02):</b> vanilla coral blocks are declared
 * with {@code requiresCorrectToolForDrops()} (confirmed via {@code javap} on {@code Blocks.class}
 * in this project's own mapped jar) and are only tagged {@code minecraft:mineable/pickaxe} — so
 * {@code ServerPlayerGameMode#canHarvestBlock} (real gate: {@code state.requiresCorrectToolForDrops()
 * && itemStack.isCorrectToolForDrops(state)}) is {@code true} for any pickaxe, but {@code false} for
 * a bare hand. When it's {@code true}, vanilla runs its own loot table normally (Silk Touch → live
 * block, otherwise dead coral) and a real dead-coral {@link ItemEntity} gets spawned; this is the
 * case the original swap-based fix below handles, by finding that already-spawned drop and
 * swapping its stack for the live block's item — a simplification worth naming honestly: it looks
 * for a matching dead-coral item entity within a small radius of the break, correct for the
 * overwhelmingly common single-block-break case this feature is meant for, but could misfire if
 * another dead coral of the exact same kind happens to be dropped at the same instant right next to
 * it. When {@code isCorrectToolForDrops} is {@code false} (bare hand, or any other non-pickaxe
 * tool), vanilla skips loot resolution entirely — the block still disappears from the world, but
 * nothing drops, so there is nothing for the swap loop to find. That branch instead constructs the
 * live coral block's item directly and spawns a fresh {@link ItemEntity} at the break position,
 * since a Mermaid mining bare-handed is exactly the case this whole feature exists for (her own
 * trident recipe needs a live coral block, and bare hand is the tool she has no other way to reach
 * one with).
 *
 * <p><b>Mermaid-only 5% prismarine shard fish drop:</b> {@link ServerLivingEntityEvents#AFTER_DEATH}
 * fires once a fish's death is final, well after loot resolution — checked directly against the
 * four vanilla fish entity types (1.21.1 still has no {@code EntityTypeTags.FISH} tag, confirmed
 * absent via {@code javap} on {@code EntityTypeTags} — re-checked against the real 1.21.1 mapped
 * jar during the port, not just carried over from the 1.20.1-era claim; that tag doesn't exist
 * until a later game version)
 * rather than a tag lookup.
 */
public final class MermaidLootEvents {
	private static final ResourceLocation MERMAID_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "mermaid");
	private static final float FISH_SHARD_DROP_CHANCE = 0.05F;
	private static final double CORAL_SWAP_SEARCH_RADIUS = 1.5;

	private MermaidLootEvents() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(MermaidLootEvents::onCoralBroken);
		ServerLivingEntityEvents.AFTER_DEATH.register(MermaidLootEvents::onLivingEntityDeath);
	}

	private static void onCoralBroken(net.minecraft.world.level.Level world, Player player, BlockPos pos,
			BlockState state, BlockEntity blockEntity) {
		if (world.isClientSide || !state.is(BlockTags.CORAL_BLOCKS)) {
			return;
		}
		if (!OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		if (EnchantmentHelper.getItemEnchantmentLevel(
				world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
			// Vanilla already dropped the live block correctly in this case, nothing to fix.
			return;
		}

		Block liveCoralBlock = state.getBlock();
		ResourceLocation liveId = BuiltInRegistries.BLOCK.getKey(liveCoralBlock);
		ResourceLocation deadId = ResourceLocation.fromNamespaceAndPath(liveId.getNamespace(), "dead_" + liveId.getPath());
		Block deadCoralBlock = BuiltInRegistries.BLOCK.get(deadId);
		if (deadCoralBlock == net.minecraft.world.level.block.Blocks.AIR) {
			return;
		}

		if (!tool.isCorrectToolForDrops(state)) {
			// Same real gate vanilla's own canHarvestBlock uses: without a correct tool, vanilla
			// skipped loot resolution entirely, so there's no dead-coral drop to swap. Spawn the
			// live block's item directly instead.
			ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					new ItemStack(liveCoralBlock.asItem()));
			world.addFreshEntity(drop);
			return;
		}

		AABB searchArea = new AABB(pos).inflate(CORAL_SWAP_SEARCH_RADIUS);
		List<ItemEntity> nearbyDrops = world.getEntitiesOfClass(ItemEntity.class, searchArea,
				drop -> drop.getItem().getItem() == deadCoralBlock.asItem());
		for (ItemEntity drop : nearbyDrops) {
			drop.setItem(new ItemStack(liveCoralBlock.asItem(), drop.getItem().getCount()));
		}
	}

	private static void onLivingEntityDeath(LivingEntity entity, DamageSource damageSource) {
		if (entity.level().isClientSide || !isFish(entity.getType())) {
			return;
		}
		Entity killer = damageSource.getEntity();
		if (!(killer instanceof Player player) || !OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			return;
		}
		if (entity.level().getRandom().nextFloat() >= FISH_SHARD_DROP_CHANCE) {
			return;
		}
		ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
				new ItemStack(Items.PRISMARINE_SHARD));
		entity.level().addFreshEntity(drop);
	}

	private static boolean isFish(EntityType<?> type) {
		return type == EntityType.COD || type == EntityType.SALMON
				|| type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH;
	}
}
