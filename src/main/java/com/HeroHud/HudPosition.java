package com.HeroHud;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HudPosition
{
	LEFT_SHOULDER("Left Shoulder", -20, 10),
	RIGHT_SHOULDER("Right Shoulder", 20, 10),
	TOP("Top", 0, -5),
	CUSTOM("Custom", 0, 0);

	private final String name;
	private final int x;
	private final int y;

	@Override
	public String toString()
	{
		return name;
	}
}
