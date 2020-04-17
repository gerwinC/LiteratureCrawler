package de.biofid.services.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;

/***
 * A class to harvest the non-semantic HTML data from the Botanical Garden of Madrid (https://bibdigital.rjb.csic.es/)
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class BibDigitalHarvester extends Harvester {
	public static final String BIB_DIGITAL_HARVESTER = "bib-digital-madrid";
	
	private static final String ATTRIBUTE_HREF = "href";
	
	private static final String BIB_DIGITAL_BASE_URL = "https://bibdigital.rjb.csic.es";
	private static final String BOTANICAL_GARDEN_MADRID_STRING = "Real Jardín Botánico";
	
	private static final String CLASS_DOWNLOADABLE_FILES_STRING = "attachedfiles";
	private static final String CLASS_KEY_STRING = "key";
	private static final String CLASS_VALUE_STRING = "value";
	
	private static final String FULL_PDF_STRING = "Full PDF";
	
	private static final String ITEM_URL_SUFFIX_STRING = "-redirection";
	private static final String ITEM_URL_TEMPATE = BIB_DIGITAL_BASE_URL + "/en/records/item/";
	
	private static final String METADATA_AUTHOR_KEY = "Author";
	private static final String METADATA_PUBLICATION_DATE = "Date";
	private static final String METADATA_TITLE_KEY = "Title comp";
	
	private static final String TAG_NAME_CONTAINING_METADATA = "tr";
	private static final String TAG_NAME_LIST_ELEMENT = "li";
	
	private List<Object> listOfItemsToDownload = new ArrayList<>();
	private Iterator<Object> itemIterator = null;
	
	public BibDigitalHarvester(Configuration configuration) throws UnsetHarvesterBaseDirectoryException {
		super(configuration);
	}
	
	public void addItemsToCollect(List<String> itemList) {
		listOfItemsToDownload.addAll(itemList);
	}
	
	public void addItemToCollect(URL itemUrl) {
		String urlString = itemUrl.toString();
		int slashIndex = urlString.lastIndexOf("/") + 1;
		listOfItemsToDownload.add(urlString.substring(slashIndex));
	}
	
	public Document getDocumentFromUrl(String url) throws IOException {
		String urlHtmlContent = openSSLConnection(new URL(url));
		return Jsoup.parse(urlHtmlContent);
	}

	@Override
	protected String getFolderName() {
		return BIB_DIGITAL_HARVESTER;
	}

	@Override
	protected boolean nextItem(Item item) {
		if (itemIterator == null) {
			itemIterator = listOfItemsToDownload.iterator();
		}
		
		while (itemIterator.hasNext()) {
			Object itemObj = itemIterator.next();
			long itemId = Long.parseLong(itemObj.toString());
			
			logger.info("Processing item ID {}", itemId);
			
			try {
				Metadata metadata = getItemMetadata(itemId);
				logger.debug("Found metadata!\n{}", metadata);
				
				addMetadataToItem(item, metadata);
				return true;
			} catch (IOException ex) {
				ex.printStackTrace();
				logger.warn("An error happend while processing item ID {}\n{} ", itemId, ex.getLocalizedMessage());
			}
		}
		
		return false;
	}
	
	private void addMetadataToItem(Item item, Metadata itemMetadata) {
    	long itemID = itemMetadata.getItemID();
    	logger.debug("Processing Item ID {}", itemID);
    	
		item.setDataSource(BOTANICAL_GARDEN_MADRID_STRING);
		item.setItemId(itemID);
		item.addTextFileUrl(itemMetadata.getPdfURL().toString(), Item.FileType.PDF);
	
		try {
			item.addMetdata(ITEM_COMPLETE_METADATA, toJsonObject(itemMetadata));
		} catch (JsonProcessingException e) {
			logger.warn("Could not write metadata for item ID {}", itemID);
		}
    }
	
	private String constructItemUrlString(long itemId) {
		return ITEM_URL_TEMPATE + itemId + ITEM_URL_SUFFIX_STRING;
	}
	
	private Metadata extractItemMetadataFromHtmlDocument(long itemId, Document itemHtmlDocument) 
			throws IOException {
		Elements metadataElements = getMetadataElementsFromHtmlDocument(itemHtmlDocument);
		String itemTitle = extractMetadataAttributeFromMetadataElements(METADATA_TITLE_KEY, metadataElements);
		String itemAuthor = extractMetadataAttributeFromMetadataElements(METADATA_AUTHOR_KEY, metadataElements);
		String itemPublicationDate = extractMetadataAttributeFromMetadataElements(
				METADATA_PUBLICATION_DATE, metadataElements);
		URL pdfUrl = extractPdfUrlFromDocument(itemHtmlDocument);
		
		Citation citation = new BibDigitalCitation();
		citation.setTitle(itemTitle);
		citation.addAuthor(itemAuthor);
		citation.setPublicationYear(Integer.parseInt(itemPublicationDate));
		
		return new Metadata(itemId, pdfUrl, citation);
	}
	
	private String extractMetadataAttributeFromMetadataElements(String attributeName, 
			Elements metadataList) {
		for (Element metadataNode : metadataList) {
			Element keyStringNode = metadataNode.getElementsByClass(CLASS_KEY_STRING).first();
			if (keyStringNode != null && keyStringNode.text().equals(attributeName)) {
				Element valueNode = metadataNode.getElementsByClass(CLASS_VALUE_STRING).first();
				if (valueNode != null) {
					return valueNode.text();
				}
				
				return null;
			}
		}
		
		return null;
	}
	
	private URL extractPdfUrlFromDocument(Document document) throws IOException {
		Element downloadableFileListNode = document.getElementsByClass(
				CLASS_DOWNLOADABLE_FILES_STRING).first();
		for (Element fileNode : downloadableFileListNode.getElementsByTag(TAG_NAME_LIST_ELEMENT)) {
			if (fileNode.text().contains(FULL_PDF_STRING)) {
				String pdfUrl = fileNode.attr(ATTRIBUTE_HREF);
				if (!pdfUrl.isEmpty()) {
					return new URL(BIB_DIGITAL_BASE_URL + pdfUrl);
				} else {
					break;
				}
			}
		}
		
		throw new PdfNotAvailableException("The PDF file could not be found!");
	}
	
	private Metadata getItemMetadata(long itemId) throws IOException {
		String itemUrlString = constructItemUrlString(itemId);
		Document itemHtmlDocument = getDocumentFromUrl(itemUrlString);
		return extractItemMetadataFromHtmlDocument(itemId, itemHtmlDocument);
	}
	
	private Elements getMetadataElementsFromHtmlDocument(Document document) {
		return document.getElementsByTag(TAG_NAME_CONTAINING_METADATA);
	}
	
	private String openSSLConnection(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        InputStream is = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String inputLine;
        String outputString = "";
        
        while ((inputLine = br.readLine()) != null) {
            outputString += inputLine;
        }

        br.close();
        
        return outputString;
	}
	
	
	private class BibDigitalCitation extends Citation {
		
	}
	
	private class PdfNotAvailableException extends IOException {
		private static final long serialVersionUID = 7269829332901886792L;

		public PdfNotAvailableException(String msg) {
			super(msg);
		}
	}
}
