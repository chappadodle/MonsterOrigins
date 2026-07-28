package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ThrownSilkNet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Renders a flying Silk Net Shooter net using its own item model — same {@link ThrownItemRenderer}
 * pattern as {@code ThrownHarpyJavelinRenderer} (the vanilla base class used for snowballs/eggs/
 * ender pearls), just backed by {@code ThrownSilkNet}'s own real {@code getItem()} (inherited
 * unchanged from {@code ThrowableItemProjectile}) instead of an accessor-mixin workaround.
 */
public class ThrownSilkNetRenderer extends ThrownItemRenderer<ThrownSilkNet> {
	public ThrownSilkNetRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
