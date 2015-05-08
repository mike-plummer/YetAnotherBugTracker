package edu.psu.yabt.util;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves as an accessor for YABT.properties
 * 
 * Externalized properties can be accessed through static use of this class.
 * All properties provide default values in case the properties file is missing
 * or un-loadable.
 */
public class YABTConstants 
{
	private static final Properties properties = new Properties();
	private static final String propertiesFile = "YABT.properties";
	private static final Logger logger = LoggerFactory.getLogger(YABTConstants.class);
	
	static
	{
		try
		{
			InputStream propFile = YABTConstants.class.getResourceAsStream(propertiesFile);
			if( propFile != null )
			{
				properties.load(propFile);
				logger.info("YABT.properties found, properties loaded.");
			}
		}
		catch(Exception e)
		{
			logger.warn("Error while loading properties, will fall back to defaults");
		}
	}
	
	public static final String SVN_URL()
	{
		return getPropertyOrDefault("yabt.svn.url", "http://subversion.assembla.com/svn/sweng500/");
	}
	
	public static final String SVN_USERNAME()
	{
		return getPropertyOrDefault("yabt.svn.username", "anonymous");
	}
	
	public static final String SVN_PASSWORD()
	{
		return getPropertyOrDefault("yabt.svn.password", "anonymous");
	}
	
	private static final String getPropertyOrDefault(String propertyKey, String defaultValue)
	{
		String value = properties.getProperty(propertyKey);
		return value == null ? defaultValue : value;
	}
}
