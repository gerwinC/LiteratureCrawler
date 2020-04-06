package de.biofid.services.crawler;

import org.json.JSONArray;
import org.json.JSONObject;

public class DummyConfigurator extends HarvesterConfigurator {

	public void removeKeyFromConfiguration(String harvesterName, String key) {
		Configuration conf = getConfigurationForName(harvesterName);
		if (conf != null) {
			JSONObject jsonConfig = conf.getHarvesterJsonConfiguration();
			jsonConfig.remove(key);
		}
	}
	
	public void addItemToArray(String harvesterName, String itemContainerName, Object item) {
		Configuration conf = getConfigurationForName(harvesterName);
		
		if (conf != null) {
			JSONObject jsonConfiguration = conf.getHarvesterJsonConfiguration();
			JSONArray jsonContainer;
			if (jsonConfiguration.has(itemContainerName)) {
				jsonContainer = jsonConfiguration.getJSONArray(itemContainerName);
			} else {
				jsonContainer = new JSONArray();
				jsonConfiguration.put(itemContainerName, jsonContainer);
			}
			
			jsonContainer.put(item);
		}
	}
	
	private Configuration getConfigurationForName(String harvesterName) {
		for (Configuration conf : configurations) {
			if (conf.getHarvesterName().equals(harvesterName)) {
				return conf;
			}
		}
		
		return null;
	}
}
