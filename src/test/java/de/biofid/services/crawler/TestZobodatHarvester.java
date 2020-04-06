package de.biofid.services.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class TestZobodatHarvester {
	
	private static final String CITATION_AUTHORS = "authors";
	private static final String CITATION_FIRST_PAGE = "firstPage";
	private static final String CITATION_JOURNAL_NAME = "journalName";
	private static final String CITATION_LAST_PAGE = "lastPage";
	private static final String CITATION_TITLE = "title";
	private static final String CITATION_YEAR = "year";
	private static final String CITATION_ISSUE_NUMBER = "issueNumber";
	
	private static final String ITEM_ARRAY = "items";
	
	private static final String METADATA_CITATION = "citation";
	private static final String METADATA_PDF_URL = "pdfUrl";
	
	private static final String TEST_OUTPUT_DIRECTORY_STRING = "/tmp/test";
	
	private boolean didTestDirectoryExistBeforeTest = true;
	private File testDirectory = null;
	
	@Test
	public void testFetchingItemListDirectly() throws IOException {
		DummyConfigurator configurator = setup();
		String startingURL = "https://www.zobodat.at/publikation_volumes.php?id=57342";
		
		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		Document siteWithItems = zobodatHarvester.getDocumentFromUrl(startingURL);
		Elements itemList = zobodatHarvester.getItemListFromWebsite(siteWithItems);
		assertEquals(7, itemList.size());
		
		zobodatHarvester.iterateItems(itemList);
		assertEquals(7, zobodatHarvester.getMetadataListSize());
		
		JSONArray itemMetadataJson = zobodatHarvester.getMetadataListAsJSONArray();
		assertEquals(7, itemMetadataJson.length());
		
		for (int i = 0; i < itemMetadataJson.length(); ++i) {
			JSONObject item = itemMetadataJson.getJSONObject(i);
			areAllMetadataFieldsSerialized(item);
		}
		
		JSONObject item2 = itemMetadataJson.getJSONObject(2);
		JSONObject item2Citation = item2.getJSONObject(METADATA_CITATION);
		assertTrue(item2Citation.getJSONArray(CITATION_AUTHORS).length() == 1);
		assertTrue(item2Citation.getJSONArray(CITATION_AUTHORS).toList().contains("Hugo Krüss"));
		assertEquals(1884, item2Citation.getInt(CITATION_YEAR));
		assertEquals("Eine neue Form des Bunsen-Photometers", item2Citation.get(CITATION_TITLE));
		assertEquals("Abhandlungen aus dem Gebiete der Naturwissenschaften Hamburg", item2Citation.get(CITATION_JOURNAL_NAME));
		assertEquals("8", item2Citation.get(CITATION_ISSUE_NUMBER));
		assertEquals("1", item2Citation.get(CITATION_FIRST_PAGE));
		assertEquals("8", item2Citation.get(CITATION_LAST_PAGE));
		
		JSONObject item4 = itemMetadataJson.getJSONObject(4);
		JSONObject item4Citation = item4.getJSONObject(METADATA_CITATION);
		assertTrue(item4Citation.getJSONArray(CITATION_AUTHORS).length() == 1);
		assertTrue(item4Citation.getJSONArray(CITATION_AUTHORS).toList().contains("Heinrich Gustav Kirchenpauer"));
		assertEquals(1884, item4Citation.getInt(CITATION_YEAR));
		assertEquals("Nordische Gatungen und Arten von Sertulariden", item4Citation.get(CITATION_TITLE));
		assertEquals("Abhandlungen aus dem Gebiete der Naturwissenschaften Hamburg", item4Citation.get(CITATION_JOURNAL_NAME));
		assertEquals("8", item4Citation.get(CITATION_ISSUE_NUMBER));
		assertEquals("1", item4Citation.get(CITATION_FIRST_PAGE));
		assertEquals("56", item4Citation.get(CITATION_LAST_PAGE));
	}
	
	@Test
	public void testFetchingItemListsFromJournalOverviewSite() throws IOException {
		DummyConfigurator configurator = setup();
		String startingUrl = "https://www.zobodat.at/publikation_series.php?id=20987";
		
		ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
				configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		Document journalSite = zobodatHarvester.getDocumentFromUrl(startingUrl);
		Elements itemList = zobodatHarvester.getItemListFromWebsite(journalSite);
		assertEquals(7, itemList.size());
		
		zobodatHarvester.iterateItems(itemList);
		assertEquals(80, zobodatHarvester.getMetadataListSize());
		
		JSONArray itemMetadataJson = zobodatHarvester.getMetadataListAsJSONArray();
		assertEquals(80, itemMetadataJson.length());
		
		for (int i = 0; i < itemMetadataJson.length(); ++i) {
			JSONObject item = itemMetadataJson.getJSONObject(i);
			areAllMetadataFieldsSerialized(item);
		}
		
		JSONObject item39 = itemMetadataJson.getJSONObject(39);
		JSONObject item39Citation = item39.getJSONObject(METADATA_CITATION);
		assertTrue(item39Citation.getJSONArray(CITATION_AUTHORS).length() == 1);
		assertTrue(item39Citation.getJSONArray(CITATION_AUTHORS).toList().contains("Hans Roeckl"));
		assertEquals(1922, item39Citation.getInt(CITATION_YEAR));
		assertEquals("P.rotokoll der Hauptversamrnlurig am 14. August 1921 nachmittags 6 Uhr im Börsensaal (Nebenzimmer) zu Augsburg.", 
				item39Citation.get(CITATION_TITLE));
		assertEquals("Bericht des Vereins zum Schutze der Alpenpflanzen", item39Citation.get(CITATION_JOURNAL_NAME));
		assertEquals("15_1922", item39Citation.get(CITATION_ISSUE_NUMBER));
		assertEquals("14", item39Citation.get(CITATION_FIRST_PAGE));
		assertEquals("20", item39Citation.get(CITATION_LAST_PAGE));
	}
	
	@Test
	public void testHarvesterRun() throws IOException {
		DummyConfigurator configurator = setup();
		String startingUrl = "https://www.zobodat.at/publikation_series.php?id=7392";
		Path outputDirectoryPath = Paths.get(TEST_OUTPUT_DIRECTORY_STRING + "/zobodat/text");
		File[] createdFiles = {
				outputDirectoryPath.resolve("145618.pdf").toFile(),
				outputDirectoryPath.resolve("145616.pdf").toFile()
		};
		
		configurator.addItemToArray(ZobodatHarvester.ZOBODAT_STRING, ITEM_ARRAY, startingUrl);
		
		try {
			ZobodatHarvester zobodatHarvester = new ZobodatHarvester(
					configurator.getConfigurationForHarvesterName(ZobodatHarvester.ZOBODAT_STRING));
		
			zobodatHarvester.run();
			
			assertTrue(createdFiles[0].exists());
			assertTrue(createdFiles[1].exists());
		} finally {
			cleanAfterTest();
		}
	}
	
	private void areAllMetadataFieldsSerialized(JSONObject item) {
		assertTrue(item.has(METADATA_PDF_URL));
		assertTrue(item.has(METADATA_CITATION));
		JSONObject itemCitation = item.getJSONObject(METADATA_CITATION);
		assertTrue(itemCitation.has(CITATION_AUTHORS));
		assertTrue(itemCitation.has(CITATION_TITLE));
		assertTrue(itemCitation.has(CITATION_YEAR));
		assertTrue(itemCitation.has(CITATION_FIRST_PAGE));
		assertTrue(itemCitation.has(CITATION_LAST_PAGE));
		assertTrue(itemCitation.has(CITATION_JOURNAL_NAME));
	}
	
	private void cleanAfterTest() throws IOException {
		if (!didTestDirectoryExistBeforeTest && testDirectory.exists()) {
			FileUtils.deleteDirectory(testDirectory);
		}
	}
	
	private DummyConfigurator getConfigurator() throws IOException {
		String configurationFilePathString = LiteratureHarvester.CONFIGURATION_FILE_PATH_STRING;
		
		DummyConfigurator configurator = new DummyConfigurator();
		
		// Read Harvester configuration from config file and dump all items and collections
		configurator.readConfigurationYamlFile(configurationFilePathString);
		configurator.removeKeyFromConfiguration(ZobodatHarvester.ZOBODAT_STRING, ITEM_ARRAY);
		
		return configurator;
	}
	
	private DummyConfigurator setup() throws IOException {
		Harvester.setOutputDirectory(TEST_OUTPUT_DIRECTORY_STRING);
		
		testDirectory = Paths.get(TEST_OUTPUT_DIRECTORY_STRING).toFile();
		didTestDirectoryExistBeforeTest = testDirectory.exists();
		
		DummyConfigurator configurator = getConfigurator();
		return configurator;
	}
}
