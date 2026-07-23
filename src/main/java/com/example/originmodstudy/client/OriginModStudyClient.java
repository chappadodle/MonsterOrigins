package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-only registration — entity renderers can't be registered from the common initializer. */
public class OriginModStudyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.THROWN_HARPY_JAVELIN, ThrownHarpyJavelinRenderer::new);
	}
}
