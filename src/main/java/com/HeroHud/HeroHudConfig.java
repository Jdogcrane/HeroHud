package com.HeroHud;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("herohud")
public interface HeroHudConfig extends Config
{
	@ConfigSection(
		name = "Stamina Settings",
		description = "Options for the stamina HUD",
		position = 0
	)
	String staminaSection = "staminaSection";

	@ConfigSection(
		name = "Health Settings",
		description = "Options for the health HUD",
		position = 1
	)
	String healthSection = "healthSection";

	@ConfigSection(
		name = "Prayer Settings",
		description = "Options for the prayer HUD",
		position = 2
	)
	String prayerSection = "prayerSection";

	// --- Stamina ---
	@ConfigItem(keyName = "showStamina", name = "Show Stamina", description = "", position = 0, section = staminaSection)
	default boolean showStamina() { return true; }

	@ConfigItem(keyName = "staminaAlwaysShow", name = "Always Show", description = "", position = 1, section = staminaSection)
	default boolean staminaAlwaysShow() { return false; }

	@ConfigItem(keyName = "staminaThreshold", name = "Fade Threshold", description = "", position = 2, section = staminaSection)
	@Range(min = 1, max = 100)
	default int staminaThreshold() { return 100; }

	@ConfigItem(keyName = "staminaStyle", name = "Style", description = "", position = 3, section = staminaSection)
	default StaminaStyle staminaStyle() { return StaminaStyle.WHEEL; }

	@ConfigItem(keyName = "staminaColor", name = "Color", description = "", position = 4, section = staminaSection)
	default Color staminaColor() { return Color.GREEN; }

	@ConfigItem(keyName = "staminaOpacity", name = "Fill Opacity", description = "", position = 5, section = staminaSection)
	@Range(max = 255)
	default int staminaOpacity() { return 255; }

	@ConfigItem(keyName = "staminaSize", name = "Size", description = "", position = 6, section = staminaSection)
	@Range(min = 10, max = 150)
	default int staminaSize() { return 24; }

	@ConfigItem(keyName = "staminaAnchorX", name = "Anchor X", description = "", position = 7, section = staminaSection)
	@Range(min = -300, max = 300)
	default int staminaAnchorX() { return 0; }

	@ConfigItem(keyName = "staminaAnchorY", name = "Anchor Y", description = "", position = 8, section = staminaSection)
	@Range(min = -300, max = 300)
	default int staminaAnchorY() { return 60; }

	// --- Health ---
	@ConfigItem(keyName = "showHealth", name = "Show Health", description = "", position = 0, section = healthSection)
	default boolean showHealth() { return false; }

	@ConfigItem(keyName = "healthAlwaysShow", name = "Always Show", description = "", position = 1, section = healthSection)
	default boolean healthAlwaysShow() { return false; }

	@ConfigItem(keyName = "healthThreshold", name = "Fade Threshold", description = "", position = 2, section = healthSection)
	@Range(min = 1, max = 100)
	default int healthThreshold() { return 100; }

	@ConfigItem(keyName = "healthStyle", name = "Style", description = "", position = 3, section = healthSection)
	default StaminaStyle healthStyle() { return StaminaStyle.BAR; }

	@ConfigItem(keyName = "healthColor", name = "Color", description = "", position = 4, section = healthSection)
	default Color healthColor() { return Color.RED; }

	@ConfigItem(keyName = "healthOpacity", name = "Fill Opacity", description = "", position = 5, section = healthSection)
	@Range(max = 255)
	default int healthOpacity() { return 255; }

	@ConfigItem(keyName = "healthSize", name = "Size", description = "", position = 6, section = healthSection)
	@Range(min = 10, max = 150)
	default int healthSize() { return 24; }

	@ConfigItem(keyName = "healthAnchorX", name = "Anchor X", description = "", position = 7, section = healthSection)
	@Range(min = -300, max = 300)
	default int healthAnchorX() { return -40; }

	@ConfigItem(keyName = "healthAnchorY", name = "Anchor Y", description = "", position = 8, section = healthSection)
	@Range(min = -300, max = 300)
	default int healthAnchorY() { return 60; }

	// --- Prayer ---
	@ConfigItem(keyName = "showPrayer", name = "Show Prayer", description = "", position = 0, section = prayerSection)
	default boolean showPrayer() { return false; }

	@ConfigItem(keyName = "prayerAlwaysShow", name = "Always Show", description = "", position = 1, section = prayerSection)
	default boolean prayerAlwaysShow() { return false; }

	@ConfigItem(keyName = "prayerThreshold", name = "Fade Threshold", description = "", position = 2, section = prayerSection)
	@Range(min = 1, max = 100)
	default int prayerThreshold() { return 100; }

	@ConfigItem(keyName = "prayerStyle", name = "Style", description = "", position = 3, section = prayerSection)
	default StaminaStyle prayerStyle() { return StaminaStyle.BAR; }

	@ConfigItem(keyName = "prayerColor", name = "Color", description = "", position = 4, section = prayerSection)
	default Color prayerColor() { return new Color(0, 200, 255); }

	@ConfigItem(keyName = "prayerOpacity", name = "Fill Opacity", description = "", position = 5, section = prayerSection)
	@Range(max = 255)
	default int prayerOpacity() { return 255; }

	@ConfigItem(keyName = "prayerSize", name = "Size", description = "", position = 6, section = prayerSection)
	@Range(min = 10, max = 150)
	default int prayerSize() { return 24; }

	@ConfigItem(keyName = "prayerAnchorX", name = "Anchor X", description = "", position = 7, section = prayerSection)
	@Range(min = -300, max = 300)
	default int prayerAnchorX() { return 40; }

	@ConfigItem(keyName = "prayerAnchorY", name = "Anchor Y", description = "", position = 8, section = prayerSection)
	@Range(min = -300, max = 300)
	default int prayerAnchorY() { return 60; }

	// --- Global ---
	@ConfigItem(keyName = "fadeEnabled", name = "Fade In/Out", description = "Globally enable/disable smooth fading", position = 20)
	default boolean fadeEnabled() { return true; }

	@ConfigItem(keyName = "showPercentage", name = "Show Percentage Text", description = "", position = 21)
	default boolean showPercentage() { return true; }

	@ConfigItem(keyName = "backgroundOpacity", name = "Background Opacity", description = "", position = 22)
	@Range(max = 255)
	default int backgroundOpacity() { return 150; }

	@ConfigItem(keyName = "relativeScalingSlider", name = "Relative Scaling strength", description = "Controls how much elements scale and move with camera zoom", position = 23)
	@Range(max = 100)
	default int relativeScalingSlider() { return 100; }
}
