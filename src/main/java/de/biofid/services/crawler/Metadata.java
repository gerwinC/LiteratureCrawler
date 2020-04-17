package de.biofid.services.crawler;

import java.net.URL;

/***
 * A helper class holding the metadata of an item.
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Metadata {
	public Citation citation;
	public long id;
	public URL pdfUrl;
	
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
	
	public URL getPdfURL() {
		return pdfUrl;
	}
}
