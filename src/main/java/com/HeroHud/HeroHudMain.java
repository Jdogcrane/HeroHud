package com.HeroHud;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "HeroHud",
	description = "Minimalistic ingame HUD with some QOL"
)
public class HeroHudMain extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private HeroHudConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HeroHudOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}


	@Provides
	HeroHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HeroHudConfig.class);
	}
}
