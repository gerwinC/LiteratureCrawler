package de.biofid.services.crawler;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/***
 * A helper class holding the metadata of an item.
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Metadata {
	
	public static final String SERIALIZATION_NAME_ITEM_URL = "url";
	
	private Citation citation;
	private long id;
	private URL itemUrl;
	private URL pdfUrl;
	
	public Metadata(long id, URL pdfUrl, Citation citation) {
		this.pdfUrl = pdfUrl;
		this.citation = citation;
		this.id = id;
	}
	
	public Citation getCitation() {
		return citation;
	}
	
	public long getItemID() {
		return id;
	}
	
	@JsonProperty(SERIALIZATION_NAME_ITEM_URL)
	public URL getItemUrl() {
		return itemUrl;
	}
	
	@JsonIgnore
	public URL getPdfURL() {
		return pdfUrl;
	}
	
	public void setItemUrl(String url) throws MalformedURLException {
		setItemUrl(new URL(url));
	}
	
	public void setItemUrl(URL url) {
		this.itemUrl = url;
	}
}
