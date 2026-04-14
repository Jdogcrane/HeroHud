package com.HeroHud;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecoveryStyle
{
	SPHERE("Small Sphere"),
	PIE_SPINNER("Pie & Spinner"),
	ORB("Small Orb"),
	BAR("Small Bar"),
	VERTICAL_BAR("Small Vertical Bar");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
