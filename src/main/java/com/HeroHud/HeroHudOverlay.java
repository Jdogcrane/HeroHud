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
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
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
	private static final float FADE_SPEED = 0.05f;

	@Inject
	private HeroHudOverlay(Client client, HeroHudConfig config)
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

		// Relative Scaling Strength
		float scaleStrength = config.relativeScalingSlider() / 100.0f;
		float zoomFactor = (float) client.getScale() / 512.0f;
		float scaleFactor = 1.0f + (zoomFactor - 1.0f) * scaleStrength;

		// --- Stamina ---
		if (config.showStamina())
		{
			int rawEnergy = client.getEnergy();
			float energy = (rawEnergy > 100) ? rawEnergy / 100f : (float) rawEnergy;
			boolean active = config.staminaAlwaysShow() || energy < config.staminaThreshold() || client.getVarbitValue(Varbits.STAMINA_EFFECT) > 0;
			staminaAlpha = updateFade(staminaAlpha, active);
			
			if (staminaAlpha > 0.01f)
			{
				renderHUD(graphics, localPlayer, energy, config.staminaStyle(), config.staminaColor(), 
					config.staminaOpacity(), config.staminaSize(), config.staminaAnchorX(), config.staminaAnchorY(), 
					scaleFactor, zoomFactor, scaleStrength, staminaAlpha, client.getVarbitValue(Varbits.STAMINA_EFFECT));
			}
		}

		// --- Health ---
		if (config.showHealth())
		{
			float health = (float) client.getBoostedSkillLevel(Skill.HITPOINTS) / client.getRealSkillLevel(Skill.HITPOINTS) * 100f;
			boolean active = config.healthAlwaysShow() || health < config.healthThreshold();
			healthAlpha = updateFade(healthAlpha, active);

			if (healthAlpha > 0.01f)
			{
				renderHUD(graphics, localPlayer, health, config.healthStyle(), config.healthColor(), 
					config.healthOpacity(), config.healthSize(), config.healthAnchorX(), config.healthAnchorY(), 
					scaleFactor, zoomFactor, scaleStrength, healthAlpha, 0);
			}
		}

		// --- Prayer ---
		if (config.showPrayer())
		{
			float prayer = (float) client.getBoostedSkillLevel(Skill.PRAYER) / client.getRealSkillLevel(Skill.PRAYER) * 100f;
			boolean active = config.prayerAlwaysShow() || prayer < config.prayerThreshold();
			prayerAlpha = updateFade(prayerAlpha, active);

			if (prayerAlpha > 0.01f)
			{
				renderHUD(graphics, localPlayer, prayer, config.prayerStyle(), config.prayerColor(), 
					config.prayerOpacity(), config.prayerSize(), config.prayerAnchorX(), config.prayerAnchorY(), 
					scaleFactor, zoomFactor, scaleStrength, prayerAlpha, 0);
			}
		}

		return null;
	}

	private float updateFade(float current, boolean active)
	{
		float target = active ? 1.0f : 0.0f;
		if (!config.fadeEnabled()) return target;
		if (current < target) return Math.min(target, current + FADE_SPEED);
		if (current > target) return Math.max(target, current - FADE_SPEED);
		return current;
	}

	private void renderHUD(Graphics2D graphics, Player player, float percent, StaminaStyle style, Color color, int opacity, int baseSize, int anchorX, int anchorY, float scaleFactor, float zoomFactor, float scaleStrength, float alpha, int varbit)
	{
		// Scale the anchor coordinates as well to keep relative distance
		// When zoomFactor increases, the screen coordinates expand. We multiply anchor by (1 + (zoom - 1) * strength)
		float anchorScale = 1.0f + (zoomFactor - 1.0f) * scaleStrength;
		int scaledAnchorX = (int) (anchorX * anchorScale);
		int scaledAnchorY = (int) (anchorY * anchorScale);
		int scaledSize = (int) (baseSize * scaleFactor);

		Point loc = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight() + scaledAnchorY);
		if (loc == null) return;

		int x = loc.getX() + scaledAnchorX;
		int y = loc.getY();

		float fillPercent = Math.max(0, Math.min(1.0f, percent / 100.0f));
		Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		switch (style)
		{
			case WHEEL: renderWheel(graphics, x, y, fillPercent, varbit, scaledSize, fillColor); break;
			case BAR: renderBar(graphics, x, y, fillPercent, scaledSize, fillColor, false); break;
			case VERTICAL_BAR: renderBar(graphics, x, y, fillPercent, scaledSize, fillColor, true); break;
			case PERCENTAGE: renderText(graphics, x, y, (int)percent + "%", scaledSize * 2); break;
			case PIE: renderPie(graphics, x, y, fillPercent, scaledSize, fillColor); break;
			case ORB: renderOrb(graphics, x, y, fillPercent, scaledSize, fillColor); break;
		}
		graphics.setComposite(oldComposite);
	}

	private void renderWheel(Graphics2D graphics, int x, int y, float percent, int varbit, int size, Color color)
	{
		int cx = x - size / 2, cy = y - size / 2;
		float sw = size / 5.0f;
		graphics.setColor(new Color(0, 0, 0, config.backgroundOpacity()));
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		drawArc(graphics, cx, cy, size, percent, color, sw, true);
		if (varbit > 0) drawArc(graphics, cx - (size/6f), cy - (size/6f), size + (size/3f), Math.min(1f, varbit/200f), new Color(0,150,255), 2f, false);
		if (config.showPercentage()) renderText(graphics, x, y, String.valueOf((int)(percent * 100)), size);
	}

	private void renderBar(Graphics2D graphics, int x, int y, float percent, int size, Color color, boolean vertical)
	{
		int w = vertical ? size / 2 : size * 2, h = vertical ? size * 2 : size / 2;
		int sx = x - w / 2, sy = y - h / 2;
		graphics.setColor(new Color(0, 0, 0, config.backgroundOpacity()));
		graphics.fill(new Rectangle2D.Float(sx, sy, w, h));
		graphics.setColor(color);
		if (vertical) graphics.fill(new Rectangle2D.Float(sx, sy + h * (1 - percent), w, h * percent));
		else graphics.fill(new Rectangle2D.Float(sx, sy, w * percent, h));
		graphics.setColor(Color.BLACK);
		graphics.draw(new Rectangle2D.Float(sx, sy, w, h));
		if (config.showPercentage()) renderText(graphics, x, y, String.valueOf((int)(percent * 100)), size);
	}

	private void renderPie(Graphics2D graphics, int x, int y, float percent, int size, Color color)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(new Color(0, 0, 0, config.backgroundOpacity()));
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		graphics.setColor(color);
		graphics.fill(new Arc2D.Float(cx, cy, size, size, 90, -360 * percent, Arc2D.PIE));
		if (config.showPercentage()) renderText(graphics, x, y, String.valueOf((int)(percent * 100)), size);
	}

	private void renderOrb(Graphics2D graphics, int x, int y, float percent, int size, Color color)
	{
		int cx = x - size / 2, cy = y - size / 2;
		graphics.setColor(new Color(0, 0, 0, config.backgroundOpacity()));
		graphics.fill(new Ellipse2D.Float(cx, cy, size, size));
		Shape oldClip = graphics.getClip();
		graphics.setClip(new Ellipse2D.Float(cx, cy, size, size));
		int fh = (int) (size * percent);
		graphics.setColor(color);
		graphics.fill(new Rectangle2D.Float(cx, cy + (size - fh), size, fh));
		graphics.setClip(oldClip);
		if (config.showPercentage()) renderText(graphics, x, y, String.valueOf((int)(percent * 100)), size);
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
