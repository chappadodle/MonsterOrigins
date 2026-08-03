package com.example.originmodstudy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * A thrown-item renderer for the Harpy Javelin (Storm Trident)/Living Coral Trident/Petrifying
 * Trident, all of which now use vanilla's own real Trident geometry ({@code ModelLayers.TRIDENT})
 * for their held-item look too (see {@code HarpyJavelinRenderer}/{@code MermaidTridentRenderer}/
 * {@code PetrifyingTridentRenderer}). Rewritten this round to stop routing through {@code
 * ItemRenderer}/{@code BuiltinItemRendererRegistry} entirely and instead bake and render a shared
 * {@code TridentModel} directly — exactly what vanilla's own dedicated {@code
 * ThrownTridentRenderer} does (decompiled directly to confirm, same pass that found the shield's
 * real rendering code).
 *
 * <p><b>Why the previous {@code ItemRenderer}-based approach caused thrown items to float above
 * the surface</b> (reported via real playtest, not caught here): the three held-item renderers
 * above intentionally mirror {@code BlockEntityWithoutLevelRenderer}'s real {@code Items.TRIDENT}
 * branch, which includes a {@code poseStack.scale(1, -1, -1)} flip — a convention specific to
 * that *held-item* rendering path. Routing the thrown/stuck visual through those same renderers
 * (via {@code ItemRenderer.renderStatic} → {@code BuiltinItemRendererRegistry}, since Fabric
 * dispatches uniformly regardless of caller) applied that same flip to the thrown case too, where
 * vanilla's own equivalent (`ThrownTridentRenderer`) never uses one at all — shifting the
 * effective embed depth into "floating above the block" territory once combined with this
 * renderer's own rotation. The user's own suggestion — "just copy the Minecraft trident entirely
 * for that" — is exactly the fix: this class now bakes and renders {@code TridentModel} the same
 * direct way vanilla's dedicated thrown-trident renderer does, with no {@code ItemDisplayContext},
 * no scale flip, and no extra translate.
 *
 * <p>Subclasses supply only their own texture via {@link #getTexture()} — the model, rotation
 * math, and render sequence are identical for all three weapons (matching vanilla's own single
 * {@code ThrownTridentRenderer} handling every trident-shaped thrown entity the same way).
 *
 * <p>Rotation offsets restored to vanilla's own real values now that the geometry is vanilla's
 * own real geometry too: {@code yaw - 90}, {@code pitch + 90} (an earlier version of this class
 * used {@code pitch - 90}, an extra 180° specifically tuned for the old hand-authored custom
 * models' different local-space convention — no longer applicable now that the mesh itself is
 * vanilla's). {@code Mth.rotLerp} (angle-safe interpolation) is kept over vanilla's own plain
 * {@code Mth.lerp} — a genuine improvement independent of which geometry is being rotated, see
 * the git history/CLAUDE.md for the reasoning.
 */
public abstract class DirectionalThrownItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T> {
	private static TridentModel tridentModel;

	protected DirectionalThrownItemRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** The texture this weapon's thrown visual is painted with — the same constant each weapon's
	 * own held-item {@code DynamicItemRenderer} already defines, reused rather than duplicated. */
	protected abstract ResourceLocation getTexture();

	@Override
	public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		if (tridentModel == null) {
			tridentModel = new TridentModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
		}
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
		RenderType renderType = tridentModel.renderType(getTexture());
		VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(buffer, renderType, false, false);
		tridentModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
	}

	/** Never actually sampled for rendering (the real texture comes through {@link #getTexture()})
	 * — the same block-atlas placeholder {@code ThrownItemRenderer} itself returns (via the
	 * non-deprecated {@code InventoryMenu.BLOCK_ATLAS} constant), since {@code EntityRenderer}
	 * just requires some texture location to exist for its own bookkeeping. */
	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
