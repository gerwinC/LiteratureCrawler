package de.biofid.services.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.biofid.services.crawler.Item.DownloadFailedException;
import de.biofid.services.crawler.Item.UnsupportedOutputFormatException;

public class TestItem {
	
	private static final String TEST_DIRECTORY = "/tmp/test";
	private static final Path testDirectoryPath = Paths.get(TEST_DIRECTORY);
	private boolean didTestDirectoryExistsBeforeTest = false;
	
	@Test
	public void testSingleFileTextDownload() throws DownloadFailedException {
		setup();
		
		Item item = new Item();
		int itemId = 122536;
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itempdf/" + itemId, Item.FileType.PDF);
		item.setItemId(itemId);
		
		List<Path> createdTextFiles = new ArrayList<Path>();
		boolean overwriteExistingFiles = true;
		try {
			createdTextFiles = item.writeTextFiles(TEST_DIRECTORY, overwriteExistingFiles);
			assertTrue(createdTextFiles.toArray().length == 1);
			
			Path pathToCreatedFile = Paths.get(TEST_DIRECTORY + "/text/" + itemId + ".pdf");
			assertTrue(pathToCreatedFile.toFile().exists());
			
		} finally {
			cleanAfterTest(createdTextFiles);
		}	
	}

	@Test
	public void testMultiFileTextDownload() throws DownloadFailedException {
		setup();
		
		Item item = new Item();
		int itemId = 12345;
		// Examples taken from https://www.biodiversitylibrary.org/browse/collection/HistoryOfCats
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itempdf/93597", Item.FileType.PDF);
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itemtext/93597", Item.FileType.TXT);
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itempdf/122536", Item.FileType.PDF);
		
		item.setItemId(itemId);
		
		List<Path> createdTextFiles = new ArrayList<Path>();
		boolean overwriteExistingFiles = true;
		try {
			createdTextFiles = item.writeTextFiles(TEST_DIRECTORY, overwriteExistingFiles);
			assertTrue(createdTextFiles.toArray().length == 3);
			
			String[] fileNameComparisonArray = {
					itemId + ".pdf", itemId + ".txt", itemId + "-1.pdf"
			};
			
			Path textFilesOutputDirectory = Paths.get(TEST_DIRECTORY + "/text");
			
			for (int i = 0; i < fileNameComparisonArray.length; ++i) {
				Path createdFilePath = createdTextFiles.get(i);
				assertEquals(textFilesOutputDirectory, createdFilePath.getParent());
				assertEquals(fileNameComparisonArray[i], createdFilePath.getFileName().toString());
				assertTrue(createdFilePath.toFile().exists());
				
				removeTestFile(createdFilePath);
			}
			
		} finally {
			cleanAfterTest(createdTextFiles);
		}
	}
	
	@Test
	public void testWriteMetadataFile() throws UnsupportedOutputFormatException, 
												ParserConfigurationException, SAXException, 
												IOException {
		setup();
		
		// TODO: Add test for XML elements. They are not working yet.
		Item item = new Item();
		int itemID = 54321;
		String dummySource = "Institute of Silly Walks";
		String dummyItemUrl = "https://www.biofid.de";
		
		item = addMetadataExampleToItem(item);
		item.setItemId(itemID);
		item.setDataSource(dummySource);
		item.setItemUrl(dummyItemUrl);
		
		Path metadataFilePath = null;
		try {
			item.writeMetadataFile(TEST_DIRECTORY, Item.FileType.XML);
			
			metadataFilePath = Paths.get(TEST_DIRECTORY + "/metadata/" + itemID + ".xml");
			assertTrue(metadataFilePath.toFile().exists());
			
			Document xmlDocument = readXmlFile(metadataFilePath);
			
			// Check if all Metadata are written to the file
			assertEquals(1, xmlDocument.getElementsByTagName("Title").getLength());
			assertEquals("Love in the Time of Corona",
					xmlDocument.getElementsByTagName("Title").item(0).getTextContent());
			assertEquals(1, xmlDocument.getElementsByTagName("Publication-year").getLength());
			assertEquals("2020",
					xmlDocument.getElementsByTagName("Publication-year").item(0).getTextContent());
			
			assertEquals(1, xmlDocument.getElementsByTagName("Authors").getLength());
			NodeList authors = xmlDocument.getElementsByTagName("Author");
			assertEquals(2, authors.getLength());
			assertEquals("Fermina Daza", authors.item(0).getTextContent());
			assertEquals("Florentino Arizas", authors.item(1).getTextContent());
			
			NodeList keywordList = xmlDocument.getElementsByTagName("Keywords");
			assertEquals(1, keywordList.getLength());
			NodeList keywords = keywordList.item(0).getChildNodes();
			assertEquals(3, keywords.getLength());
			assertEquals("Drama", keywords.item(0).getTextContent());
			assertEquals("Crisis", keywords.item(1).getTextContent());
			assertEquals("Toilet Paper", keywords.item(2).getTextContent());
			
			assertEquals(Integer.toString(itemID), 
					xmlDocument.getElementsByTagName(Item.METADATA_ITEM_ID_STRING).item(0).getTextContent());
			assertEquals(dummySource, 
					xmlDocument.getElementsByTagName(Item.METADATA_ITEM_SOURCE_STRING).item(0).getTextContent());
			
			NodeList textUrlList = xmlDocument.getElementsByTagName(Item.METADATA_ITEM_TEXT_URLS_PARENT_STRING);
			assertEquals(1, textUrlList.getLength());
			
			String[] expectedTextUrls = {
					"https://www.biofid.de/item/1234",
					"https://www.biofid.de/item/9875.txt",
					"https://www.biofid.de/item/6254234"
			};
			
			Item.FileType[] expectedTextFileTypes = {
					Item.FileType.PDF,
					Item.FileType.TXT,
					Item.FileType.PDF
			};
			
			NodeList textUrlChildNodes = textUrlList.item(0).getChildNodes();
			assertEquals(3, textUrlChildNodes.getLength());
			
			for (int i = 0; i < textUrlChildNodes.getLength(); ++i) {
				Node node = textUrlChildNodes.item(i);
				assertEquals(expectedTextFileTypes[i].toString(),
						node.getFirstChild().getTextContent());
				assertEquals(expectedTextUrls[i],
						node.getLastChild().getTextContent());
			}
	
		} finally {
			cleanAfterTest(metadataFilePath);
		}
	}
	
