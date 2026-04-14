package com.HeroHud;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HeroHud
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HeroHudMain.class);
		RuneLite.main(args);
	}
}