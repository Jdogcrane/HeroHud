package com.HeroHud;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Style
{
	WHEEL("Wheel"),
	BAR("Horizontal Bar"),
	VERTICAL_BAR("Vertical Bar"),
	PERCENTAGE("Percentage Only"),
	PIE("Pie Chart"),
	ORB("Liquid Orb");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
