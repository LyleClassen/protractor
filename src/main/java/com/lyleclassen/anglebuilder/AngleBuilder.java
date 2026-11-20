package com.lyleclassen.anglebuilder;

import com.lyleclassen.anglebuilder.client.AngleBuilderClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only: the mod does nothing on a dedicated server and isn't needed there. */
@Mod(value = AngleBuilder.MOD_ID, dist = Dist.CLIENT)
public class AngleBuilder {
	public static final String MOD_ID = "anglebuilder";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public AngleBuilder(IEventBus modBus) {
		AngleBuilderClient.init(modBus);
	}
}
