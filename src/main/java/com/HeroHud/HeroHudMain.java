package com.HeroHud;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "HeroHud",
	description = "Custom HUD overlays for better gameplay"
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
		log.info("HeroHud started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		log.info("HeroHud stopped!");
	}

	@Provides
	HeroHudConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HeroHudConfig.class);
	}
}
