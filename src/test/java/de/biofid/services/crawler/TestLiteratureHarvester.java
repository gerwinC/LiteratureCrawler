package de.biofid.services.crawler;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestLiteratureHarvester {
	
	private static final String ITEM_ARRAY = "items";
	private static final String TITLE_ARRAY = "titles";
	
	DummyConfigurator configurator = null;

	@Test
	public void testHarvesterInstantiation() {
		Harvester.setOutputDirectory("path/to/output");
		LiteratureHarvester literatureHarvester = new LiteratureHarvester();
		Configuration configuration = configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING);
		Harvester instantiatedHarvester = literatureHarvester.instantiateHarvester(configuration);
		assertEquals("BHL", instantiatedHarvester.getFolderName());
	}
	
	@Before
	public void setup() throws IOException {
		configurator = getConfigurator();
	}
	
	@After
	public void cleanup() {
		configurator = null;
	}
	
	
	private DummyConfigurator getConfigurator() throws IOException {
		String configurationFilePathString = LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING;
		
		DummyConfigurator configurator = new DummyConfigurator();
		
		// Read Harvester configuration from config file and dump all items and collections
		configurator.readConfigurationYamlFile(configurationFilePathString);
		configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, ITEM_ARRAY);
		configurator.removeKeyFromConfiguration(BhlHarvester.BHL_STRING, TITLE_ARRAY);
		
		return configurator;
	}
}