	@Test
	public void testOverwriteAgenda() throws DownloadFailedException, IOException {
		setup();
		
		Item item = new Item();
		int itemId = 12345;
		// Examples taken from https://www.biodiversitylibrary.org/browse/collection/HistoryOfCats
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itempdf/93597", Item.FileType.PDF);
		item.addTextFileUrl("https://www.biodiversitylibrary.org/itemtext/93597", Item.FileType.TXT);
		
		item.setItemId(itemId);
		
		List<Path> createdTextFiles = new ArrayList<Path>();
		boolean overwriteExistingFiles = false;
		try {
			File textBaseFolder = testDirectoryPath.resolve("text").toFile();
			textBaseFolder.mkdirs();
			File dummyExistingFile = testDirectoryPath.resolve("text/12345.pdf").toFile();
			assertTrue(dummyExistingFile.createNewFile());
			
			createdTextFiles = item.writeTextFiles(TEST_DIRECTORY, overwriteExistingFiles);
			assertTrue(createdTextFiles.toArray().length == 1);

			Path createdFilePath = Paths.get(TEST_DIRECTORY + "/text/12345.txt");
			Path textFilesOutputDirectory = Paths.get(TEST_DIRECTORY + "/text");
			
			assertEquals(textFilesOutputDirectory, createdFilePath.getParent());
			assertTrue(createdFilePath.toFile().exists());
			
			removeTestFile(createdFilePath);
		} finally {
			cleanAfterTest(createdTextFiles);
		}
	}
	
	private Item addMetadataExampleToItem(Item item) {
		item.addMetdata("Title", "Love in the Time of Corona");
		item.addMetdata("Publication-year", 2020);
		
		JSONObject authorList = new JSONObject();
		String[] authorNames = {"Fermina Daza", "Florentino Arizas"};
		authorList.put("Author", authorNames);
		item.addMetadataToArray("Authors", authorList);
		
		String[] keywordList = {"Drama", "Crisis", "Toilet Paper"};
		JSONObject jsonKeyordList = new JSONObject();
		jsonKeyordList.put("Keyword", keywordList);
		item.addMetadataToArray("Keywords", jsonKeyordList);
		
		item.addTextFileUrl("https://www.biofid.de/item/1234", Item.FileType.PDF);
		item.addTextFileUrl("https://www.biofid.de/item/9875.txt", Item.FileType.TXT);
		item.addTextFileUrl("https://www.biofid.de/item/6254234", Item.FileType.PDF);
		
		return item;
	}
	
	private void cleanAfterTest(List<Path> filesToRemove) {
		for (Path filePath : filesToRemove) {
			removeTestFile(filePath);
		}
		
		if (!didTestDirectoryExistsBeforeTest && testDirectoryPath.toFile().exists()) {
			try {
				FileUtils.deleteDirectory(testDirectoryPath.toFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void cleanAfterTest(Path filePath) {
		ArrayList<Path> list = new ArrayList<Path>();
		if (filePath != null) {
			list.add(filePath);
		}
		
		cleanAfterTest(list);
	}
	
	private Document readXmlFile(Path filePath) throws ParserConfigurationException, 
														SAXException, IOException {
		File file = new File(filePath.toFile().toString());
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		return documentBuilder.parse(file);
	}
	
	private boolean removeTestFile(Path filePath) {
		return filePath.toFile().delete();
	}
	
	private void setup() {
		didTestDirectoryExistsBeforeTest = testDirectoryPath.toFile().exists();
	}
}
