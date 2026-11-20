package com.lyleclassen.protractor.client;

import com.lyleclassen.protractor.ProtractorMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Client-side entry point; never loaded on a dedicated server. */
@Mod(value = ProtractorMod.MOD_ID, dist = Dist.CLIENT)
public final class ClientEntry {
	public ClientEntry(IEventBus modBus) {
		ProtractorClient.init(modBus);
	}
}
