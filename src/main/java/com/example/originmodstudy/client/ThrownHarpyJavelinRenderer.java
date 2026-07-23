package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ThrownJavelin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Renders a flying or landed Harpy Javelin using its own item model — {@link ThrownItemRenderer}
 * is the same vanilla base class used for snowballs/eggs/ender pearls, calling
 * {@code ItemRenderer.renderStatic} with the entity's carried {@code ItemStack} instead of a
 * hardcoded entity model. See {@code ThrownJavelin} and CLAUDE.md for why the default
 * {@code ThrownTridentRenderer} couldn't be reused as-is.
 */
public class ThrownHarpyJavelinRenderer extends ThrownItemRenderer<ThrownJavelin> {
	public ThrownHarpyJavelinRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
