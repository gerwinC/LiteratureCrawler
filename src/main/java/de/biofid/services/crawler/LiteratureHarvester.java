package de.biofid.services.crawler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/***
 * This class calls the harvesters and starts them.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class LiteratureHarvester {
	
	public static final String CONFIGURATION_FILE_PATH_STRING = "config/harvesting.yml";
	public static final String LOGGER_OUTPUT_DIRECTORY_STRING = "logs/";
	
	private static final String LOGGER_FILE_STRING = "main.log";
	private static final Logger logger = Logger.getLogger(LiteratureHarvester.class.getName());
	private Level loggerLevel = Level.INFO;
	
	private HarvesterConfigurator configurator;
	
	public LiteratureHarvester() {
		this.configurator = new HarvesterConfigurator();
		
		try {
			configurator.readConfigurationYamlFile(CONFIGURATION_FILE_PATH_STRING);
			setLoggerLevel(configurator.getLoggerLevel());
		} catch (IOException ex) {
			logSevereError("Configuration file error!", ex);
		}
		
		setupLogger();
	}
	
	public void start() {
		Harvester.setOutputDirectory(configurator.getBaseOutputPath());
		Harvester.setLoggerLevel(loggerLevel);
		
		for (Configuration harvesterConfiguration : configurator.getConfigurations()) {
			Harvester harvester = instantiateHarvester(harvesterConfiguration);
			if (harvester != null) {
				harvester.run();
			}
		}
	}
	
	public static void main(String[] args) {
		LiteratureHarvester harvester = new LiteratureHarvester();
		harvester.start();
	}
	
	@SuppressWarnings("unchecked")
	private Constructor<Harvester> getHarvesterConstructorForName(String qualifiedHarvesterClassName) 
			throws ClassNotFoundException, NoSuchMethodException{
		Class<?> clazz = Class.forName(qualifiedHarvesterClassName);
		return (Constructor<Harvester>) clazz.getConstructor(Configuration.class);
	}
	
	private Harvester instantiateHarvester(Configuration harvesterConfiguration) {
		String harvesterName = harvesterConfiguration.getHarvesterClassName();
		Constructor<Harvester> harvesterConstructor = null;
		try {
			 harvesterConstructor = getHarvesterConstructorForName(harvesterName);
		} catch (ClassNotFoundException | NoSuchMethodException ex) {
			logSevereError("Problems finding constructor for Harvester '" + harvesterName + "'!", ex);
			return null;
		}
		
		Harvester harvester;
		try {
			harvester = harvesterConstructor.newInstance(harvesterConfiguration);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException ex) {
			logSevereError("Could not instantiate Harvester '" + harvesterName + "'!", ex);
			return null;
		}
		
		return harvester;
	}

	
	private void setupLogger() {
		logger.setLevel(loggerLevel);
		
		FileHandler fileTxt;
		try {
			Path loggerOutputDirectoryPath = Paths.get(LiteratureHarvester.LOGGER_OUTPUT_DIRECTORY_STRING);
			File loggerOutputDirectory = loggerOutputDirectoryPath.toFile();
			if (!loggerOutputDirectory.exists()) {
				loggerOutputDirectory.mkdirs();
			}
			
			fileTxt = new FileHandler(LiteratureHarvester.LOGGER_OUTPUT_DIRECTORY_STRING + LiteratureHarvester.LOGGER_FILE_STRING);
		} catch (SecurityException | IOException e) {
			logger.warning(Arrays.toString(e.getStackTrace()));
			return;
		}

        // create a TXT formatter
        SimpleFormatter formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);
	}
	
	private void setLoggerLevel(String loggerLevelString) {
		switch (loggerLevelString) {
			case "debug": case "finer": loggerLevel = Level.FINER; break;
			case "fine": loggerLevel = Level.FINE; break;
			case "info": default: loggerLevel = Level.INFO; break;
		}
	}
	
	private void logSevereError(String msg, Exception ex) {
		logger.severe(msg);
		logger.severe("Received error message: " + ex.getMessage());
		logger.severe(Arrays.toString(ex.getStackTrace()));
	}
}
