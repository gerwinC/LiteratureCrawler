package de.biofid.services.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import de.biofid.services.crawler.BhlHarvester.ItemDoesNotExistException;

public class TestBhlHarvester {
	
	private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
	private boolean didTestDirectoryExistBeforeTest = true;
	private File testDirectory = null;
	
	private static final String ITEM_ARRAY = "items";
	private static final String TITLE_ARRAY = "titles";
	
	@Test
	public void testGetAllCollections() throws IOException {
		DummyConfigurator configurator = setup();
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		Map<Long, JSONObject> collectionMap = bhlHarvester.getAllCollections();
		assertFalse(collectionMap.isEmpty());
	}
	
	@Test
	public void testGetItemMetadata() throws IOException, AuthenticationException {
		DummyConfigurator configurator = setup();
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		int itemId = 22497;
		
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemId);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		JSONObject itemJson = bhlHarvester.getItemMetadata(itemId);
		assertEquals((int) itemJson.get("ItemID"), itemId);
		assertTrue(itemJson.has("Pages"));
		assertTrue(itemJson.has("Parts"));
		assertFalse(itemJson.has("OcrText"));
	}
	
	@Test
	public void testHarvestSingleElement() throws Exception {
		DummyConfigurator configurator = setup();
		
		long itemID = 22314;
		
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemID);
		
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		bhlHarvester.run();
		
		Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/text");
		Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/metadata");
		
		Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(Long.toString(itemID) + ".xml");
		Path expectedPdfFilePath = expectedTextDirectory.resolve(Long.toString(itemID) + ".pdf");
		Path expectedTxtFilePath = expectedTextDirectory.resolve(Long.toString(itemID) + ".txt");
		
		assertTrue(expectedPdfFilePath.toFile().exists());
		assertTrue(expectedMetadataFilePath.toFile().exists());
		assertTrue(expectedTxtFilePath.toFile().exists());
	}
	
	@Test
	public void testHarvestMultipleElements() throws Exception {
		DummyConfigurator configurator = setup();
		
		long[] itemIDArray = {22314, 122748};
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIDArray[0]);
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIDArray[1]);
		
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		bhlHarvester.run();
	
		Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/text");
		Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/metadata");
	
		for (long itemID : itemIDArray) {
			Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(Long.toString(itemID) + ".xml");
			Path expectedPdfFilePath = expectedTextDirectory.resolve(Long.toString(itemID) + ".pdf");
			Path expectedTxtFilePath = expectedTextDirectory.resolve(Long.toString(itemID) + ".txt");
			
			assertTrue(expectedPdfFilePath.toFile().exists());
			assertTrue(expectedMetadataFilePath.toFile().exists());
			assertTrue(expectedTxtFilePath.toFile().exists());
		}
	}
	
	@Test
	public void testLoadingTitleListFromFile() throws IOException {
		DummyConfigurator configurator = setup();
		
		String dummyFilePath = "src/test/resources/listOfTitles.txt";
		configurator.addItemToArray(BhlHarvester.BHL_STRING, TITLE_ARRAY, dummyFilePath);
		
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		assertEquals(599, bhlHarvester.getListOfItems().size());
	}
	
	@Test
	public void testHarvestTitles() throws Exception {
		DummyConfigurator configurator = setup();
		
		long titleID = 155962;
		long[] itemsIncludedInTitles = {261598, 261814};
		
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		List<Long> itemsFoundForTitle = bhlHarvester.getItemsFromTitle(titleID);
		
		for (long itemID : itemsIncludedInTitles) {
			assertTrue(itemsFoundForTitle.contains(itemID));
		}
	}
	
	@Test
	public void testExternalResourceAccess() throws IOException {
		DummyConfigurator configurator = setup();
		
		long itemID = 147893;
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemID);
		
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		
		bhlHarvester.run();
		
		Path expectedTextDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/text");
		Path expectedMetadataDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/bhl/metadata");
		
		Path expectedMetadataFilePath = expectedMetadataDirectory.resolve(Long.toString(itemID) + ".xml");
		Path expectedPdfFilePath = expectedTextDirectory.resolve(Long.toString(itemID) + ".pdf");
		
		assertTrue(expectedPdfFilePath.toFile().exists());
		assertTrue(expectedMetadataFilePath.toFile().exists());
	}
	
	@Test
	public void testHarvestCollections() throws Exception {
		DummyConfigurator configurator = setup();
		
		long[] collectionIDArray = {97};
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, collectionIDArray[0]);
		// TODO: Finish test
				
	}
	
	@Test(expected = ItemDoesNotExistException.class)
	public void testItemDoesNotExistException() 
			throws AuthenticationException, IOException  {
		DummyConfigurator configurator = setup();
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		long itemIdThatDoesNotExist = 1;
		
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIdThatDoesNotExist);

		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		bhlHarvester.getItemMetadata(itemIdThatDoesNotExist);
	}
	
	@Test(expected = AuthenticationException.class)
	public void testInvalidAuthorizationException() 
			throws AuthenticationException, IOException {
		DummyConfigurator configurator = setup();
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		long itemIdThatDoesNotExist = 1;
		
		configurator.addItemToArray(BhlHarvester.BHL_STRING, ITEM_ARRAY, itemIdThatDoesNotExist);
		
		BhlHarvester bhlHarvester = new BhlHarvester(
				configurator.getConfigurationForHarvesterName(BhlHarvester.BHL_STRING));
		bhlHarvester.setBhlApiKey("th1s-4p1-k3y-1sn0t-v4l1d");
		bhlHarvester.getItemMetadata(itemIdThatDoesNotExist);
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
	
	private DummyConfigurator setup() throws IOException {
		testDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING).toFile();
		didTestDirectoryExistBeforeTest = testDirectory.exists();
		
		return getConfigurator();
	}
	
	@After
	public void cleanAfterTest() throws IOException {
		if (!didTestDirectoryExistBeforeTest && testDirectory.exists()) {
			FileUtils.deleteDirectory(testDirectory);
		}
	}
}
