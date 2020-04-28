package de.biofid.services.crawler;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TestFileHandler {
	
	@Test
	public void testReadListFromFile() {
		String sourceFilePathString = "src/test/resources/listOfTitles.txt";
		List<String> listOfItems = FileHandler.readListFromFile(sourceFilePathString);
		assertEquals(21, listOfItems.size());
		assertEquals("60", listOfItems.get(0));
		assertEquals("64", listOfItems.get(1));
		assertEquals("2105", listOfItems.get(11));
		assertEquals("5540", listOfItems.get(listOfItems.size() - 1));
	}
}
