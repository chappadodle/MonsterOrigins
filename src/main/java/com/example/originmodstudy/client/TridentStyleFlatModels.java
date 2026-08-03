package com.example.originmodstudy.client;

import com.example.originmodstudy.item.HarpyJavelinItem;
import com.example.originmodstudy.item.MermaidTridentItem;
import com.example.originmodstudy.item.PetrifyingTridentItem;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
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
	// 1.21.1 port note: this constant's own path DOES need the "item/" prefix now — the opposite of
	// the 1.20.1-era bare-path convention this comment used to document. Confirmed by decompiling
	// both ends of Fabric API 2.1.0's real extra-model path: real vanilla items get their "item/"
	// prefix added automatically by `ModelBakery.loadItemModelAndDependencies` (`resourceLocation
	// .withPrefix("item/")`, confirmed via CFR), but Fabric's own `ModelLoaderMixin.addExtraModel`
	// (the actual consumer of `Context.addModels(...)`'s ids, confirmed via decompiling
	// fabric-model-loading-api-v1) loads its ids through the *raw* `getOrLoadModel`/`getModel` path
	// with no such prefix — it resolves straight to `models/<path>.json`, not `models/item/<path>
	// .json`. Since this project's real model files live at `models/item/storm_trident_flat.json`
	// etc (unchanged from before), the ResourceLocation handed to `addModels` now has to spell that
	// prefix out itself. The lookup side (`TridentStyleFlatIconMixin`) just needs to wrap the same
	// constant in `ModelResourceLocation.inventory(...)`/`new ModelResourceLocation(..., "inventory")`
	// consistently, which it already does — both ends only ever need to agree with each other, not
	// with vanilla's own per-item convention.
	public static final ResourceLocation STORM_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "item/storm_trident_flat");
	public static final ResourceLocation MERMAID_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "item/mermaid_trident_flat");
	public static final ResourceLocation PETRIFYING_TRIDENT_FLAT =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "item/petrifying_trident_flat");

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
		// Context#addModels(ResourceLocation...) as of Fabric API 2.1.0 (1.21.1) — confirmed via
		// javap that ModelLoadingPlugin.Context no longer has a ModelResourceLocation-accepting
		// overload at all, only plain ResourceLocation (ModelResourceLocation stopped being a
		// ResourceLocation subtype in this version too — it's now its own standalone record wrapping
		// one). The "inventory" variant is applied on the lookup side instead (see
		// TridentStyleFlatIconMixin), matching how Fabric's own ModelLoaderMixin#addExtraModel
		// registers these ids internally.
		ModelLoadingPlugin.register(context -> context.addModels(
				STORM_TRIDENT_FLAT,
				MERMAID_TRIDENT_FLAT,
				PETRIFYING_TRIDENT_FLAT
		));
	}
}
