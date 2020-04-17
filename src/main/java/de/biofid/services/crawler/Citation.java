package de.biofid.services.crawler;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/***
 * An abstract class holding the citation data for an item.
 * 
 * Its purpose is that of a container to improve readability. Hence all variables are public.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public abstract class Citation {
	private List<String> authors = new ArrayList<>();
	private String firstPage = ""; // No Integer, because page numbers can also be roman!
	private String issueNumber = "";
	private String journalName = "";
	private String lastPage = "";
	private String title = "";
	private int year = -1;
	
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
	
	public String getFirstPage() {
		return firstPage;
	}
	
	public String getIssueNumber() {
		return issueNumber;
	}
	
	public String getJournalName() {
		return journalName;
	}
	
	public String getLastPage() {
		return lastPage;
	}
	
	public int getPublicationYear() {
		return year;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setIssueNumber(String issueNumber) {
		this.issueNumber = issueNumber;
	}
	
	public void setJournalName(String journalName) {
		this.journalName = journalName;
	}
	
	public void setPages(String firstPage, String lastPage) {
		this.firstPage = firstPage;
		this.lastPage = lastPage;
	}
	
	public void setPublicationYear(int year) {
		this.year = year;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String toString() {
		return "Authors: " + authors.toString() + "\n Title: " + title + "\n Year: " + year + "\n"
				+ " First Page: " + firstPage + "\n Last Page: " + lastPage + "\n Journal: " + journalName
				+ "\n Issue number: " + issueNumber;			
	}
}
