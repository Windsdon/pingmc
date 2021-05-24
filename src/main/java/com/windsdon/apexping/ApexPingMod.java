package com.windsdon.apexping;

import net.fabricmc.api.ModInitializer;

public class ApexPingMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ApexPingOptionsProvider.init();
	}
}
