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

	@ConfigItem(keyName = "staminaStyle", name = "Style", description = "", position = 1, section = staminaSection)
	default Style staminaStyle() { return Style.WHEEL; }

	@ConfigItem(keyName = "staminaPosition", name = "Position", description = "Preset positions relative to player", position = 2, section = staminaSection)
	default HudPosition staminaPosition() { return HudPosition.LEFT_SHOULDER; }

	@ConfigItem(keyName = "staminaAlwaysShow", name = "Always Show", description = "Ignore thresholds and timers", position = 3, section = staminaSection)
	default boolean staminaAlwaysShow() { return false; }

	@ConfigItem(keyName = "staminaOnlyCombat", name = "Only In Combat", description = "Overrides thresholds when fighting", position = 4, section = staminaSection)
	default boolean staminaOnlyCombat() { return false; }

	@ConfigItem(keyName = "staminaHideWalking", name = "Hide When Walking", description = "Only show when stamina is actively decreasing (running)", position = 5, section = staminaSection)
	default boolean staminaHideWalking() { return true; }

	@ConfigItem(keyName = "staminaInactivityTimer", name = "Inactivity Timer (s)", description = "Fade out after X seconds of no change (0 to disable)", position = 6, section = staminaSection)
	@Range(min = 0, max = 60)
	default int staminaInactivityTimer() { return 0; }

	@ConfigItem(keyName = "staminaMinThreshold", name = "Min Show Threshold", description = "Hide if stamina is BELOW this value", position = 7, section = staminaSection)
	@Range(min = 0, max = 100)
	default int staminaMinThreshold() { return 10; }

	@ConfigItem(keyName = "staminaMaxThreshold", name = "Max Show Threshold", description = "Hide if stamina is ABOVE this value", position = 8, section = staminaSection)
	@Range(min = 1, max = 100)
	default int staminaMaxThreshold() { return 99; }

	@ConfigItem(keyName = "staminaShowValue", name = "Show Value Text", description = "Show the number", position = 9, section = staminaSection)
	default boolean staminaShowValue() { return false; }

	@ConfigItem(keyName = "staminaShowPercentage", name = "Show as Percentage", description = "Add % symbol", position = 10, section = staminaSection)
	default boolean staminaShowPercentage() { return true; }

	@ConfigItem(keyName = "staminaColor", name = "Fill Color", description = "", position = 11, section = staminaSection)
	default Color staminaColor() { return Color.decode("#FFE229"); }

	@ConfigItem(keyName = "staminaOpacity", name = "Fill Opacity", description = "0-255", position = 12, section = staminaSection)
	@Range(max = 255)
	default int staminaOpacity() { return 255; }

	@ConfigItem(keyName = "staminaBgColor", name = "Bg Color", description = "", position = 13, section = staminaSection)
	default Color staminaBgColor() { return Color.decode("#000000"); }

	@ConfigItem(keyName = "staminaBgOpacity", name = "Bg Opacity", description = "0-255", position = 14, section = staminaSection)
	@Range(max = 255)
	default int staminaBgOpacity() { return 50; }

	@ConfigItem(keyName = "staminaShowBorder", name = "Show Border", description = "", position = 15, section = staminaSection)
	default boolean staminaShowBorder() { return false; }

	@ConfigItem(keyName = "staminaBorderThickness", name = "Border Thick", description = "", position = 16, section = staminaSection)
	@Range(min = 1, max = 10)
	default int staminaBorderThickness() { return 1; }

	@ConfigItem(keyName = "staminaWheelThickness", name = "Wheel Thickness", description = "Only for Wheel style", position = 17, section = staminaSection)
	@Range(min = 1, max = 20)
	default int staminaWheelThickness() { return 4; }

	@ConfigItem(keyName = "staminaSize", name = "Size", description = "", position = 18, section = staminaSection)
	@Range(min = 10, max = 150)
	default int staminaSize() { return 15; }

	@ConfigItem(keyName = "staminaAnchorX", name = "Anchor X Override", description = "Used if position is Custom", position = 19, section = staminaSection)
	@Range(min = -300, max = 300)
	default int staminaAnchorX() { return 0; }

	@ConfigItem(keyName = "staminaAnchorY", name = "Anchor Y Override", description = "Used if position is Custom", position = 20, section = staminaSection)
	@Range(min = -300, max = 300)
	default int staminaAnchorY() { return 0; }

	// --- Health ---
	@ConfigItem(keyName = "showHealth", name = "Show Health", description = "", position = 0, section = healthSection)
	default boolean showHealth() { return false; }

	@ConfigItem(keyName = "healthStyle", name = "Style", description = "", position = 1, section = healthSection)
	default Style healthStyle() { return Style.BAR; }

	@ConfigItem(keyName = "healthPosition", name = "Position", description = "", position = 2, section = healthSection)
	default HudPosition healthPosition() { return HudPosition.TOP; }

	@ConfigItem(keyName = "healthAlwaysShow", name = "Always Show", description = "", position = 3, section = healthSection)
	default boolean healthAlwaysShow() { return false; }

	@ConfigItem(keyName = "healthOnlyCombat", name = "Only In Combat", description = "", position = 4, section = healthSection)
	default boolean healthOnlyCombat() { return true; }

	@ConfigItem(keyName = "healthHideWalking", name = "Hide When Walking", description = "Only show when running", position = 5, section = healthSection)
	default boolean healthHideWalking() { return false; }

	@ConfigItem(keyName = "healthInactivityTimer", name = "Inactivity Timer (s)", description = "", position = 6, section = healthSection)
	@Range(min = 0, max = 60)
	default int healthInactivityTimer() { return 5; }

	@ConfigItem(keyName = "healthMaxThreshold", name = "Max Show Threshold", description = "Hide if health is ABOVE this %", position = 7, section = healthSection)
	@Range(min = 1, max = 100)
	default int healthMaxThreshold() { return 100; }

	@ConfigItem(keyName = "healthShowValue", name = "Show Value Text", description = "", position = 8, section = healthSection)
	default boolean healthShowValue() { return false; }

	@ConfigItem(keyName = "healthColor", name = "Fill Color", description = "", position = 9, section = healthSection)
	default Color healthColor() { return Color.decode("#0AFF00"); }

	@ConfigItem(keyName = "healthOpacity", name = "Fill Opacity", description = "0-255", position = 10, section = healthSection)
	@Range(max = 255)
	default int healthOpacity() { return 255; }

	@ConfigItem(keyName = "healthBgColor", name = "Bg Color", description = "", position = 11, section = healthSection)
	default Color healthBgColor() { return Color.decode("#FF0000"); }

	@ConfigItem(keyName = "healthBgOpacity", name = "Bg Opacity", description = "0-255", position = 12, section = healthSection)
	@Range(max = 255)
	default int healthBgOpacity() { return 255; }

	@ConfigItem(keyName = "healthShowBorder", name = "Show Border", description = "", position = 13, section = healthSection)
	default boolean healthShowBorder() { return false; }

	@ConfigItem(keyName = "healthBorderThickness", name = "Border Thick", description = "", position = 14, section = healthSection)
	@Range(min = 1, max = 10)
	default int healthBorderThickness() { return 1; }

	@ConfigItem(keyName = "healthWheelThickness", name = "Wheel Thickness", description = "", position = 15, section = healthSection)
	@Range(min = 1, max = 20)
	default int healthWheelThickness() { return 10; }

	@ConfigItem(keyName = "healthSize", name = "Size", description = "", position = 16, section = healthSection)
	@Range(min = 10, max = 150)
	default int healthSize() { return 15; }

	@ConfigItem(keyName = "healthAnchorX", name = "Anchor X Override", description = "", position = 17, section = healthSection)
	@Range(min = -300, max = 300)
	default int healthAnchorX() { return 0; }

	@ConfigItem(keyName = "healthAnchorY", name = "Anchor Y Override", description = "", position = 18, section = healthSection)
	@Range(min = -300, max = 300)
	default int healthAnchorY() { return 0; }

	// --- Prayer ---
	@ConfigItem(keyName = "showPrayer", name = "Show Prayer", description = "", position = 0, section = prayerSection)
	default boolean showPrayer() { return true; }

	@ConfigItem(keyName = "prayerStyle", name = "Style", description = "", position = 1, section = prayerSection)
	default Style prayerStyle() { return Style.PIE; }

	@ConfigItem(keyName = "prayerPosition", name = "Position", description = "", position = 2, section = prayerSection)
	default HudPosition prayerPosition() { return HudPosition.LEFT_SHOULDER; }

	@ConfigItem(keyName = "prayerAlwaysShow", name = "Always Show", description = "", position = 3, section = prayerSection)
	default boolean prayerAlwaysShow() { return false; }

	@ConfigItem(keyName = "prayerOnlyCombat", name = "Only In Combat", description = "", position = 4, section = prayerSection)
	default boolean prayerOnlyCombat() { return false; }

	@ConfigItem(keyName = "prayerOnlyActive", name = "Only When Active", description = "Only show when a prayer is turned on", position = 5, section = prayerSection)
	default boolean prayerOnlyActive() { return true; }

	@ConfigItem(keyName = "prayerHideWalking", name = "Hide When Walking", description = "Only show when running", position = 6, section = prayerSection)
	default boolean prayerHideWalking() { return false; }

	@ConfigItem(keyName = "prayerInactivityTimer", name = "Inactivity Timer (s)", description = "", position = 7, section = prayerSection)
	@Range(min = 0, max = 60)
	default int prayerInactivityTimer() { return 0; }

	@ConfigItem(keyName = "prayerMinThreshold", name = "Min Show Threshold", description = "Hide if prayer is BELOW this value", position = 8, section = prayerSection)
	@Range(min = 0, max = 100)
	default int prayerMinThreshold() { return 0; }

	@ConfigItem(keyName = "prayerMaxThreshold", name = "Max Show Threshold", description = "Hide if prayer is ABOVE this value", position = 9, section = prayerSection)
	@Range(min = 1, max = 100)
	default int prayerMaxThreshold() { return 100; }

	@ConfigItem(keyName = "prayerShowValue", name = "Show Value Text", description = "", position = 10, section = prayerSection)
	default boolean prayerShowValue() { return true; }

	@ConfigItem(keyName = "prayerColor", name = "Fill Color", description = "", position = 11, section = prayerSection)
	default Color prayerColor() { return Color.decode("#00C8FF"); }

	@ConfigItem(keyName = "prayerOpacity", name = "Fill Opacity", description = "0-255", position = 12, section = prayerSection)
	@Range(max = 255)
	default int prayerOpacity() { return 255; }

	@ConfigItem(keyName = "prayerBgColor", name = "Bg Color", description = "", position = 13, section = prayerSection)
	default Color prayerBgColor() { return Color.BLACK; }

	@ConfigItem(keyName = "prayerBgOpacity", name = "Bg Opacity", description = "0-255", position = 14, section = prayerSection)
	@Range(max = 255)
	default int prayerBgOpacity() { return 150; }

	@ConfigItem(keyName = "prayerShowBorder", name = "Show Border", description = "", position = 15, section = prayerSection)
	default boolean prayerShowBorder() { return false; }

	@ConfigItem(keyName = "prayerBorderThickness", name = "Border Thick", description = "", position = 16, section = prayerSection)
	@Range(min = 1, max = 10)
	default int prayerBorderThickness() { return 1; }

	@ConfigItem(keyName = "prayerWheelThickness", name = "Wheel Thickness", description = "", position = 17, section = prayerSection)
	@Range(min = 1, max = 20)
	default int prayerWheelThickness() { return 10; }

	@ConfigItem(keyName = "prayerSize", name = "Size", description = "", position = 18, section = prayerSection)
	@Range(min = 10, max = 150)
	default int prayerSize() { return 10; }

	@ConfigItem(keyName = "prayerAnchorX", name = "Anchor X Override", description = "", position = 19, section = prayerSection)
	@Range(min = -300, max = 300)
	default int prayerAnchorX() { return 0; }

	@ConfigItem(keyName = "prayerAnchorY", name = "Anchor Y Override", description = "", position = 20, section = prayerSection)
	@Range(min = -300, max = 300)
	default int prayerAnchorY() { return 0; }

	// --- Global ---
	@ConfigItem(keyName = "relativeScalingSlider", name = "Relative Scaling strength", description = "Syncs zooming for all elements", position = 21)
	@Range(max = 100)
	default int relativeScalingSlider() { return 100; }
}
