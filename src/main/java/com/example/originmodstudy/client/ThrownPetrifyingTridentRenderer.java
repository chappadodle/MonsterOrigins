package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ThrownPetrifyingTrident;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a flying or landed Petrifying Trident using vanilla's real Trident geometry, oriented
 * along its flight direction — see {@link DirectionalThrownItemRenderer} for the full explanation.
 */
public class ThrownPetrifyingTridentRenderer extends DirectionalThrownItemRenderer<ThrownPetrifyingTrident> {
	public ThrownPetrifyingTridentRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected ResourceLocation getTexture() {
		return PetrifyingTridentRenderer.TEXTURE;
	}
}
