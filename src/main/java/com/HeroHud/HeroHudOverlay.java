package com.HeroHud;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Queue;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class HeroHudOverlay extends Overlay
{
	private final Client client;
	private final HeroHudConfig config;
	private final ItemManager itemManager;

	private float staminaAlpha = 0f;
	private float healthAlpha = 0f;
	private float prayerAlpha = 0f;

	private long lastStaminaChange = 0;
	private long lastHealthChange = 0;
	private long lastPrayerChange = 0;

	private int lastEnergy = -1;
	private int lastHealth = -1;
	private int lastPrayer = -1;
	private long lastEnergyDecreaseTime = 0;
	private long lastCombatTime = 0;

	private final Queue<Float> energyHistory = new LinkedList<>();
	private float trailingEnergy = -1f;
	private int lastHistoryTick = -1;

	private int lastEnergyValueForProgress = -1;
	private long lastEnergyChangeTimeForProgress = 0;

	private static final float FADE_SPEED = 0.05f;

	@Inject
	private HeroHudOverlay(Client client, HeroHudConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return null;

		float scaleStrength = config.relativeScalingSlider() / 100.0f;
		float scaleFactor = 1.0f + (((float) client.getScale() / 512.0f) - 1.0f) * scaleStrength;

		long now = System.currentTimeMillis();
		int currentTick = client.getTickCount();

		boolean inCombat = false;
		Actor interacting = localPlayer.getInteracting();
		if (interacting instanceof NPC)
		{
			NPCComposition comp = ((NPC) interacting).getComposition();
			if (comp != null && comp.getActions() != null)
			{
				for (String action : comp.getActions())
				{
					if ("Attack".equalsIgnoreCase(action))
					{
						inCombat = true;
						break;
					}
				}
			}
		}
		else if (interacting instanceof Player || (interacting == null && localPlayer.getHealthScale() > 0))
		{
			if (localPlayer.getHealthScale() > 0 || (interacting != null && interacting.getHealthScale() > 0))
			{
				inCombat = true;
			}
		}

		if (inCombat) lastCombatTime = now;
		boolean isRecentlyInCombat = (now - lastCombatTime) < 2000;

		int energy = client.getEnergy();
		float currentEnergyPercent = (energy > 100) ? energy / 100f : (float) energy;

		if (lastEnergy != -1 && energy < lastEnergy)
		{
			lastEnergyDecreaseTime = now;
		}
		boolean energyChanged = energy != lastEnergy;
		boolean isRunning = (now - lastEnergyDecreaseTime) < 2000;
		lastEnergy = energy;

		// --- Energy History for Trail (Tick-synced) ---
		if (currentTick != lastHistoryTick)
		{
			energyHistory.add(currentEnergyPercent);
			if (energyHistory.size() > 3)
			{
				trailingEnergy = energyHistory.poll();
			}
			else if (trailingEnergy == -1f)
			{
				trailingEnergy = currentEnergyPercent;
			}
			lastHistoryTick = currentTick;
		}

		// --- Stamina ---
		if (config.showStamina())
		{
			if (energyChanged) { lastStaminaChange = now; }
			
			boolean active;
			boolean timedOut;

			if (config.staminaOnlyCombat())
			{
				active = isRecentlyInCombat;
				timedOut = false;
			}
			else
			{
				active = (currentEnergyPercent >= config.staminaMinThreshold() && currentEnergyPercent <= config.staminaMaxThreshold())
					|| client.getVarbitValue(Varbits.STAMINA_EFFECT) > 0;
				if (config.staminaHideWalking() && !isRunning) active = false;
				timedOut = config.staminaInactivityTimer() > 0 && (now - lastStaminaChange) > (config.staminaInactivityTimer() * 1000L);
			}

			staminaAlpha = updateAlpha(staminaAlpha, active && !timedOut, config.staminaAlwaysShow());
			
			if (staminaAlpha > 0.01f)
			{
				String text = config.staminaShowValue() ? ((int)currentEnergyPercent + (config.staminaShowPercentage() ? "%" : "")) : "";
				int offsetX = config.staminaPosition().getX() + config.staminaAnchorX();
				int offsetY = config.staminaPosition().getY() + config.staminaAnchorY();

				float sprintMultiplier = calculateSprintMultiplier();
				float energyChangeProgress = calculateEnergyChangeProgress(isRunning, currentEnergyPercent);

				renderStaminaHUD(graphics, localPlayer, currentEnergyPercent, trailingEnergy, config.staminaStyle(), config.staminaColor(),
					config.staminaOpacity(), config.staminaBgColor(), config.staminaBgOpacity(), 
					config.staminaSize(), offsetX, offsetY, 
					scaleFactor, client.getVarbitValue(Varbits.STAMINA_EFFECT), staminaAlpha, text, 
					config.staminaShowBorder(), config.staminaBorderThickness(), config.staminaWheelThickness(), energyChangeProgress, sprintMultiplier);
			}
		}

		// --- Health ---
		if (config.showHealth())
		{
			int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
			boolean valueChanged = lastHealth != -1 && hp != lastHealth;
			if (valueChanged) { lastHealthChange = now; lastHealth = hp; }
			float percent = (float) hp / maxHp * 100f;

			boolean active;
			boolean timedOut;

			if (config.healthOnlyCombat())
			{
				active = isRecentlyInCombat;
				timedOut = false;
			}
			else
			{
				active = percent <= config.healthMaxThreshold();
				if (config.healthHideWalking() && !isRunning) active = false;
				timedOut = config.healthInactivityTimer() > 0 && (now - lastHealthChange) > (config.healthInactivityTimer() * 1000L);
			}

			healthAlpha = updateAlpha(healthAlpha, active && !timedOut, config.healthAlwaysShow());

			if (healthAlpha > 0.01f)
			{
				String text = config.healthShowValue() ? String.valueOf(hp) : "";
				int offsetX = config.healthPosition().getX() + config.healthAnchorX();
				int offsetY = config.healthPosition().getY() + config.healthAnchorY();

				renderHUD(graphics, localPlayer, percent, config.healthStyle(), config.healthColor(), 
					config.healthOpacity(), config.healthBgColor(), config.healthBgOpacity(), 
					config.healthSize(), offsetX, offsetY, 
					scaleFactor, 0, healthAlpha, text, 
					config.healthShowBorder(), config.healthBorderThickness(), config.healthWheelThickness());
			}
		}

		// --- Prayer ---
		if (config.showPrayer())
		{
			int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
			int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
			boolean valueChanged = lastPrayer != -1 && prayer != lastPrayer;
			if (valueChanged) { lastPrayerChange = now; lastPrayer = prayer; }
			float percent = (float) prayer / maxPrayer * 100f;

			boolean active;
			boolean timedOut;

			if (config.prayerOnlyCombat())
			{
				active = isRecentlyInCombat;
				timedOut = false;
			}
			else
			{
				active = (percent >= config.prayerMinThreshold() && percent <= config.prayerMaxThreshold());
				if (config.prayerHideWalking() && !isRunning) active = false;
				timedOut = config.prayerInactivityTimer() > 0 && (now - lastPrayerChange) > (config.prayerInactivityTimer() * 1000L);
			}

			if (config.prayerOnlyActive() && !isAnyPrayerActive())
			{
				active = false;
			}

			prayerAlpha = updateAlpha(prayerAlpha, active && !timedOut, config.prayerAlwaysShow());

			if (prayerAlpha > 0.01f)
			{
				String text = config.prayerShowValue() ? String.valueOf(prayer) : "";
				int offsetX = config.prayerPosition().getX() + config.prayerAnchorX();
				int offsetY = config.prayerPosition().getY() + config.prayerAnchorY();

				renderHUD(graphics, localPlayer, percent, config.prayerStyle(), config.prayerColor(), 
					config.prayerOpacity(), config.prayerBgColor(), config.prayerBgOpacity(), 
					config.prayerSize(), offsetX, offsetY, 
					scaleFactor, 0, prayerAlpha, text, 
					config.prayerShowBorder(), config.prayerBorderThickness(), config.prayerWheelThickness());
			}
		}

		return null;
	}

	private float calculateSprintMultiplier()
	{
		int agility = client.getRealSkillLevel(Skill.AGILITY);
		int weight = client.getWeight();
		int clampedWeight = Math.max(0, Math.min(64, weight));

		// UnitsLost = floor(60 + (67 * weight/64)) * (1 - agility/300)
		float unitsLost = (float) Math.floor(60 + (67.0 * clampedWeight / 64.0)) * (1.0f - (agility / 300.0f));

		// Baseline: 20kg weight
		float baselineUnitsLost = (float) Math.floor(60 + (67.0 * 20.0 / 64.0)) * (1.0f - (agility / 300.0f));

		float ratio = baselineUnitsLost / Math.max(1, unitsLost);

		// User wants 0kg to be "double circle" (2.0) and 20kg to be "full ring" (1.0)
		// At 0kg, actual ratio is ~80/60 = 1.33. We map this to 2.0.
		if (ratio > 1.0f)
		{
			// Amplifying the efficiency to hit exactly 2.0 at 0kg (ratio ~1.33)
			float unitsLostAt0 = (float) Math.floor(60) * (1.0f - (agility / 300.0f));
			float maxRatio = baselineUnitsLost / Math.max(1, unitsLostAt0);
			if (maxRatio > 1.0f)
			{
				return 1.0f + (ratio - 1.0f) / (maxRatio - 1.0f);
			}
		}

		return ratio;
	}

	private float calculateEnergyChangeProgress(boolean running, float currentEnergyPercent)
	{
		int energyInt = (int) (currentEnergyPercent);
		if (energyInt != lastEnergyValueForProgress)
		{
			lastEnergyValueForProgress = energyInt;
			lastEnergyChangeTimeForProgress = System.currentTimeMillis();
		}

		if (energyInt >= 100 && !running) return 0f;

		float unitsPerTick;
		if (running)
		{
			int agility = client.getRealSkillLevel(Skill.AGILITY);
			int weight = client.getWeight();
			int clampedWeight = Math.max(0, Math.min(64, weight));
			unitsPerTick = (float) Math.floor(60 + (67.0 * clampedWeight / 64.0)) * (1.0f - (agility / 300.0f));
			if (client.getVarbitValue(Varbits.STAMINA_EFFECT) > 0) unitsPerTick *= 0.3f;
		}
		else
		{
			int agility = client.getRealSkillLevel(Skill.AGILITY);
			float baseR = (float) (Math.floor(agility / 10.0) + 15);

			int gracefulBonus = calculateGracefulBonus();
			float multiplier = 1.0f + (gracefulBonus / 100.0f);
			if (gracefulBonus >= 30) multiplier = 1.3f; // Full set is 30%

			unitsPerTick = (float) Math.floor(baseR * multiplier);
		}

		if (unitsPerTick <= 0) return 0f;

		// 100 units = 1 energy point
		float ticksPerPoint = 100f / unitsPerTick;
		long msPerPoint = (long)(ticksPerPoint * 600);

		if (msPerPoint <= 0) return 0f;
		long elapsed = System.currentTimeMillis() - lastEnergyChangeTimeForProgress;
		float progress = (elapsed % msPerPoint) / (float) msPerPoint;

		return running ? (1.0f - progress) : progress;
	}

	private int calculateGracefulBonus()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) return 0;

		int bonus = 0;
		int pieces = 0;
		Item[] items = equipment.getItems();

		int[] slots = {0, 1, 4, 7, 9, 10}; // HEAD, CAPE, BODY, LEGS, HANDS, BOOTS
		int[] pieceBonuses = {3, 4, 4, 5, 2, 2};

		for (int i = 0; i < slots.length; i++)
		{
			int slot = slots[i];
			if (slot < items.length && items[slot] != null)
			{
				String name = itemManager.getItemComposition(items[slot].getId()).getName();
				if (name != null && name.toLowerCase().contains("graceful"))
				{
					bonus += pieceBonuses[i];
					pieces++;
				}
			}
		}
		if (pieces == 6) bonus = 30;
		return bonus;
	}

	private boolean isAnyPrayerActive()
	{
		for (net.runelite.api.Prayer prayer : net.runelite.api.Prayer.values())
		{
			if (client.isPrayerActive(prayer))
			{
				return true;
			}
		}
		return false;
	}

	private float updateAlpha(float current, boolean active, boolean alwaysShow)
	{
		float target = (alwaysShow || active) ? 1.0f : 0.0f;
		if (current < target) return Math.min(target, current + FADE_SPEED);
		if (current > target) return Math.max(target, current - FADE_SPEED);
		return current;
	}

	private void renderStaminaHUD(Graphics2D graphics, Player player, float percent, float trailingPercent, Style style, Color color, int opacity, Color bgColor, int bgOpacity, int baseSize, int offsetX, int offsetY, float scaleFactor, int varbit, float alpha, String text, boolean showBorder, int borderThick, int wheelThick, float energyChangeProgress, float sprintMultiplier)
	{
		int size = (int) (baseSize * scaleFactor);
		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight());
		if (loc == null) return;

		int x = loc.getX() + (int)(offsetX * scaleFactor);
		int y = loc.getY() + (int)(offsetY * scaleFactor);

		float fillPercent = Math.max(0, Math.min(1.0f, percent / 100.0f));
		float trailPercent = Math.max(0, Math.min(1.0f, trailingPercent / 100.0f));

		Color fillC = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
		Color bgC = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgOpacity);
		Color trailC = new Color(255, 60, 0, (int)(opacity * 0.7f));

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (style == Style.WHEEL)
		{
			renderStaminaWheel(graphics, x, y, fillPercent, trailPercent, varbit, size, fillC, bgC, trailC, showBorder, borderThick, (int)(wheelThick * scaleFactor), energyChangeProgress, sprintMultiplier);
		}
		else
		{
			renderHUD(graphics, player, percent, style, color, opacity, bgColor, bgOpacity, baseSize, offsetX, offsetY, scaleFactor, varbit, alpha, text, showBorder, borderThick, wheelThick);
		}
		
		if (style != Style.PERCENTAGE && !text.isEmpty())
		{
			renderText(graphics, x, y, text, size);
		}
		
		graphics.setComposite(oldComposite);
	}

	private void renderStaminaWheel(Graphics2D graphics, int x, int y, float percent, float trailPercent, int varbit, int size, Color color, Color bg, Color trailColor, boolean showBorder, int borderThick, int wheelThick, float energyChangeProgress, float sprintMultiplier)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		
		float visualPercent = percent * sprintMultiplier;
		float visualTrail = trailPercent * sprintMultiplier;

		int maxLayers = (int) Math.ceil(sprintMultiplier);
		if (sprintMultiplier < 1.0f) maxLayers = 1;

		for (int i = 0; i < maxLayers; i++)
		{
			// Increased spacing (0.4f instead of 0.2f)
			float layerScale = 1.0f + (i * 0.4f);
			int layerSize = (int)(size * layerScale);
			int lcx = x - layerSize / 2, lcy = y - layerSize / 2;
			int lWheelThick = (i == 0) ? wheelThick : (int)(wheelThick * 0.6f);

			float wheelFill = Math.max(0, Math.min(1.0f, visualPercent - i));
			float trailFill = Math.max(0, Math.min(1.0f, visualTrail - i));

			if (wheelFill <= 0 && trailFill <= 0) continue;

			// Trail
			if (trailFill > wheelFill)
			{
				drawArc(graphics, lcx + lWheelThick/2f, lcy + lWheelThick/2f, layerSize - lWheelThick, trailFill, trailColor, lWheelThick, true);
			}

			// Stamina
			drawArc(graphics, lcx + lWheelThick/2f, lcy + lWheelThick/2f, layerSize - lWheelThick, wheelFill, color, lWheelThick, true);

			if (i == 0 && varbit > 0)
			{
				float pThick = Math.max(2, lWheelThick / 3f);
				drawArc(graphics, lcx - pThick, lcy - pThick, layerSize + pThick*2, Math.min(1f, varbit/200f), new Color(0,150,255), pThick, false);
			}

			if (showBorder)
			{
				graphics.setColor(Color.BLACK);
				graphics.setStroke(new BasicStroke(borderThick));
				graphics.draw(new Ellipse2D.Float(lcx, lcy, layerSize, layerSize));
				graphics.draw(new Ellipse2D.Float(lcx + lWheelThick, lcy + lWheelThick, layerSize - lWheelThick*2, layerSize - lWheelThick*2));
			}
		}

		float outerScale = 1.0f + ((maxLayers - 1) * 0.4f);
		int outerSize = (int)(size * outerScale);
		int ocx = x - outerSize / 2, ocy = y - outerSize / 2;

		if (energyChangeProgress > 0)
		{
			Color recoveryColor = color.brighter();
			graphics.setColor(new Color(recoveryColor.getRed(), recoveryColor.getGreen(), recoveryColor.getBlue(), 180));
			graphics.setStroke(new BasicStroke(borderThick + 0.5f));
			graphics.draw(new Arc2D.Float(ocx, ocy, outerSize, outerSize, 90, -360 * energyChangeProgress, Arc2D.OPEN));
		}
	}

	private void renderHUD(Graphics2D graphics, Player player, float percent, Style style, Color color, int opacity, Color bgColor, int bgOpacity, int baseSize, int offsetX, int offsetY, float scaleFactor, int varbit, float alpha, String text, boolean showBorder, int borderThick, int wheelThick)
	{
		int size = (int) (baseSize * scaleFactor);
		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight());
		if (loc == null) return;

		int x = loc.getX() + (int)(offsetX * scaleFactor);
		int y = loc.getY() + (int)(offsetY * scaleFactor);

		float fillPercent = Math.max(0, Math.min(1.0f, percent / 100.0f));
		Color fillC = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
		Color bgC = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgOpacity);

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		switch (style)
		{
			case WHEEL: renderWheel(graphics, x, y, fillPercent, varbit, size, fillC, bgC, showBorder, borderThick, (int)(wheelThick * scaleFactor)); break;
			case BAR: renderBar(graphics, x, y, fillPercent, size, fillC, bgC, false, showBorder, borderThick); break;
			case VERTICAL_BAR: renderBar(graphics, x, y, fillPercent, size, fillC, bgC, true, showBorder, borderThick); break;
			case PERCENTAGE: renderText(graphics, x, y, text, size); break;
			case PIE: renderPie(graphics, x, y, fillPercent, size, fillC, bgC, showBorder, borderThick); break;
			case ORB: renderOrb(graphics, x, y, fillPercent, size, fillC, bgC, showBorder, borderThick); break;
		}
		
		if (style != Style.PERCENTAGE && !text.isEmpty())
		{
			renderText(graphics, x, y, text, size);
		}
		
		graphics.setComposite(oldComposite);
	}

	private void renderWheel(Graphics2D graphics, int x, int y, float percent, int varbit, int size, Color color, Color bg, boolean showBorder, int borderThick, int wheelThick)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));

		drawArc(graphics, cx + wheelThick/2f, cy + wheelThick/2f, size - wheelThick, percent, color, wheelThick, true);
		
		if (varbit > 0)
		{
			float pThick = Math.max(2, wheelThick / 3f);
			drawArc(graphics, cx - pThick, cy - pThick, size + pThick*2, Math.min(1f, varbit/200f), new Color(0,150,255), pThick, false);
		}
		
		if (showBorder)
		{
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(borderThick));
			graphics.draw(new Ellipse2D.Float(cx, cy, size, size));
			graphics.draw(new Ellipse2D.Float(cx + wheelThick, cy + wheelThick, size - wheelThick*2, size - wheelThick*2));
		}
	}

	private void renderBar(Graphics2D graphics, int x, int y, float percent, int size, Color color, Color bg, boolean vertical, boolean showBorder, int borderThick)
	{
		int w = vertical ? size / 2 : size * 2, h = vertical ? size * 2 : size / 2;
		int sx = x - w / 2, sy = y - h / 2;
		graphics.setColor(bg);
		graphics.fill(new Rectangle2D.Float(sx, sy, w, h));
		graphics.setColor(color);
		if (vertical) graphics.fill(new Rectangle2D.Float(sx, sy + h * (1 - percent), w, h * percent));
		else graphics.fill(new Rectangle2D.Float(sx, sy, w * percent, h));
		
		if (showBorder)
		{
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(borderThick));
			graphics.draw(new Rectangle2D.Float(sx, sy, w, h));
		}
	}

	private void renderPie(Graphics2D graphics, int x, int y, float percent, int size, Color color, Color bg, boolean showBorder, int borderThick)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		graphics.setColor(color);
		graphics.fill(new Arc2D.Float(cx, cy, size, size, 90, -360 * percent, Arc2D.PIE));
		if (showBorder)
		{
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(borderThick));
			graphics.draw(new Ellipse2D.Float(cx, cy, size, size));
		}
	}

	private void renderOrb(Graphics2D graphics, int x, int y, float percent, int size, Color color, Color bg, boolean showBorder, int borderThick)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		Shape oldClip = graphics.getClip();
		graphics.setClip(new Ellipse2D.Float(cx, cy, size, size));
		int fh = (int) (size * percent);
		graphics.setColor(color);
		graphics.fill(new Rectangle2D.Float(cx, cy + (size - fh), size, fh));
		graphics.setClip(oldClip);
		if (showBorder)
		{
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(borderThick));
			graphics.draw(new Ellipse2D.Float(cx, cy, size, size));
		}
	}

	private void drawArc(Graphics2D graphics, float x, float y, float diameter, float percent, Color color, float sw, boolean cw)
	{
		if (percent <= 0) return;
		Stroke old = graphics.getStroke();
		graphics.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		graphics.setColor(color);
		graphics.draw(new Arc2D.Float(x, y, diameter, diameter, 90, cw ? -360 * percent : 360 * percent, Arc2D.OPEN));
		graphics.setStroke(old);
	}

	private void renderText(Graphics2D graphics, int x, int y, String text, int size)
	{
		int fs = Math.max(10, size / 2);
		graphics.setFont(new Font("SansSerif", Font.BOLD, fs));
		int tw = graphics.getFontMetrics().stringWidth(text), th = graphics.getFontMetrics().getAscent();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x - tw / 2 + 1, y + th / 2 - 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x - tw / 2, y + th / 2 - 2);
	}
}
