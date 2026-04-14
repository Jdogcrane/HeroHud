package com.HeroHud;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisibilityMode
{
	UNSET("Unset"),
	ALWAYS_SHOW("Always Show"),
	SHOW_IN_COMBAT("Show in Combat"),
	HIDE_IN_COMBAT("Hide in Combat");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
