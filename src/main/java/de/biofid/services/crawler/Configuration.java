package de.biofid.services.crawler;

import org.json.JSONObject;

/***
 * A single configuration holding all necessary settings for a single {@link de.biofid.services.crawler.Harvester}.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Configuration {
	
	private String apiKey = null;
	private long delayBetweenRequestsInMilliseconds = 0;
	private String harvesterClassName;
	private String harvesterName;
	private boolean isOverwrittingEnabled = true;
	private JSONObject jsonConfiguration;
	
	public Configuration(Configuration conf) {
		this.harvesterName = conf.harvesterName;
		this.harvesterClassName = conf.harvesterClassName;
		this.jsonConfiguration = new JSONObject(conf.jsonConfiguration.toString());
		this.apiKey = conf.apiKey;
		this.isOverwrittingEnabled = conf.isOverwrittingEnabled();
		this.delayBetweenRequestsInMilliseconds = conf.getRequestDelay();
	}
	
	public Configuration(String harvesterName, String harvesterClassName, JSONObject jsonConfiguration) {
		this.harvesterName = harvesterName;
		this.harvesterClassName = harvesterClassName;
		this.jsonConfiguration = jsonConfiguration;
	}
	
	public String getHarvesterApiKey() {
		return apiKey;
	}
	
	public String getHarvesterClassName() {
		return harvesterClassName;
	}
	
	public JSONObject getHarvesterJsonConfiguration() {
		return jsonConfiguration;
	}
	
	public String getHarvesterName() {
		return harvesterName;
	}
	
	public long getRequestDelay() {
		return delayBetweenRequestsInMilliseconds;
	}
	
	public boolean isOverwrittingEnabled() {
		return isOverwrittingEnabled;
	}
	
	public void setHarvesterApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public void setOverwritting(boolean isOverwrittingEnabled) {
		this.isOverwrittingEnabled = isOverwrittingEnabled;
	}
	
	public void setRequestDelay(long delayInMilliseconds) {
		this.delayBetweenRequestsInMilliseconds = delayInMilliseconds;
	}
}
