package edu.psu.yabt.notification;

import java.util.HashMap;
import java.util.Map;

public enum LinkType 
{
	BLOCKS("blocks"),
	IS_BLOCKED_BY("is blocked by"),
	IS_RELATED_TO("is related to"),
	IMPLEMENTS("implements"),
	IS_IMPLEMENTED_BY("is implemented by"),
	TESTS("tests"),
	IS_TESTED_BY("is tested by"),
	IS_DEPENDENCY_OF("is dependency of"),
	DEPENDS_ON("depends on");
	
	private String text;
	
	private static final Map<LinkType, LinkType> inverseTypes = new HashMap<LinkType, LinkType>();
	
	static
	{
		inverseTypes.put(BLOCKS, IS_BLOCKED_BY);
		inverseTypes.put(IS_BLOCKED_BY, BLOCKS);
		inverseTypes.put(IS_RELATED_TO, IS_RELATED_TO);
		inverseTypes.put(IMPLEMENTS, IS_IMPLEMENTED_BY);
		inverseTypes.put(IS_IMPLEMENTED_BY, IMPLEMENTS);
		inverseTypes.put(TESTS, IS_TESTED_BY);
		inverseTypes.put(IS_TESTED_BY, TESTS);
		inverseTypes.put(IS_DEPENDENCY_OF, DEPENDS_ON);
		inverseTypes.put(DEPENDS_ON, IS_DEPENDENCY_OF);
	}
	
	private LinkType(String text)
	{
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
	
	public LinkType getInverseLinkType()
	{
		return inverseTypes.get(this);
	}
}
