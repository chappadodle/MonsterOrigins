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
 * Renders the Living Coral Trident using vanilla's real Trident geometry, exactly like
 * {@link HarpyJavelinRenderer} does for the Harpy Javelin (see that class's doc for the full
 * decompile-based explanation of why this needs its own {@code BuiltinItemRendererRegistry}
 * hookup rather than a plain item model). Not named {@code ThrownMermaidTridentRenderer} — that
 * class already exists for this weapon's separate thrown-entity renderer
 * ({@code DirectionalThrownItemRenderer} subclass); this one is the held/inventory renderer.
 */
public class MermaidTridentRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	static final ResourceLocation TEXTURE =
			new ResourceLocation("monster_origins", "textures/entity/mermaid_trident.png");

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
		this.tridentModel.renderToBuffer(poseStack, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}
}
