package edu.psu.yabt.workflow;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.psu.yabt.util.StringUtils;

public class StringUtilsTest 
{

	@Test
	public void testNullOrEmpty() 
	{
		assertTrue(StringUtils.isNullOrEmpty(null));
		assertTrue(StringUtils.isNullOrEmpty(""));
		assertTrue(!StringUtils.isNullOrEmpty(" "));
	}
	
	@Test
	public void testLength() 
	{
		assertEquals(StringUtils.length(null), 0);
		assertEquals(StringUtils.length(""), 0);
		assertEquals(StringUtils.length(" "), 1);
		assertEquals(StringUtils.length("abcd"), 4);
	}
	
	@Test
	public void testTrim() 
	{
		assertEquals(StringUtils.trim(null), "");
		assertEquals(StringUtils.trim(" "), "");
		assertEquals(StringUtils.trim("abcd"), "abcd");
	}
}
