package com.example.originmodstudy.item;

import com.example.originmodstudy.util.OriginUtil;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * Medusa's Serpent Aegis (Task 19): an off-hand defensive item extending vanilla
 * {@link ShieldItem} directly, inheriting its real raise/block/disable mechanics unchanged
 * (confirmed via {@code javap} that {@code ShieldItem} exposes nothing that needs overriding for
 * this — no custom block-detection path was worth building from scratch) — except {@link #use}
 * below, which is a real, mechanical gate, not just a tooltip claim: playtest feedback was that
 * "Medusa only" (the tooltip line) wasn't actually true, any origin that isn't blocked by
 * {@code origins:no_shield} could raise and block with it same as her. Anyone can still craft it
 * and hold it in the off-hand slot (this only prevents actually raising/blocking with it), but
 * blocking a melee attack only inflicts Slowness on the attacker for a Medusa wielder anyway —
 * see {@code SerpentAegisBlockMixin} for why that has to live in a mixin on {@code
 * LivingEntity.blockedByShield} rather than here (this class has no on-block hook of its own to
 * override; blocking is resolved entirely inside vanilla's damage-taking pipeline).
 *
 * <p>Blocks a projectile no differently than any other shield (full vanilla behavior, matching
 * the source doc's "no effect vs projectiles" — that's simply what a plain shield already does,
 * no extra code needed to make it *not* do something extra there).
 *
 * <p>Playtest addition: a real shield this heavy should cost something even before it blocks
 * anything. First attempt applied a real, ticking {@code minecraft:slowness} status effect while
 * equipped — silently never showed up for Medusa specifically, since her own {@code
 * stone_cursed_immunity.json} (Apoli's {@code origins:effect_immunity}) makes her immune to
 * Slowness entirely (deliberate — her own petrify powers apply it to herself in passing, and she
 * shouldn't be slowed by her own curse). Since Medusa is also the *only* origin that can actually
 * raise/block with this shield (see {@link #use} above), a status-effect-based penalty could never
 * have worked for its one real user. Fixed with a real attribute modifier instead — Slowness I's
 * own real formula (-0.15 to movement speed, {@code MULTIPLY_TOTAL}, confirmed via decompile — see
 * {@code ThrownSilkNet}'s own doc comment for the same real number), applied automatically by
 * vanilla's own per-slot attribute system whenever this is in the off-hand, the same mechanism
 * {@code HarpyJavelinItem}'s own weapon-stat override already uses for its own slot. A flat
 * attribute modifier isn't a status effect at all, so it can't be blocked by {@code
 * origins:effect_immunity} or anything else gated on "does this entity have effect X."
 */
public class SerpentAegisItem extends ShieldItem {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = new ResourceLocation("monster_origins", "medusa");
	private static final UUID SLOWNESS_MODIFIER_ID = UUID.fromString("8e0a3f9c-6e2f-4b8a-9b3d-2e6a1e2c9f7a");
	private final Multimap<Attribute, AttributeModifier> defaultModifiers;

	public SerpentAegisItem(Properties properties) {
		super(properties);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
				SLOWNESS_MODIFIER_ID, "Serpent Aegis weight", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL));
		this.defaultModifiers = builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		if (slot == EquipmentSlot.OFFHAND) {
			return this.defaultModifiers;
		}
		return super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!OriginUtil.hasOrigin(player, MEDUSA_ORIGIN_ID)) {
			return InteractionResultHolder.fail(player.getItemInHand(hand));
		}
		return super.use(level, player, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Blocking a melee attack slows the attacker and reflects half the damage");
		OriginUtil.addOriginGatedTooltip(tooltip, "Immune to Ghast fireballs");
		OriginUtil.addOriginGatedTooltip(tooltip, "Nearby allies take half damage while you block");
		OriginUtil.addOriginGatedTooltip(tooltip, "Medusa only");
	}
}
