package com.lyleclassen.protractor;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the Protractor, which is why the mod must also be on a server. Everything else
 * (Anchors, Lines, rendering, storage) is client-only; see {@code client.ClientEntry}.
 */
@Mod(ProtractorMod.MOD_ID)
public class ProtractorMod {
	public static final String MOD_ID = "protractor";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	/** A plain item: all its behaviour is client-side input handling in {@code ProtractorClient}. */
	public static final DeferredItem<Item> PROTRACTOR = ITEMS.registerSimpleItem("protractor", new Item.Properties().stacksTo(1));

	public ProtractorMod(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(ProtractorMod::addToCreativeTab);
	}

	private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(PROTRACTOR.get());
	}
}
