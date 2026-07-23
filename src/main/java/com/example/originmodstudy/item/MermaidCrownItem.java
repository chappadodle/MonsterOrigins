package com.example.originmodstudy.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Mermaid's exclusive crown: +2 hearts while worn (a real attribute modifier, same
 * {@code getDefaultAttributeModifiers} technique {@code HarpyJavelinItem} uses for its own stats,
 * just the HEAD slot instead of MAINHAND) plus continuous Regeneration while actually equipped —
 * for that part, {@code inventoryTick} (confirmed via {@code javap}: {@code inventoryTick
 * (ItemStack, Level, Entity, int, boolean)}) is the standard vanilla per-tick hook for "this stack
 * exists somewhere in a living entity's inventory," so it's guarded by an explicit
 * "is this stack actually the one in my head slot right now" check rather than firing for a
 * crown just sitting in a chest or the player's general inventory.
 */
public class MermaidCrownItem extends ArmorItem {
	private static final UUID CROWN_HEALTH_MODIFIER_ID = UUID.fromString("6b4f6a3a-6e7e-4b8a-9f7a-3c1f0a9e2d41");
	private final Multimap<Attribute, AttributeModifier> defaultModifiers;

	public MermaidCrownItem(ArmorMaterial material, Type type, Properties properties) {
		super(material, type, properties);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.putAll(super.getDefaultAttributeModifiers(EquipmentSlot.HEAD));
		builder.put(Attributes.MAX_HEALTH, new AttributeModifier(
				CROWN_HEALTH_MODIFIER_ID, "Mermaid's Crown health bonus", 4.0, AttributeModifier.Operation.ADDITION));
		this.defaultModifiers = builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		if (slot == EquipmentSlot.HEAD) {
			return this.defaultModifiers;
		}
		return super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (level.isClientSide || !(entity instanceof LivingEntity livingEntity)) {
			return;
		}
		if (livingEntity.getItemBySlot(EquipmentSlot.HEAD) != stack) {
			return;
		}
		if (level.getGameTime() % 40 == 0) {
			livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
		}
	}
}
