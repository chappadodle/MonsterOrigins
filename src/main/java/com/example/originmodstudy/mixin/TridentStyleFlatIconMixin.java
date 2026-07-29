package com.example.originmodstudy.mixin;

import com.example.originmodstudy.client.TridentStyleFlatModels;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces the Storm Trident/Living Coral Trident/Petrifying Trident back to a plain flat icon for
 * {@code GUI}/{@code GROUND}/{@code FIXED} display contexts, replicating vanilla's own real
 * dual-check for its Trident (decompiled directly to confirm, this session): {@code
 * ItemRenderer.getModel()} unconditionally resolves {@code Items.TRIDENT} to a separately-loaded
 * "in hand" model, and {@code render()} swaps it back to the flat one specifically for those three
 * contexts via {@code if (bl2) { if (itemStack.is(Items.TRIDENT)) { bakedModel = TRIDENT_MODEL; }
 * }}. Our own three items don't need the {@code getModel()} half of that dance — their *default*
 * registered model is already the 3D {@code builtin/entity} one (unlike vanilla's Trident, whose
 * default is the flat one) — only the flat-for-GUI direction needs forcing here.
 *
 * <p>Captures {@code itemStack}/{@code itemDisplayContext} via a HEAD {@code @Inject} into fields
 * first, the same enclosing-method-parameter-capture technique already proven in this project
 * (see {@code ThrownTridentMixin}/{@code MermaidTridentBonusDamageMixin}), rather than declaring
 * them as extra {@code @ModifyVariable} handler parameters — the latter relies on Mixin's local-
 * variable-table matching for parameters *other* than the one actually being modified, which this
 * project has not previously exercised and didn't want to risk untested on a real player-facing
 * render path. The {@code @ModifyVariable} handler below only ever touches the one variable it's
 * declared for ({@code BakedModel}, matched purely by type — the simplest, most standard use of
 * that annotation), reading the two captured fields instead of declaring them as its own params.
 */
@Mixin(ItemRenderer.class)
public abstract class TridentStyleFlatIconMixin {
	private ItemStack monsterOrigins$renderStack;
	private ItemDisplayContext monsterOrigins$renderContext;

	@Inject(method = "render", at = @At("HEAD"))
	private void monsterOrigins$captureRenderContext(ItemStack itemStack, ItemDisplayContext itemDisplayContext,
			boolean bl, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j,
			BakedModel bakedModel, CallbackInfo ci) {
		this.monsterOrigins$renderStack = itemStack;
		this.monsterOrigins$renderContext = itemDisplayContext;
	}

	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
	private BakedModel monsterOrigins$forceFlatIcon(BakedModel bakedModel) {
		ItemStack stack = this.monsterOrigins$renderStack;
		ItemDisplayContext context = this.monsterOrigins$renderContext;
		if (stack == null || context == null || !TridentStyleFlatModels.isFlatContext(context)) {
			return bakedModel;
		}
		ResourceLocation flatModelId = TridentStyleFlatModels.FLAT_MODEL_BY_ITEM_CLASS.get(stack.getItem().getClass());
		if (flatModelId == null) {
			return bakedModel;
		}
		return Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(flatModelId, "inventory"));
	}
}
