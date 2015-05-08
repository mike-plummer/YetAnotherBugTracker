package edu.psu.yabt.util;

public final class StringUtils 
{
	public static final boolean isNullOrEmpty(String input)
	{
		return input == null || input.isEmpty();
	}
	
	public static final int length(String input)
	{
		return input == null ? 0 : input.length();
	}
	
	public static final String trim(String input)
	{
		return input == null ? "" : input.trim();
	}
}
