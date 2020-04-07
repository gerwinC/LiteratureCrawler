package de.biofid.services.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/***
 * A Harvester to crawl Zobodat.at literature.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class ZobodatHarvester extends Harvester {
	
	public static final String ZOBODAT_LITERATURE_BASE_URL = "https://www.zobodat.at/publikation_series.php";
	public static final String ZOBODAT_STRING = "Zobodat";
	
	private static final String ATTRIBUTE_HREF = "href";
	
	private static final String HYPERLINK_REFERENCE_TO_AUTHOR = "personen.php";
	private static final String HYPERLINK_REFERENCE_TO_PUBLICATION_NAME = "publikation_series.php";
	private static final String HYPERLINK_REFERENCE_TO_PUBLICATION_VOLUME = "publikation_volumes.php";
	
	private static final String ITEM_COMPLETE_METADATA = "Item";
	private static final String CONFIGURATION_ITEM_LIST = "items";
	
	private static final Pattern REGEX_PATTERN_AUTHOR_AND_YEAR = Pattern.compile("^(.*?) ?\\(([0-9]{4})\\)");
	private static final Pattern REGEX_PATTERN_ISSUE_NUMBER = Pattern.compile("– (.*?): ");
	private static final Pattern REGEX_PATTERN_ITEM_ID_IN_ZOBODAT_URL = Pattern.compile("\\?id=([0-9]*)");
	private static final Pattern REGEX_PATTERN_PAGES = Pattern.compile(": ([XI0-9]*?) - ([XI0-9]*?)\\.$");
	private static final Pattern REGEX_PATTERN_TITLE_AND_JOURNAL_NAME = Pattern.compile("\\([0-9]{4}\\): (.*?) – (.*) – ");
	
	private static final String SELECTOR_CITATION_CONTAINER = "#publikation_articles .text";
	private static final String SELECTOR_CONTENT = "div.content";
	private static final String SELECTOR_DIV = "div";
	private static final String SELECTOR_HYPERLINKS = "a";
	private static final String SELECTOR_ITEM_FROM_DOCUMENT_LIST = "ul.search-results-list li.result";
	private static final String SELECTOR_ITEM_URL = ".content a.red";
	private static final String SELECTOR_PUBLICATION_LINK = "a.publication-link";
	
	private static final String ZOBODAT_URL = "https://www.zobodat.at";
	
	private boolean isMetadataCollected = false;
	private Iterator<Metadata> itemMetadataIterator = null;
	private List<Metadata> itemMetadataList = new ArrayList<>();
	
	private List<Object> listOfItemsToDownload = new ArrayList<>();

	public ZobodatHarvester(Configuration configuration) throws UnsetHarvesterBaseDirectoryException {
		super(configuration);
		
		JSONObject jsonConfiguration = configuration.getHarvesterJsonConfiguration();
		
		if (jsonConfiguration.has(CONFIGURATION_ITEM_LIST)) {
    		JSONArray itemListFromConfiguration = jsonConfiguration.getJSONArray(CONFIGURATION_ITEM_LIST);
    		listOfItemsToDownload = itemListFromConfiguration.toList();
    	}
	}
	
	public Document getDocumentFromUrl(String url) throws IOException {
		return Jsoup.connect(url).get();
	}
	
	public String getFolderName() {
		return ZOBODAT_STRING;
	}
	
	public Elements getItemListFromWebsite(Document website) {
		return website.select(SELECTOR_ITEM_FROM_DOCUMENT_LIST);
	}
	
	public JSONArray getMetadataListAsJSONArray() {
		return toJSONArray(itemMetadataList);
	}
	
	public int getMetadataListSize() {
		return itemMetadataList.size();
	}
	
	/***
	 * Run over a given list of item elements from the Zobodat page.
	 * 
	 * This function finds out, if the given list represents a list of articles or if it needs
	 * to delve deeper. If the latter, all given items are crawled recursively.
	 * If it is a list with articles, their metadata are extracted and stored.
	 * @param itemList A list of items from the Zobodat page.
	 */
	public void iterateItems(Elements itemList) {
		int sumOfAllItemsReferencedFromThisSite = 0;
		
		for (Element item : itemList) {
			String urlString = item.select(SELECTOR_ITEM_URL).attr(ATTRIBUTE_HREF);
			urlString = generateZobodatUrlStringFromString(urlString);
			
			if (!urlString.isEmpty()) {
				sumOfAllItemsReferencedFromThisSite = crawlUrlRecursively(urlString);
			}
		}
		
		if (sumOfAllItemsReferencedFromThisSite == 0) {
			logger.fine("Is article list!");
			itemMetadataList.addAll(extractItemMetadataFromArticleList(itemList));
		}
	}
	
	public boolean nextItem(Item item) {
		if (!isMetadataCollected) {
			logger.info("Start crawling metadata!");
			if (!listOfItemsToDownload.isEmpty()) {
				for (Object obj : listOfItemsToDownload) {
					String itemUrl = (String) obj;
					crawlUrlRecursively(itemUrl);
				}
			} else {
				crawlUrlRecursively(ZOBODAT_LITERATURE_BASE_URL);
			}
			isMetadataCollected = true;
			itemMetadataIterator = itemMetadataList.iterator();
			logger.info("Crawling of metadata complete!");
		}
		
		if (itemMetadataIterator.hasNext()) {
			Metadata itemMetadata = itemMetadataIterator.next();
			addMetadataToItem(item, itemMetadata);
		} else {
			return false;
		}
		
		return true;
	}
	
	private void addMetadataToItem(Item item, Metadata itemMetadata) {
		ObjectMapper mapper = new ObjectMapper();
		String metdataJSONString;
		try {
			metdataJSONString = mapper.writeValueAsString(itemMetadata);
		} catch (JsonProcessingException e) {
			logger.warning("Could not create metadata JSON from item " + item.getItemId());
			return;
		}
		
		JSONObject itemMetadataJSON = new JSONObject(metdataJSONString);
		
		long itemID = Long.parseLong(itemMetadataJSON.remove("id").toString());
    	logger.fine("Processing Item ID " + itemID);
		item.setDataSource(ZOBODAT_STRING);
		item.setItemId(itemID);
		item.addTextFileUrl((String) itemMetadataJSON.remove("pdfUrl"), Item.FileType.PDF);
		item.addMetdata(ITEM_COMPLETE_METADATA, itemMetadataJSON);
	}
	
	private int crawlUrlRecursively(String url) {
		if (url.isEmpty()) {
			return 0;
		}
		
		pause();
		
		logger.info("Processing URL " + url + "");
		
		Document website;
		try {
			website = getDocumentFromUrl(url);
		} catch (IOException ex) {
			logger.warning("Could not fetch URL " + url);
			return 0;
		}
		
		Elements itemList = getItemListFromWebsite(website);
		iterateItems(itemList);
		
		return itemList.size();
	}
	
	private List<Metadata> extractItemMetadataFromArticleList(Elements itemList) {
		List<Metadata> metadataList = new ArrayList<>();
		
		logger.info("Found " + itemList.size() + " items on this site!");
		
		for (Element item : itemList) {
			URL itemPdfUrl = getItemPdfUrl(item);
			
			if (itemPdfUrl != null) {
				URL citationUrl = getCitationUrl(item);
				long itemID = getItemIDFromUrl(citationUrl);
				Citation citation = getCitationFromUrl(citationUrl);
				metadataList.add(new Metadata(itemID, itemPdfUrl, citation));
			}
		}
		
		
		return metadataList;
	}
	
	private String generateZobodatUrlStringFromString(String url) {
		if (url.isEmpty()) {
			return url;
		}
		
		if (url.startsWith(ZOBODAT_URL)) {
			return url;
		} else if (!url.startsWith("/")) {
			url = "/" + url;
		}
		
		return ZOBODAT_URL + url;
	}
	
	private Citation getCitationFromUrl(URL url) {
		Citation citation;
		try {
			Document citationSite = getDocumentFromUrl(url.toString());
			Element citationContainer = citationSite.selectFirst(SELECTOR_CITATION_CONTAINER);
			citation = new Citation(citationContainer);
			logger.finer("Generated citation: " + citation.toString());
		} catch (IOException e) {
			logger.warning("Could not collect citation site: " + url.toString());
			citation = null;
		}
		
		return citation;
	}
	
	private URL getCitationUrl(Element item) {
		Element contentContainer = item.selectFirst(SELECTOR_CONTENT);
		Element citationLinkElement = contentContainer.select(SELECTOR_DIV).last();
		String citationUrlString = generateZobodatUrlStringFromString(
				citationLinkElement.select(SELECTOR_HYPERLINKS).attr(ATTRIBUTE_HREF));
		URL citationUrl;
		try {
			citationUrl = new URL(citationUrlString);
			logger.fine("Found citation URL: " + citationUrlString);
		} catch (MalformedURLException e) {
			logger.warning("Malformed Citation URL: " + citationUrlString);
			citationUrl = null;
		}
		
		return citationUrl;
	}
	
	private long getItemIDFromUrl(URL url) {
		Matcher itemIdMatcher = REGEX_PATTERN_ITEM_ID_IN_ZOBODAT_URL.matcher(url.toString());
		
		long itemID = -1;
		if (itemIdMatcher.find()) {
			 itemID = Long.parseLong(itemIdMatcher.group(1));
		}
		
		return itemID;
	}
	
	private URL getItemPdfUrl(Element item) {
		Element pdfElement = item.selectFirst(SELECTOR_PUBLICATION_LINK);
		if (pdfElement != null) {
			String pdfUrlSubString = pdfElement.attr(ATTRIBUTE_HREF);
			String pdfUrlString = generateZobodatUrlStringFromString(pdfUrlSubString);
			URL pdfUrl;
			try {
				pdfUrl = new URL(pdfUrlString);
				logger.fine("Found PDF URL: " + pdfUrlString);
			} catch (MalformedURLException e) {
				logger.warning("Malformed PDF URL: " + pdfUrlString);
				pdfUrl = null;
			}
			
			return pdfUrl;
		}
		
		return null;
	}
	
	private JSONArray toJSONArray(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonStringOfList = mapper.writeValueAsString(obj);
			return new JSONArray(jsonStringOfList);
		} catch (JsonProcessingException ex) {
			logger.warning("Could not convert item metadata list to JSON!");
		}
		
		return null;
	}
	
	private class Citation {
		public List<String> authors = new ArrayList<>();
		public String firstPage = ""; // No Integer, because page numbers can also be roman!
		public String issueNumber = "";
		public String journalName = "";
		public String lastPage = "";
		public String title = "";
		public int year = -1;
		
		private final Pattern authorAndYearPattern = ZobodatHarvester.REGEX_PATTERN_AUTHOR_AND_YEAR;
		private final Pattern issueNumberPattern = ZobodatHarvester.REGEX_PATTERN_ISSUE_NUMBER;
		private final Pattern pagesPattern = ZobodatHarvester.REGEX_PATTERN_PAGES;
		private final Pattern titleAndJournalNamePattern = ZobodatHarvester.REGEX_PATTERN_TITLE_AND_JOURNAL_NAME;
		
		public Citation(Element citationContainer) {
			parseHyperlinkTextsToRespectiveClassField(citationContainer);
			tryToFillEmptyClassFieldsFromContainerText(citationContainer);
		}
		
		public void addAuthor(String author) {
			if (!this.authors.contains(author)) {
				author = author.trim();
				this.authors.add(author);
			}
		}
		
		public void addAuthors(String[] authors) {
			for (String author : authors) {
				addAuthor(author);
			}
		}
		
		public String toString() {
			return "Authors: " + authors.toString() + "\n Title: " + title + "\n Year: " + year + "\n"
					+ " First Page: " + firstPage + "\n Last Page: " + lastPage + "\n Journal: " + journalName
					+ "\n Issue number: " + issueNumber;			
		}
		
		private boolean doesHyperlinkReferenceContainSubstring(Element hyperlink, String subString) {
			return hyperlink.attr(ATTRIBUTE_HREF).contains(subString);
		}
		
		private void parseHyperlinkTextsToRespectiveClassField(Element citationContainer) {
			Elements links = citationContainer.select(SELECTOR_HYPERLINKS);
			
			for (Element link : links) {
				if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_AUTHOR)) {
					authors.add(link.text());
				} else if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_PUBLICATION_NAME)) {
					journalName = link.text();
				} else if (doesHyperlinkReferenceContainSubstring(link, HYPERLINK_REFERENCE_TO_PUBLICATION_VOLUME)) {
					issueNumber = link.text();
				}
			}
		}
		
		private void tryToFillEmptyClassFieldsFromContainerText(Element citationContainer) {
			String citationText = citationContainer.text();
			
			// It appears at Zobodat that a single author in the author list is linked, but the others not.
			// Hence, we cannot simply check, if the author list is not empty!
			Matcher authorAndYearMatcher = authorAndYearPattern.matcher(citationText);
			while (authorAndYearMatcher.find()) {
				String authorsString = authorAndYearMatcher.group(1);
				addAuthors(authorsString.split(","));
				year = Integer.parseInt(authorAndYearMatcher.group(2));
			}
			
			if (issueNumber.isEmpty()) {
				Matcher issueNumberMatcher = issueNumberPattern.matcher(citationText);
				while (issueNumberMatcher.find()) {
					issueNumber = issueNumberMatcher.group(1);
				}
			}
			
			Matcher pageMatcher = pagesPattern.matcher(citationText);
			while (pageMatcher.find()) {
				firstPage = pageMatcher.group(1);
				lastPage = pageMatcher.group(2);
			}
			
			Matcher titleAndJournalNameMatcher = titleAndJournalNamePattern.matcher(citationText);
			while (titleAndJournalNameMatcher.find()) {
				title = titleAndJournalNameMatcher.group(1);
				journalName = titleAndJournalNameMatcher.group(2);
			}	
		}
	}
	
	private class Metadata {
		public Citation citation;
		public long id;
		public URL pdfUrl;
		
		public Metadata(long id, URL pdfUrl, Citation citation) {
			this.pdfUrl = pdfUrl;
			this.citation = citation;
			this.id = id;
		}
	}
}
