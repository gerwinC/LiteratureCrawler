package de.biofid.services.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/***
 * A class holding all configurations given in a YAML file.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class HarvesterConfigurator {
	
	private static final String GENERAL_HARVESTING_DELAY = "request-delay";
	private static final String GENERAL_LOGGER_LEVEL = "logging-level";
	private static final String GENERAL_OUTPUT_PATH = "output-path";
	private static final String GENERAL_OVERWRITE_STRING = "overwrite";
	private static final String GENERAL_SETTINGS = "General";
	
	private static final String HARVESTER_API_KEY = "api-key";
	private static final String HARVESTER_CLASS_NAME = "class";
	private static final String HARVESTER_CONFIGURATIONS_PARENT = "Harvesters";
	
	private static final boolean OVERWRITTING_DEFAULT = true;
	
	protected Map<String, String> apiKeysForHarvesters = new HashMap<>();
	protected String baseOutputPathString = null;
	protected List<Configuration> configurations = new ArrayList<>();
	protected long delayBetweenRequestsInMilliseconds = 0;
	protected boolean isOverwrittingEnabled = true;
	protected String loggerLevel = "INFO";
	

	public String getBaseOutputPath() {
		return baseOutputPathString;
	}
	
	/***
	 * Returns the configuration set as defined in the configuration file for the given harvester.
	 * @param harvesterName The harvester name to look up.
	 * @return The configuration of the given harvester. Null, otherwise.
	 */
	public Configuration getConfigurationForHarvesterName(String harvesterName) {
		for (Configuration conf : configurations) {
			if (conf.getHarvesterName().equals(harvesterName)) {
				return conf;
			}
		}
		
		return null;
	}
	
	/***
	 * Returns a deep copy of the configuration list.
	 * @return A list of JSON objects, containing the configurations for the single harvesters.
	 */
	public List<Configuration> getConfigurations() {
		List<Configuration> newList = new ArrayList<>();
		
		for (Configuration config : configurations) {
			newList.add(new Configuration(config));
		}
		
		return newList;
	}
	
	public String getLoggerLevel() {
		return loggerLevel;
	}
	
	/***
	 * Reads a YAML file to configure the Harvesters.
	 * 
	 * @param configurationFileString The file path of the file to read.
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void readConfigurationYamlFile(String configurationFileString) throws IOException {
		File configurationFile = Paths.get(configurationFileString).toFile();
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		JsonNode configurationJson = mapper.readTree(configurationFile);
		
		JsonNode generalSettingsTree = configurationJson.get(GENERAL_SETTINGS);
		baseOutputPathString = generalSettingsTree.get(GENERAL_OUTPUT_PATH).asText();
		delayBetweenRequestsInMilliseconds = generalSettingsTree.get(GENERAL_HARVESTING_DELAY).asLong();
		
		if (generalSettingsTree.has(GENERAL_LOGGER_LEVEL)) {
			loggerLevel = generalSettingsTree.get(GENERAL_LOGGER_LEVEL).asText().toLowerCase();
		}
		
		if (generalSettingsTree.has(GENERAL_OVERWRITE_STRING)) {
			isOverwrittingEnabled = generalSettingsTree.get(GENERAL_OVERWRITE_STRING).asBoolean();
		}
		
		JsonNode harvesterConfigurationTree = configurationJson.get(HARVESTER_CONFIGURATIONS_PARENT);
		
		@SuppressWarnings("unchecked")
		List<LinkedHashMap> harvesterConfigurationList = new ObjectMapper().convertValue(
				harvesterConfigurationTree, ArrayList.class);
		
		for (LinkedHashMap harvesterConfiguration : harvesterConfigurationList) {
			String harvesterName = (String) harvesterConfiguration.keySet().toArray()[0];		
			
			JSONObject jsonConfiguration = new JSONArray(harvesterConfiguration.values()).getJSONObject(0);
			String harvesterClassName = jsonConfiguration.getString(HARVESTER_CLASS_NAME);
			
			if (jsonConfiguration.has(HARVESTER_API_KEY)) {
				apiKeysForHarvesters.put(harvesterName, getApiKey(jsonConfiguration));
			}
			
			Configuration config = new Configuration(harvesterName, harvesterClassName, jsonConfiguration);
			config.setHarvesterApiKey(getApiKey(jsonConfiguration));
			config.setOverwritting(getOverwrittingPolicyForHarvester(jsonConfiguration));
			
			configurations.add(config);
		}
	}
	
	private String getApiKey(JSONObject jsonConfiguration) {
		String apiKey = null;
		if (!jsonConfiguration.has(HARVESTER_API_KEY)) {
			return apiKey;
		}
		String apiKeyString = jsonConfiguration.getString(HARVESTER_API_KEY);
    	if (isStringPathOrFile(apiKeyString)) {
    		apiKey = readApiKey(apiKeyString);
    	} else {
    		apiKey = apiKeyString;
    	}
    	return apiKey;
	}
	
	private boolean getOverwrittingPolicyForHarvester(JSONObject jsonConfiguration) {
		boolean isOverWrittingEnabled = OVERWRITTING_DEFAULT;
		
		if (jsonConfiguration.has(GENERAL_OVERWRITE_STRING)) {
			isOverWrittingEnabled = jsonConfiguration.getBoolean(GENERAL_OVERWRITE_STRING);
		}
		
		return isOverWrittingEnabled;
	}
	
	private boolean isStringPathOrFile(String pathOrFile) {
        try {
            Paths.get(pathOrFile);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }
    
	private String readApiKey(String apiSourceFile) {
    	String apiKey = null;
    	URI filePath = Paths.get(apiSourceFile).toUri();
    	
    	try {
			File file = new File(filePath);
			Scanner scanner = new Scanner(file);
			
			apiKey = scanner.nextLine();
		
			scanner.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("Could not find file " + filePath.toString());
		}
    	
		return apiKey;
    }
}
