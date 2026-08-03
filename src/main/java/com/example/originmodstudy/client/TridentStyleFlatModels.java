package com.example.originmodstudy.client;

import com.example.originmodstudy.item.HarpyJavelinItem;
import com.example.originmodstudy.item.MermaidTridentItem;
import com.example.originmodstudy.item.PetrifyingTridentItem;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;

/**
 * Registers the "flat icon" fallback models for the three items that now use vanilla's real
 * Trident geometry as their normal, always-registered model ({@code HarpyJavelinRenderer}/
 * {@code MermaidTridentRenderer}/{@code PetrifyingTridentRenderer}, all {@code builtin/entity}).
 *
 * <p>Real vanilla achieves "flat icon in the hotbar/inventory, real 3D when held" for its own
 * Trident via a hardcoded, item-specific special case buried in {@code ItemRenderer} itself
 * (decompiled directly to confirm — {@code getModel()} unconditionally swaps {@code Items.TRIDENT}
 * to a *separately loaded* {@code trident_in_hand} model resource, and {@code render()} swaps it
 * back to the plain flat one specifically for {@code GUI}/{@code GROUND}/{@code FIXED} contexts).
 * There is no generic per-item toggle for this in vanilla or Fabric — confirmed via web research,
 * since this project's own prior research never covered it. The real, documented Fabric mechanism
 * for loading an extra model resource that isn't a block state or an item's own default
 * registration is {@code ModelLoadingPlugin.register(...)} + {@code Context.addModels(...)} (the
 * Fabric Wiki's own "Rendering Blocks and Items Dynamically" tutorial); {@code
 * TridentStyleFlatIconMixin} is the other half — it's what actually decides, at render time,
 * *when* to swap to one of the models registered here, mirroring vanilla's own {@code render()}
 * bl2-branch logic exactly.
 */
public class TridentStyleFlatModels {
	// Bare model id, no "item/" folder prefix — matches vanilla's own real convention exactly
	// (confirmed via decompile: `ModelResourceLocation.vanilla("trident", "inventory")` wraps the
	// bare path "trident", not "item/trident"; the "inventory" variant is what tells the loader to
	// resolve it under models/item/ automatically). Getting this wrong resolves to a nonexistent
	// "models/item/item/storm_trident_flat.json" — a real bug shipped in the first version of this
	// class, reported back as "missing texture" in the hotbar/inventory (a missing *model*
	// resolves to the same visual as a missing texture, which is what made this look like a wrong-
	// texture-file problem at first rather than a wrong-model-path one).
	public static final ResourceLocation STORM_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "storm_trident_flat");
	public static final ResourceLocation MERMAID_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "mermaid_trident_flat");
	public static final ResourceLocation PETRIFYING_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "petrifying_trident_flat");

	/** Looked up by {@code TridentStyleFlatIconMixin} for whichever item is actually being
	 * rendered — keyed by {@code Item} instance rather than a per-item field, so adding a fourth
	 * trident-shaped item later only means one new map entry, not a new mixin branch. */
	public static final Map<Class<? extends Item>, ResourceLocation> FLAT_MODEL_BY_ITEM_CLASS = Map.of(
			HarpyJavelinItem.class, STORM_TRIDENT_FLAT,
			MermaidTridentItem.class, MERMAID_TRIDENT_FLAT,
			PetrifyingTridentItem.class, PETRIFYING_TRIDENT_FLAT
	);

	/** The three display contexts real vanilla forces back to a flat model for its own Trident —
	 * everything else (first/third person held, and this project's own {@code NONE}-context
	 * thrown/stuck rendering, which never reaches {@code ItemRenderer} at all) keeps the real 3D
	 * geometry. */
	public static boolean isFlatContext(ItemDisplayContext context) {
		return context == ItemDisplayContext.GUI
				|| context == ItemDisplayContext.GROUND
				|| context == ItemDisplayContext.FIXED;
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.addModels(
				new ModelResourceLocation(STORM_TRIDENT_FLAT, "inventory"),
				new ModelResourceLocation(MERMAID_TRIDENT_FLAT, "inventory"),
				new ModelResourceLocation(PETRIFYING_TRIDENT_FLAT, "inventory")
		));
	}
}
