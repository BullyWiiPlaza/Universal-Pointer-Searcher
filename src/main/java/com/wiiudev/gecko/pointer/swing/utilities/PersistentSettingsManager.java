package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static com.wiiudev.gecko.pointer.swing.StackTraceUtilities.handleException;
import static com.wiiudev.gecko.pointer.swing.UniversalPointerSearcherGUI.APPLICATION_NAME;
import static com.wiiudev.gecko.pointer.swing.utilities.ProgramDirectoryUtilities.getProgramDirectory;
import static java.io.File.separator;

public class PersistentSettingsManager
{
	private static final String PERSISTENT_SETTINGS_EXTENSION = "properties";
	private Properties properties;
	private String propertiesFilePath;

	private PersistentSettingsManager(final String propertiesFilePath)
	{
		this.propertiesFilePath = propertiesFilePath;
		loadProperties();
	}

	public PersistentSettingsManager()
	{
		this(null);
	}

	private void loadProperties()
	{
		if (propertiesFilePath == null)
		{
			propertiesFilePath = getProgramDirectory()
			                     + separator + APPLICATION_NAME
			                     + "." + PERSISTENT_SETTINGS_EXTENSION;
		}

		properties = new Properties();

		try
		{
			if (new File(propertiesFilePath).exists())
			{
				val propertiesReader = new FileInputStream(propertiesFilePath);
				properties.load(propertiesReader);
			}
		} catch (IOException exception)
		{
			handleException(null, exception);
		}
	}

	public void put(String key, String value)
	{
		properties.setProperty(key, value);
	}

	public void writeToFile()
	{
		try
		{
			val propertiesWriter = new FileOutputStream(propertiesFilePath);
			properties.store(propertiesWriter, null);
		} catch (IOException exception)
		{
			handleException(null, exception);
		}
	}

	public String get(String key)
	{
		return (String) properties.get(key);
	}
}
