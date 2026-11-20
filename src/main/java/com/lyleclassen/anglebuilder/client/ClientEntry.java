package com.lyleclassen.anglebuilder.client;

import com.lyleclassen.anglebuilder.AngleBuilder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Client-side entry point; never loaded on a dedicated server. */
@Mod(value = AngleBuilder.MOD_ID, dist = Dist.CLIENT)
public final class ClientEntry {
	public ClientEntry(IEventBus modBus) {
		AngleBuilderClient.init(modBus);
	}
}
