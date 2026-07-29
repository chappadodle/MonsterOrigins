package com.example.originmodstudy.loot;

import com.example.originmodstudy.util.OriginUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * <p><b>Bare-hand coral mining:</b> vanilla's own coral block loot tables (confirmed directly from
 * the vanilla data bundled in the game jar, e.g. {@code data/minecraft/loot_tables/blocks/
 * tube_coral_block.json}) drop the live block only with Silk Touch, otherwise its dead variant.
 * Rather than fight the loot table's own {@code alternatives} structure (which would need a new
 * registered {@code LootItemCondition} type just to check the breaking player's origin at roll
 * time), this listens on {@link PlayerBlockBreakEvents#AFTER} — by which point vanilla has already
 * spawned the dead coral's item entity — and swaps that already-spawned stack for the live block's
 * item, for a Mermaid player mining bare-handed (or with any non-Silk-Touch tool). A simplification
 * worth naming honestly: it looks for a matching dead-coral item entity within a small radius of
 * the break, which is correct for the overwhelmingly common single-block-break case this feature
 * is meant for, but could misfire if another dead coral of the exact same kind happens to be
 * dropped at the same instant right next to it.
 *
 * <p><b>Mermaid-only 5% prismarine shard fish drop:</b> {@link ServerLivingEntityEvents#AFTER_DEATH}
 * fires once a fish's death is final, well after loot resolution — checked directly against the
 * four vanilla fish entity types (1.20.1 has no {@code EntityTypeTags.FISH} tag, confirmed absent
 * via {@code javap} on {@code EntityTypeTags} — that tag doesn't exist until a later game version)
 * rather than a tag lookup.
 */
public final class MermaidLootEvents {
	private static final ResourceLocation MERMAID_ORIGIN_ID = new ResourceLocation("monster_origins", "mermaid");
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
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
			// Vanilla already dropped the live block correctly in this case, nothing to fix.
			return;
		}

		Block liveCoralBlock = state.getBlock();
		ResourceLocation liveId = BuiltInRegistries.BLOCK.getKey(liveCoralBlock);
		ResourceLocation deadId = new ResourceLocation(liveId.getNamespace(), "dead_" + liveId.getPath());
		Block deadCoralBlock = BuiltInRegistries.BLOCK.get(deadId);
		if (deadCoralBlock == net.minecraft.world.level.block.Blocks.AIR) {
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
