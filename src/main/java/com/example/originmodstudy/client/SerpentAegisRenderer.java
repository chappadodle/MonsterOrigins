package com.example.originmodstudy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Serpent Aegis exactly like vanilla renders its own Shield, minus the banner-pattern
 * branch (the Aegis never has dye/pattern NBT, so that whole case is dead code here). Reproduces
 * {@code BlockEntityWithoutLevelRenderer.renderByItem}'s real `itemStack.is(Items.SHIELD)` branch
 * directly (decompiled to confirm this — that check is an exact identity check, not
 * {@code instanceof ShieldItem}, which is exactly why a plain {@code ShieldItem} subclass like
 * {@code SerpentAegisItem} never gets vanilla's real 3D shield rendering for free and needs this
 * class at all): push pose, flip {@code scale(1, -1, -1)}, build a foil-aware vertex consumer via
 * {@code shieldModel.renderType(TEXTURE)} (a plain direct-texture {@code RenderType.entitySolid},
 * confirmed from {@code ShieldModel}'s own constructor — no atlas/{@code Material} juggling needed
 * since we always use one fixed texture, never vanilla's banner-pattern atlas), then render
 * {@code handle()} followed by {@code plate()}.
 *
 * <p>Reuses vanilla's real {@code ShieldModel} baked from the already-registered
 * {@code ModelLayers.SHIELD} layer (same 12x22x1 plate / 2x6x6 handle geometry every vanilla
 * shield has) — only the texture changes, so the Aegis is genuinely vanilla's own shield model,
 * not a hand-authored lookalike, matching the "copy the java shield" request directly.
 */
public class SerpentAegisRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "textures/entity/serpent_aegis.png");

	private ShieldModel shieldModel;

	@Override
	public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack,
			MultiBufferSource bufferSource, int light, int overlay) {
		if (this.shieldModel == null) {
			this.shieldModel = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
		}
		poseStack.pushPose();
		poseStack.scale(1.0F, -1.0F, -1.0F);
		RenderType renderType = this.shieldModel.renderType(TEXTURE);
		VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, stack.hasFoil());
		this.shieldModel.handle().render(poseStack, vertexConsumer, light, overlay, -1);
		this.shieldModel.plate().render(poseStack, vertexConsumer, light, overlay, -1);
		poseStack.popPose();
	}
}
