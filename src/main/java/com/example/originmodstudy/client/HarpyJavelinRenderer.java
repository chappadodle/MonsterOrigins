package com.example.originmodstudy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Harpy Javelin exactly like vanilla renders its own Trident — same "copy the java
 * [item]" technique as {@code SerpentAegisRenderer}, just reusing {@code ModelLayers.TRIDENT}/
 * {@code TridentModel} instead of the shield's model. Reproduces
 * {@code BlockEntityWithoutLevelRenderer.renderByItem}'s real {@code itemStack.is(Items.TRIDENT)}
 * branch directly (decompiled to confirm, same pass that found the shield's real code): push
 * pose, flip {@code scale(1, -1, -1)}, build a foil-aware vertex consumer via
 * {@code tridentModel.renderType(TEXTURE)} — note vanilla passes {@code false} (not {@code true},
 * unlike the shield's own call) for the "affects outline" argument here, matched exactly — then a
 * single {@code renderToBuffer} call (no separate handle/plate parts the way the shield has).
 *
 * <p>{@code TridentModel.createLayer()} bakes at 32x32 (confirmed via decompile), which is exactly
 * why the user's own replacement texture is 32x32, not the usual 16x16 flat-icon size this mod's
 * other items use — it's painted directly onto vanilla's real Trident UV layout via a Blockbench
 * plugin that reads `.minecraft`'s own files, the same technique already used for the Serpent
 * Aegis's shield texture.
 *
 * <p>Because {@code ItemRenderer.renderStatic} dispatches to this same registered renderer
 * regardless of *why* it was called, {@code DirectionalThrownItemRenderer} (which calls
 * {@code renderStatic} directly to draw the Javelin mid-flight/stuck in a target) automatically
 * picks this real 3D geometry up too, rotated to face the throw direction the same way it already
 * rotates the model — no separate thrown-specific renderer code needed.
 */
public class HarpyJavelinRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "textures/entity/harpy_javelin.png");

	private TridentModel tridentModel;

	@Override
	public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack,
			MultiBufferSource bufferSource, int light, int overlay) {
		if (this.tridentModel == null) {
			this.tridentModel = new TridentModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
		}
		poseStack.pushPose();
		poseStack.scale(1.0F, -1.0F, -1.0F);
		RenderType renderType = this.tridentModel.renderType(TEXTURE);
		VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, false, stack.hasFoil());
		this.tridentModel.renderToBuffer(poseStack, vertexConsumer, light, overlay, -1);
		poseStack.popPose();
	}
}
