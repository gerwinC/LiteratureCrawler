package de.biofid.services.crawler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/***
 * This class calls the harvesters and starts them.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class LiteratureHarvester {
	
	public static final String CONFIGURATION_FILE_PATH_STRING = "config/harvesting.yml";
	public static final String LOGGER_NAME = "globalLogger";
	
	private static final Logger logger = LogManager.getLogger(LOGGER_NAME);
	
	private HarvesterConfigurator configurator;
	
	public LiteratureHarvester() {
		this.configurator = new HarvesterConfigurator();
		
		try {
			configurator.readConfigurationYamlFile(CONFIGURATION_FILE_PATH_STRING);
		} catch (IOException ex) {
			logSevereError("Configuration file error!", ex);
		}
	}
	
	public void start() {
		Harvester.setOutputDirectory(configurator.getBaseOutputPath());
		
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

	private void logSevereError(String msg, Exception ex) {
		logger.fatal(msg);
		logger.fatal("Received error message: " + ex.getMessage());
		logger.fatal(Arrays.toString(ex.getStackTrace()));
	}
}
