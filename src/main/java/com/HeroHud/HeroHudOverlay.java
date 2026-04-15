/*
 * Copyright (c) 2026, Joshua Crane <Joshua.Crane.me@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.HeroHud;

import com.google.common.collect.ImmutableSet;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class HeroHudOverlay extends Overlay
{
	private final Client client;
	private final HeroHudConfig config;

    private float staminaAlpha = 0f;
	private float healthAlpha = 0f;
	private float prayerAlpha = 0f;
	private float recoveryAlpha = 0f;

	private long lastStaminaChange = 0;
	private long lastHealthChange = 0;
	private long lastPrayerChange = 0;

	private int lastEnergy = -1;
	private int lastHealthValue = -1;
	private int lastPrayerValue = -1;
	private long lastEnergyDecreaseTime = 0;
	private long lastCombatTime = 0;

	private final Queue<Float> energyHistory = new LinkedList<>();
	private float trailingEnergy = -1f;
	private int lastHistoryTick = -1;

	// Pulser state
	private float pulserProgress = 0f;
	private float currentPulseDuration = 2000f; 
	private long lastPulseFrameTime = 0;
	private boolean lastRunningState = false;

	// Regen synchronization state
	private int ticksToRunEnergyRegen = 0;
	private long millisecondsToRunEnergyRegen = 0;
	private long millisecondsSinceRunEnergyRegen = 0;
	private int nextHighestRunEnergyMark = 0;

	private static final float FADE_SPEED = 0.02f;

	@Getter
	private enum GracefulEquipmentSlot
	{
		HEAD(EquipmentInventorySlot.HEAD.getSlotIdx(), 3, ItemID.GRACEFUL_HOOD),
		BODY(EquipmentInventorySlot.BODY.getSlotIdx(), 4, ItemID.GRACEFUL_TOP),
		LEGS(EquipmentInventorySlot.LEGS.getSlotIdx(), 4, ItemID.GRACEFUL_LEGS),
		GLOVES(EquipmentInventorySlot.GLOVES.getSlotIdx(), 3, ItemID.GRACEFUL_GLOVES),
		BOOTS(EquipmentInventorySlot.BOOTS.getSlotIdx(), 3, ItemID.GRACEFUL_BOOTS),
		CAPE(EquipmentInventorySlot.CAPE.getSlotIdx(), 3, ItemID.GRACEFUL_CAPE, ItemID.SKILLCAPE_AGILITY, ItemID.SKILLCAPE_AGILITY_TRIMMED_WORN, ItemID.SKILLCAPE_MAX);

		private final int index;
		private final int boost;
		private final Set<Integer> items;

		GracefulEquipmentSlot(int index, int boost, int... baseItems)
		{
			this.index = index;
			this.boost = boost;

			final ImmutableSet.Builder<Integer> itemsBuilder = ImmutableSet.builder();
			for (int item : baseItems)
			{
				itemsBuilder.addAll(ItemVariationMapping.getVariations(item));
			}
			items = itemsBuilder.build();
		}

		private static final int TOTAL_BOOSTS = Arrays.stream(values()).mapToInt(GracefulEquipmentSlot::getBoost).sum();
	}

	@Inject
	private HeroHudOverlay(Client client, HeroHudConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
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
		float currentEnergyPercent = energy / 100f;

		if (lastEnergy != -1 && energy < lastEnergy)
		{
			lastEnergyDecreaseTime = now;
		}
		boolean isRunning = (now - lastEnergyDecreaseTime) < 2000;

		// --- Pulser Duration Tracking ---
		if (lastEnergy != -1 && energy != lastEnergy)
		{
			lastStaminaChange = now;
		}
		
		if (isRunning != lastRunningState)
		{
			lastRunningState = isRunning;
		}

		// --- Energy History for Trail ---
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
			VisibilityMode mode = config.staminaVisibilityMode();
			boolean active = false;
			boolean timedOut = false;
			boolean alwaysShowOverride = false;

			switch (mode)
			{
				case ALWAYS_SHOW:
					active = true;
					alwaysShowOverride = true;
					break;
				case SHOW_IN_COMBAT:
					active = isRecentlyInCombat;
					break;
				case HIDE_IN_COMBAT:
					if (isRecentlyInCombat)
					{
						active = false;
					}
					else
					{
						active = (currentEnergyPercent >= config.staminaMinThreshold() && currentEnergyPercent <= config.staminaMaxThreshold())
							|| client.getVarbitValue(Varbits.STAMINA_EFFECT) > 0;
						if (config.staminaHideWalking() && !isRunning) active = false;
						timedOut = config.staminaInactivityTimer() > 0 && (now - lastStaminaChange) > (config.staminaInactivityTimer() * 1000L);
					}
					break;
				case UNSET:
				default:
					active = (currentEnergyPercent >= config.staminaMinThreshold() && currentEnergyPercent <= config.staminaMaxThreshold())
						|| client.getVarbitValue(Varbits.STAMINA_EFFECT) > 0;
					if (config.staminaHideWalking() && !isRunning) active = false;
					timedOut = config.staminaInactivityTimer() > 0 && (now - lastStaminaChange) > (config.staminaInactivityTimer() * 1000L);
					break;
			}

			staminaAlpha = updateAlpha(staminaAlpha, active && !timedOut, alwaysShowOverride);
			
			int offsetX = config.staminaPosition().getX() + config.staminaAnchorX();
			int offsetY = config.staminaPosition().getY() + config.staminaAnchorY();
			float sprintMultiplier = calculateSprintMultiplier();
			updatePulserProgress(isRunning, currentEnergyPercent);

			if (staminaAlpha > 0.01f)
			{
				Color staminaColor = config.staminaColor();
				if (config.staminaDynamicColor())
				{
					staminaColor = getDynamicColor(staminaColor, currentEnergyPercent, 50f);
				}

				String text = config.staminaShowValue() ? String.valueOf((int)currentEnergyPercent) : "";
				
				// Only show main pulser if not using 'hide when walking' logic (meaning we aren't hiding it while recovering)
				// OR if Always Show is enabled (meaning the main HUD is consistently visible).
				boolean allowMainPulser = (mode == VisibilityMode.ALWAYS_SHOW) || !(config.staminaHideWalking() && !isRunning);

				renderStaminaHUD(graphics, localPlayer, currentEnergyPercent, trailingEnergy, config.staminaStyle(), staminaColor,
					config.staminaOpacity(), config.staminaBgColor(), config.staminaBgOpacity(), 
					config.staminaSize(), offsetX, offsetY, 
					scaleFactor, client.getVarbitValue(Varbits.STAMINA_EFFECT), staminaAlpha, text,
					config.staminaShowBorder(), config.staminaBorderThickness(), config.staminaWheelThickness(), pulserProgress, sprintMultiplier, isRunning, allowMainPulser, config.staminaPosition());
				recoveryAlpha = 0f;
			}
			else if (config.staminaShowRecoveryHidden())
			{
				boolean recoveryActive = !isRunning && energy < 10000;
				recoveryAlpha = updateAlpha(recoveryAlpha, recoveryActive, false);

				if (recoveryAlpha > 0.01f)
				{
					Color recoveryColor = config.staminaColor();
					if (config.staminaDynamicColor())
					{
						recoveryColor = getDynamicColor(recoveryColor, currentEnergyPercent, 50f);
					}
					
					float timingAlpha = 1.0f;
					long stopTime = lastEnergyDecreaseTime + 2000;
					long fadeStartTime = stopTime + 3000;
					if (now > fadeStartTime)
					{
						float fadeTarget = config.staminaRecoveryFade() / 255.0f;
						timingAlpha = Math.max(fadeTarget, 1.0f - (float)(now - fadeStartTime) / 1000.0f);
					}

					renderRecoveryOnly(graphics, localPlayer, config.staminaRecoveryStyle(), recoveryColor, (int)(config.staminaOpacity() * recoveryAlpha * timingAlpha),
						config.staminaSize(), offsetX, offsetY, scaleFactor, pulserProgress, config.staminaRecoveryScale() / 100f, currentEnergyPercent / 100f, isRunning, recoveryAlpha);
				}
			}
			else
			{
				recoveryAlpha = 0f;
			}
		}

		// --- Health ---
		if (config.showHealth())
		{
			int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			int maxHp = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
			if (lastHealthValue != -1 && hp != lastHealthValue) { lastHealthChange = now; }
			lastHealthValue = hp;
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

			int offsetX = config.healthPosition().getX() + config.healthAnchorX();
			int offsetY = config.healthPosition().getY() + config.healthAnchorY();

			if (healthAlpha > 0.01f)
			{
				Color healthColor = config.healthColor();
				if (config.healthDynamicColor())
				{
					healthColor = getDynamicColor(healthColor, percent, 100f);
				}

				String text = config.healthShowValue() ? String.valueOf(hp) : "";
				renderHUD(graphics, localPlayer, percent, config.healthStyle(), healthColor, 
					config.healthOpacity(), config.healthBgColor(), config.healthBgOpacity(), 
					config.healthSize(), offsetX, offsetY, 
					scaleFactor, 0, healthAlpha, text, 
					config.healthShowBorder(), config.healthBorderThickness(), config.healthWheelThickness(), 0f, config.healthPosition());
			}
		}

		// --- Prayer ---
		if (config.showPrayer())
		{
			int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
			int maxPrayer = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
			if (lastPrayerValue != -1 && prayer != lastPrayerValue) { lastPrayerChange = now; }
			lastPrayerValue = prayer;
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

			int offsetX = config.prayerPosition().getX() + config.prayerAnchorX();
			int offsetY = config.prayerPosition().getY() + config.prayerAnchorY();

			if (prayerAlpha > 0.01f)
			{
				Color prayerColor = config.prayerColor();
				if (config.prayerDynamicColor())
				{
					prayerColor = getDynamicColor(prayerColor, percent, 100f);
				}

				String text = config.prayerShowValue() ? String.valueOf(prayer) : "";
				renderHUD(graphics, localPlayer, percent, config.prayerStyle(), prayerColor, 
					config.prayerOpacity(), config.prayerBgColor(), config.prayerBgOpacity(), 
					config.prayerSize(), offsetX, offsetY, 
					scaleFactor, 0, prayerAlpha, text, 
					config.prayerShowBorder(), config.prayerBorderThickness(), config.prayerWheelThickness(), 0f, config.prayerPosition());
			}
		}

		lastEnergy = energy;
		return null;
	}

	private float calculateSprintMultiplier()
	{
		int agility = client.getRealSkillLevel(Skill.AGILITY);
		int weight = client.getWeight();
		int clampedWeight = Math.max(0, Math.min(64, weight));

		float unitsLost = (float) Math.floor(60 + (67.0 * clampedWeight / 64.0)) * (1.0f - (agility / 300.0f));
		float baselineUnitsLost = (float) Math.floor(60 + (67.0 * 20.0 / 64.0)) * (1.0f - (agility / 300.0f));

		float ratio = baselineUnitsLost / Math.max(1, unitsLost);

		if (ratio > 1.0f)
		{
			float unitsLostAt0 = 60.0f * (1.0f - (agility / 300.0f));
			float maxRatio = baselineUnitsLost / Math.max(1, unitsLostAt0);
			if (maxRatio > 1.0f)
			{
				return 1.0f + (ratio - 1.0f) / (maxRatio - 1.0f);
			}
		}

		return ratio;
	}

	private int getGracefulRecoveryBoost()
	{
		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) return 0;

		final Item[] items = equipment.getItems();
		int boost = 0;

		for (final GracefulEquipmentSlot slot : GracefulEquipmentSlot.values())
		{
			if (items.length <= slot.getIndex()) continue;

			final Item wornItem = items[slot.getIndex()];
			if (wornItem != null && slot.getItems().contains(wornItem.getId()))
			{
				boost += slot.getBoost();
			}
		}

		if (boost == GracefulEquipmentSlot.TOTAL_BOOSTS)
		{
			boost += 10; // GRACEFUL_FULL_SET_BOOST_BONUS
		}

		return boost;
	}

	private float calculateExpectedDuration(boolean running)
	{
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
			double rawRunEnergyRegenPerTick = Math.floor((1 + (getGracefulRecoveryBoost() / 100.0d)) * (Math.floor(client.getBoostedSkillLevel(Skill.AGILITY) / 10.0d) + 15));
			unitsPerTick = (float) rawRunEnergyRegenPerTick;
		}
		
		if (unitsPerTick <= 0) return 2500f;
		// 1% of energy is 100 units.
		return (100f / unitsPerTick) * 600f;
	}

	private void updatePulserProgress(boolean running, float currentEnergyPercent)
	{
		long now = System.currentTimeMillis();
		if (lastPulseFrameTime == 0)
		{
			lastPulseFrameTime = now;
			return;
		}

		long deltaTime = now - lastPulseFrameTime;
		lastPulseFrameTime = now;

		if (running)
		{
			nextHighestRunEnergyMark = 0;
			currentPulseDuration = calculateExpectedDuration(true);
			pulserProgress += (float) deltaTime / currentPulseDuration;
			if (pulserProgress >= 1.0f)
			{
				pulserProgress -= (int) pulserProgress;
			}
		}
		else
		{
			int energy = client.getEnergy();
			if (energy >= 10000)
			{
				pulserProgress = 0;
				nextHighestRunEnergyMark = 0;
				return;
			}

			if (energy >= nextHighestRunEnergyMark || (lastEnergy != -1 && energy / 100 > lastEnergy / 100))
			{
				nextHighestRunEnergyMark = ((energy + 99) / 100) * 100;
				int agility = client.getBoostedSkillLevel(Skill.AGILITY);
				int boost = getGracefulRecoveryBoost();
				double rawRegen = Math.floor((1 + (boost / 100.0d)) * (Math.floor(agility / 10.0d) + 15));

				ticksToRunEnergyRegen = (int) Math.ceil((nextHighestRunEnergyMark - energy) / Math.max(1, rawRegen));
				millisecondsToRunEnergyRegen = ticksToRunEnergyRegen * 600L;
				millisecondsSinceRunEnergyRegen = 0;
			}

			if (millisecondsToRunEnergyRegen > 0)
			{
				millisecondsSinceRunEnergyRegen += deltaTime;
				float target = Math.min(1.0f, (float) millisecondsSinceRunEnergyRegen / millisecondsToRunEnergyRegen);
				// Small smoothing lerp to avoid jitter
				pulserProgress = pulserProgress + (target - pulserProgress) * 0.15f;
			}
			else
			{
				pulserProgress = 0;
			}
		}
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

	private void renderRecoveryOnly(Graphics2D graphics, Player player, RecoveryStyle style, Color color, int opacity, int baseSize, int offsetX, int offsetY, float scaleFactor, float recoveryProgress, float customScale, float staminaPercent, boolean isRunning, float shrinkScale)
	{
		float size = baseSize * scaleFactor * customScale * shrinkScale;
		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight());
		if (loc == null) return;

		float lx = (float)loc.getX();
		float ly = (float)loc.getY();
		float x = lx + offsetX * scaleFactor;
		float y = ly + offsetY * scaleFactor;

		Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
		Color pulserColor = isRunning ? new Color(255, 0, 0, opacity) : new Color(0, 255, 0, opacity);
		Color bgC = new Color(0, 0, 0, (int)(opacity * 0.4f));

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		switch (style)
		{
			case SPHERE:
				float currentSphereSize = size * 0.75f * staminaPercent;
				graphics.setColor(c);
				graphics.fill(new Ellipse2D.Float(x - currentSphereSize/2f, y - currentSphereSize/2f, currentSphereSize, currentSphereSize));
				if (recoveryProgress > 0)
				{
					graphics.setColor(pulserColor);
					graphics.setStroke(new BasicStroke(1.5f * scaleFactor * shrinkScale));
					graphics.draw(new Arc2D.Float(x - size/2f, y - size/2f, size, size, 90, -360 * recoveryProgress, Arc2D.OPEN));
				}
				break;
			case PIE_SPINNER:
				float pieSize = size * 0.75f;
				graphics.setColor(bgC);
				graphics.fill(new Ellipse2D.Float(x - pieSize/2f, y - pieSize/2f, pieSize, pieSize));
				graphics.setColor(c);
				graphics.fill(new Arc2D.Float(x - pieSize/2f, y - pieSize/2f, pieSize, pieSize, 90, -360 * staminaPercent, Arc2D.PIE));
				if (recoveryProgress > 0)
				{
					graphics.setColor(pulserColor);
					graphics.setStroke(new BasicStroke(1.5f * scaleFactor * shrinkScale));
					graphics.draw(new Arc2D.Float(x - size/2f, y - size/2f, size, size, 90, -360 * recoveryProgress, Arc2D.OPEN));
				}
				break;
			case ORB:
				float orbSize = size * 0.8f;
				float orbGap = size * 0.2f;
				float ox1 = x - orbSize - orbGap/2f;
				graphics.setColor(bgC);
				graphics.fill(new Ellipse2D.Float(ox1, y - orbSize/2f, orbSize, orbSize));
				Shape oldClip1 = graphics.getClip();
				graphics.setClip(new Ellipse2D.Float(ox1, y - orbSize/2f, orbSize, orbSize));
				graphics.setColor(c);
				float fh1 = orbSize * staminaPercent;
				graphics.fill(new Rectangle2D.Float(ox1, y + orbSize/2f - fh1, orbSize, fh1));
				graphics.setClip(oldClip1);
				float ox2 = x + orbGap/2f;
				graphics.setColor(bgC);
				graphics.fill(new Ellipse2D.Float(ox2, y - orbSize/2f, orbSize, orbSize));
				Shape oldClip2 = graphics.getClip();
				graphics.setClip(new Ellipse2D.Float(ox2, y - orbSize/2f, orbSize, orbSize));
				graphics.setColor(pulserColor);
				float fh2 = orbSize * recoveryProgress;
				graphics.fill(new Rectangle2D.Float(ox2, y + orbSize/2f - fh2, orbSize, fh2));
				graphics.setClip(oldClip2);
				break;
			case BAR:
				float bw = size * 1.5f, bh = size * 0.3f;
				float bGap = size * 0.15f;
				graphics.setColor(bgC);
				graphics.fill(new Rectangle2D.Float(x - bw/2f, y - bh - bGap/2f, bw, bh));
				graphics.setColor(c);
				graphics.fill(new Rectangle2D.Float(x - bw/2f, y - bh - bGap/2f, bw * staminaPercent, bh));
				graphics.setColor(bgC);
				graphics.fill(new Rectangle2D.Float(x - bw/2f, y + bGap/2f, bw, bh));
				graphics.setColor(pulserColor);
				graphics.fill(new Rectangle2D.Float(x - bw/2f, y + bGap/2f, bw * recoveryProgress, bh));
				break;
			case VERTICAL_BAR:
				float vbw = size * 0.3f, vbh = size * 1.5f;
				float vbGap = size * 0.15f;
				graphics.setColor(bgC);
				graphics.fill(new Rectangle2D.Float(x - vbw - vbGap/2f, y - vbh/2f, vbw, vbh));
				graphics.setColor(c);
				graphics.fill(new Rectangle2D.Float(x - vbw - vbGap/2f, y + vbh/2f - vbh * staminaPercent, vbw, vbh * staminaPercent));
				graphics.setColor(bgC);
				graphics.fill(new Rectangle2D.Float(x + vbGap/2f, y - vbh/2f, vbw, vbh));
				graphics.setColor(pulserColor);
				graphics.fill(new Rectangle2D.Float(x + vbGap/2f, y + vbh/2f - vbh * recoveryProgress, vbw, vbh * recoveryProgress));
				break;
		}
	}

	private void renderStaminaHUD(Graphics2D graphics, Player player, float percent, float trailingPercent, Style style, Color color, int opacity, Color bgColor, int bgOpacity, int baseSize, int offsetX, int offsetY, float scaleFactor, int varbit, float alpha, String text, boolean showBorder, int borderThick, int wheelThick, float energyChangeProgress, float sprintMultiplier, boolean isRunning, boolean allowPulser, HudPosition pos)
	{
		float size = baseSize * scaleFactor * alpha;
		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight());
		if (loc == null) return;

		float x = (float)loc.getX() + offsetX * scaleFactor;
		float y = (float)loc.getY() + offsetY * scaleFactor;

		float fillPercent = Math.max(0, Math.min(1.0f, percent / 100.0f));
		float trailPercent = Math.max(0, Math.min(1.0f, trailingPercent / 100.0f));

		Color fillC = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
		Color bgC = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgOpacity);
		Color trailC = new Color(255, 60, 0, (int)(opacity * 0.7f));

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		if (style == Style.WHEEL)
		{
			renderStaminaWheel(graphics, x, y, fillPercent, trailPercent, varbit, size, fillC, bgC, trailC, showBorder, borderThick, wheelThick * scaleFactor * alpha, energyChangeProgress, sprintMultiplier, isRunning, allowPulser, opacity);
		}
		else
		{
			renderHUD(graphics, player, percent, style, color, opacity, bgColor, bgOpacity, baseSize, offsetX, offsetY, scaleFactor, varbit, alpha, text, showBorder, borderThick, wheelThick, energyChangeProgress, pos);
		}
		
		if (style != Style.PERCENTAGE && !text.isEmpty())
		{
			renderText(graphics, x, y, text, size);
		}
		
		graphics.setComposite(oldComposite);
	}

	private void renderStaminaWheel(Graphics2D graphics, float x, float y, float percent, float trailPercent, int varbit, float size, Color color, Color bg, Color trailColor, boolean showBorder, int borderThick, float wheelThick, float energyChangeProgress, float sprintMultiplier, boolean isRunning, boolean allowPulser, int opacity)
	{
		float cx = x - size / 2f, cy = y - size / 2f;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		
		boolean effective = config.staminaEffectiveStamina();
		float visualPercent = effective ? percent * sprintMultiplier : percent;
		float visualTrail = effective ? trailPercent * sprintMultiplier : trailPercent;

		int maxLayers = effective ? (int) Math.ceil(sprintMultiplier) : 1;
		if (effective && sprintMultiplier < 1.0f) maxLayers = 1;

		int highestVisibleLayer = 0;

		for (int i = 0; i < maxLayers; i++)
		{
			float layerScale = 1.0f + (i * 0.4f);
			float layerSize = size * layerScale;
			float lcx = x - layerSize / 2f, lcy = y - layerSize / 2f;
			float lWheelThick = (i == 0) ? wheelThick : (wheelThick * 0.6f);

			float wheelFill = Math.max(0, Math.min(1.0f, visualPercent - i));
			float lTrailFill = Math.max(0, Math.min(1.0f, visualTrail - i));

			if (wheelFill <= 0 && lTrailFill <= 0) continue;
			
			highestVisibleLayer = i;

			if (wheelFill > lTrailFill)
			{
				// Lead (Regen)
				drawArc(graphics, lcx + lWheelThick / 2f, lcy + lWheelThick / 2f, layerSize - lWheelThick, wheelFill, color.brighter(), lWheelThick, true);
				drawArc(graphics, lcx + lWheelThick / 2f, lcy + lWheelThick / 2f, layerSize - lWheelThick, lTrailFill, color, lWheelThick, true);
			}
			else
			{
				// Trail (Loss)
				if (lTrailFill > wheelFill)
				{
					drawArc(graphics, lcx + lWheelThick / 2f, lcy + lWheelThick / 2f, layerSize - lWheelThick, lTrailFill, trailColor, lWheelThick, true);
				}
				// Stamina
				drawArc(graphics, lcx + lWheelThick / 2f, lcy + lWheelThick / 2f, layerSize - lWheelThick, wheelFill, color, lWheelThick, true);
			}

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

		float outerScale = 1.0f + (highestVisibleLayer * 0.4f) + 0.18f;
		float outerSize = size * outerScale;
		float ocx = x - outerSize / 2f, ocy = y - outerSize / 2f;

		boolean showPulser = allowPulser && (isRunning ? config.staminaShowDepletionPulser() : config.staminaShowRecoveryPulser());
		if (energyChangeProgress > 0 && showPulser)
		{
			Color circlingColor = isRunning ? new Color(255, 60, 0, opacity) : new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity).brighter();
			graphics.setColor(circlingColor);
			graphics.setStroke(new BasicStroke(borderThick + 0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
			float extent = isRunning ? 360 * energyChangeProgress : -360 * energyChangeProgress;
			graphics.draw(new Arc2D.Float(ocx, ocy, outerSize, outerSize, 90, extent, Arc2D.OPEN));
		}
	}

	private void renderHUD(Graphics2D graphics, Player player, float percent, Style style, Color color, int opacity, Color bgColor, int bgOpacity, int baseSize, int offsetX, int offsetY, float scaleFactor, int varbit, float alpha, String text, boolean showBorder, int borderThick, int wheelThick, float pulserProgress, HudPosition pos)
	{
		float size = baseSize * scaleFactor * alpha;
		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight());
		if (loc == null) return;

		float x = (float)loc.getX() + offsetX * scaleFactor;
		float y = (float)loc.getY() + offsetY * scaleFactor;

		float fillPercent = Math.max(0, Math.min(1.0f, percent / 100.0f));
		Color fillC = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
		Color bgC = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgOpacity);

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		switch (style)
		{
			case WHEEL: renderWheel(graphics, x, y, fillPercent, varbit, size, fillC, bgC, showBorder, borderThick, wheelThick * scaleFactor * alpha); break;
			case BAR: renderBar(graphics, x, y, fillPercent, size, fillC, bgC, false, showBorder, borderThick, pulserProgress, pos, scaleFactor * alpha); break;
			case VERTICAL_BAR: renderBar(graphics, x, y, fillPercent, size, fillC, bgC, true, showBorder, borderThick, pulserProgress, pos, scaleFactor * alpha); break;
			case PERCENTAGE: renderText(graphics, x, y, text, size); break;
			case PIE: renderPie(graphics, x, y, fillPercent, size, fillC, bgC, showBorder, borderThick, pulserProgress, scaleFactor * alpha); break;
			case ORB: renderOrb(graphics, x, y, fillPercent, size, fillC, bgC, showBorder, borderThick); break;
		}
		
		if (style != Style.PERCENTAGE && !text.isEmpty())
		{
			renderText(graphics, x, y, text, size);
		}
		
		graphics.setComposite(oldComposite);
	}

	private void renderWheel(Graphics2D graphics, float x, float y, float percent, int varbit, float size, Color color, Color bg, boolean showBorder, int borderThick, float wheelThick)
	{
		float cx = x - size / 2f, cy = y - size / 2f;
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

	private void renderBar(Graphics2D graphics, float x, float y, float percent, float size, Color color, Color bg, boolean vertical, boolean showBorder, int borderThick, float pulserProgress, HudPosition pos, float pulserScale)
	{
		float w = vertical ? size / 2f : size * 2f, h = vertical ? size * 2f : size / 2f;
		float sx = x - w / 2f, sy = y - h / 2f;
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

		if (pulserProgress > 0)
		{
			graphics.setColor(color.brighter());
			graphics.setStroke(new BasicStroke(1.5f * pulserScale));
			if (vertical)
			{
				float px = (pos == HudPosition.LEFT_SHOULDER) ? sx - 3f * pulserScale : sx + w + 3f * pulserScale;
				float py = sy + h * (1 - pulserProgress);
				graphics.draw(new Rectangle2D.Float(px, py, 2f * pulserScale, pulserScale));
			}
			else
			{
				float px = sx + w * pulserProgress;
				float py = sy - 3f * pulserScale;
				graphics.draw(new Rectangle2D.Float(px, py, pulserScale, 2f * pulserScale));
			}
		}
	}

	private void renderPie(Graphics2D graphics, float x, float y, float percent, float size, Color color, Color bg, boolean showBorder, int borderThick, float pulserProgress, float pulserScale)
	{
		float cx = x - size / 2f, cy = y - size / 2f;
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

		if (pulserProgress > 0)
		{
			float outerSize = size + 4f * pulserScale;
			float ocx = x - outerSize / 2f, ocy = y - outerSize / 2f;
			graphics.setColor(color.brighter());
			graphics.setStroke(new BasicStroke(1.5f * pulserScale));
			graphics.draw(new Arc2D.Float(ocx, ocy, outerSize, outerSize, 90, -360 * pulserProgress, Arc2D.OPEN));
		}
	}

	private void renderOrb(Graphics2D graphics, float x, float y, float percent, float size, Color color, Color bg, boolean showBorder, int borderThick)
	{
		float cx = x - size / 2f, cy = y - size / 2f;
		graphics.setColor(bg);
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		Shape oldClip = graphics.getClip();
		graphics.setClip(new Ellipse2D.Float(cx, cy, size, size));
		float fh = size * percent;
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

	private void renderText(Graphics2D graphics, float x, float y, String text, float size)
	{
		float fs = size / 2f;
		if (fs < 1) return;
		graphics.setFont(new Font("SansSerif", Font.BOLD, (int)fs));
		int tw = graphics.getFontMetrics().stringWidth(text), th = graphics.getFontMetrics().getAscent();
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x - tw / 2f + 1, y + th / 2f - 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x - tw / 2f, y + th / 2f - 2);
	}

	private Color getDynamicColor(Color baseColor, float percent, float threshold)
	{
		if (percent >= threshold) return baseColor;

		float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
		float originalBrightness = hsb[2];
		float originalSaturation = hsb[1];

		float targetHue;
		if (percent <= 10f)
		{
			targetHue = 0f;
		}
		else if (percent <= 30f)
		{
			float t = (percent - 10f) / 20f;
			targetHue = t * 0.08f;
		}
		else
		{
			float t = (percent - 30f) / (threshold - 30f);
			targetHue = 0.08f + t * (0.16f - 0.08f);
		}

		return Color.getHSBColor(targetHue, originalSaturation, originalBrightness);
	}
}
