package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ModEntities;
import com.example.originmodstudy.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client-only registration — entity renderers can't be registered from the common initializer. */
public class OriginModStudyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.THROWN_HARPY_JAVELIN, ThrownHarpyJavelinRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_SILK_NET, ThrownSilkNetRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_MERMAID_TRIDENT, ThrownMermaidTridentRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_PETRIFYING_TRIDENT, ThrownPetrifyingTridentRenderer::new);
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.SERPENT_AEGIS, new SerpentAegisRenderer());
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.STORM_TRIDENT, new HarpyJavelinRenderer());
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.MERMAID_TRIDENT, new MermaidTridentRenderer());
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.PETRIFYING_TRIDENT, new PetrifyingTridentRenderer());
		TridentStyleFlatModels.register();
		MermaidAirSupplyHud.register();
	}
}
