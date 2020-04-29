package de.biofid.services.crawler;

import static org.texttechnologylab.utilities.helper.RESTUtils.METHODS.GET;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.texttechnologylab.utilities.helper.RESTUtils;

import com.goebl.david.WebbException;

/***
 * A Harvester to crawl the biodiversity heritage library (BHL).
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author Giuseppe Abrami (TextTechnologyLab, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class BhlHarvester extends Harvester {
	
	public static final String BHL_STRING = "BHL";

    private static final String API_KEY = "apikey";
    
    private static final String BHL_API_URL = "https://www.biodiversitylibrary.org/api3";
    
    private static final String COLLECTION = "collection";
    private static final String COLLECTION_DESCRIPTION = "CollectionDescription";
    private static final String COLLECTION_ID = "CollectionID";
    private static final String COLLECTION_NAME = "CollectionName";
    
    private static final String CONFIGURATION_ITEM_LIST = "items";
    private static final String CONFIGURATION_TITLE_LIST = "titles";
    
    // BHL OAI Parameters
    // Documentation at https://www.biodiversitylibrary.org/docs/api3.html
    private static final String EXTERNAL_URL = "ExternalUrl";
    private static final String FORMAT = "format";
    private static final String GET_COLLECTIONS = "GetCollections";
    private static final String GET_ITEM_METADATA = "GetItemMetadata";
    private static final String GET_TITLE_METADATA = "GetTitleMetadata";
    private static final String ID = "id";
    private static final String ITEM_COMPLETE_METADATA = "Item";
    private static final String ITEM_ID = "ItemID";
    private static final String ITEM_PDF_URL = "ItemPDFUrl";
    private static final String ITEM_TXT_URL = "ItemTextUrl";
    private static final String ITEMS = "Items";
    private static final String JSON_FORMAT = "json";
    private static final String LANGUAGE = "language";
    private static final String OCR = "ocr";
    private static final String OPERATION = "op";
    private static final String PAGE = "page";
    private static final String PAGES = "pages";
    private static final String PARTS = "parts";
    private static final String SOURCE = "Source";
    
    // API Result Tags
    private static final String REQUEST_OK = "ok";
    private static final String REQUEST_RESULT = "Result";
    private static final String REQUEST_STATUS = "Status";
    private static final String REQUEST_UNAUTHORIZED = "Unauthorized";
    
    private String apiKey;
    private List<Object> listOfItemsToDownload = new ArrayList<>();
    private Iterator<Object> itemListIterator = null;
    
	public BhlHarvester(Configuration configuration)
    		throws UnsetHarvesterBaseDirectoryException {
    	super(configuration);
    	
    	JSONObject jsonConfiguration = configuration.getHarvesterJsonConfiguration();
    	
    	apiKey = configuration.getHarvesterApiKey();
    	
    	if (jsonConfiguration.has(CONFIGURATION_ITEM_LIST)) {
    		listOfItemsToDownload = getListFromJsonKey(CONFIGURATION_ITEM_LIST, jsonConfiguration);
    	}
    	
    	if (jsonConfiguration.has(CONFIGURATION_TITLE_LIST)) {
    		List<Object> titles = getListFromJsonKey(CONFIGURATION_TITLE_LIST, jsonConfiguration);
    		List<Object> itemsExtractedFromTitle = new ArrayList<>();
    		for (Object titleObj : titles) {
    			long titleID = Long.parseLong(titleObj.toString());
    			try {
					itemsExtractedFromTitle.addAll(getItemsFromTitle(titleID));
				} catch (ItemDoesNotExistException ex) {
					logger.error("The given Title ID {} could not be found!", titleID);
				} catch (AuthenticationException ex) {
					logger.fatal(ex.getLocalizedMessage());
				}
    		}
    		listOfItemsToDownload.addAll(itemsExtractedFromTitle);
    	}
    }

    /***
     * Retrieve all collections listed in BHL.
     * 
     * This is a call to https://www.biodiversitylibrary.org/api3?format=json&op=GetCollections&apikey=<KEY>
     * @return A map with the Collection IDs as keys and the name and description as values.
     */
    public Map<Long, JSONObject> getAllCollections(){
    	
        Map<Long, JSONObject> collectionMap = new HashMap<>(0);
        Map<String, Object> params = new HashMap<>(0);

        params.put(API_KEY, apiKey);
        params.put(FORMAT, JSON_FORMAT);
        params.put(OPERATION, GET_COLLECTIONS);

        JSONObject rObject = RESTUtils.getObjectFromRest(BHL_API_URL, GET, params);

        if(rObject.has(REQUEST_STATUS) && rObject.getString(REQUEST_STATUS).equalsIgnoreCase(REQUEST_OK)) {
            JSONArray collectionsJSON = rObject.getJSONArray(REQUEST_RESULT);
            for (int i = 0; i < collectionsJSON.length(); i++) {
                JSONObject jsonCollection = collectionsJSON.getJSONObject(i);
                collectionMap.put(jsonCollection.getLong(COLLECTION_ID),
                        new JSONObject().put(jsonCollection.getString(COLLECTION_NAME), 
                        		jsonCollection.getString(COLLECTION_DESCRIPTION))
                );
            }
        }

        return collectionMap;
    }

    /***
     * Retrieve the metadata of a single item.
     * The result will contain the page and parts metadata, but no OCR.
     * @param itemID The item id as given by BHL (https://www.biodiversitylibrary.org/item/<itemID>).
     * @return A JSONObject with the received data.
     * @throws AuthenticationException
     * @throws ItemDoesNotExistException
     */
    public JSONObject getItemMetadata(long itemID) 
    		throws AuthenticationException, ItemDoesNotExistException {
    	return getItemMetadata(itemID, true, false, true);
    }
    
    /***
     * Retrieve the metadata of a single item.
     * @param itemID The item id as given by BHL (https://www.biodiversitylibrary.org/item/<itemID>).
     * @param wantsPages If the page metadata shall be returned.
     * @param wantsOcr If the OCR should be returned.
     * @param wantsParts If the document parts (if available) should be returned.
     * @return A JSONObject with the received data.
     * @throws AuthenticationException
     * @throws ItemDoesNotExistException
     */
    public JSONObject getItemMetadata(long itemID, boolean wantsPages, boolean wantsOcr, boolean wantsParts) 
    		throws AuthenticationException, ItemDoesNotExistException {
    	logger.debug("Calling for metadata for Item ID {}", itemID);
    	
    	Map<String, Object> params = new HashMap<>();
    	params.put(API_KEY, apiKey);
    	params.put(FORMAT, JSON_FORMAT);
    	params.put(OPERATION, GET_ITEM_METADATA);
    	params.put(PAGES, wantsPages);
    	params.put(OCR, wantsOcr);
    	params.put(PARTS, wantsParts);
    	params.put(ID, itemID);
    	
    	JSONObject itemJson;
    	try {
    		JSONObject apiResponse = RESTUtils.getObjectFromRest(BHL_API_URL, GET, params);
    		itemJson = getApiResultObject(apiResponse);	
    	} catch (ItemDoesNotExistException ex) {
    		throw new ItemDoesNotExistException("The item with the ID " + itemID + "could not be found!");
    	} catch (WebbException ex) {
    		handleWebbException(ex);
    		return null;
    	}
    	
    	return itemJson;
    }
    
    /***
     * This is a call to https://www.biodiversitylibrary.org/api3?op=PublicationSearchAdvanced&language=GERMAN&collection=97&apikey=<KEY>&format=json
     * @param lCollectionID
     * @param sLanguage
     * @return
     * @throws AuthenticationException
     */
    public Map<Long, String> getItemsForCollection(long lCollectionID, String sLanguage) 
    		throws AuthenticationException {

        Map<Long, String> rMap = new HashMap<>(0);
        Map<String, Object> params = new HashMap<>(0);

        params.put(API_KEY, apiKey);
        params.put(FORMAT, JSON_FORMAT);
        params.put(OPERATION, "PublicationSearchAdvanced");
        params.put(LANGUAGE, sLanguage);
        params.put(COLLECTION, lCollectionID);

        boolean run = true;
        int currentPage = 1;

        while(run) {
            params.put(PAGE, currentPage);

            try {
                JSONObject rObject = RESTUtils.getObjectFromRest(BHL_API_URL, GET, params);

                if (rObject.has(REQUEST_STATUS) && 
                		rObject.getString(REQUEST_STATUS).equalsIgnoreCase(REQUEST_OK)) {
 
                    JSONArray rArray = rObject.getJSONArray(REQUEST_RESULT);

                    run = rArray.length() > 0;

                    for (int i = 0; i < rArray.length(); i++) {
                        JSONObject tObject = rArray.getJSONObject(i);
                        rMap.put(tObject.getLong(ITEM_ID), tObject.getString("Title"));
                    }
                }
                ++currentPage;
            }
            catch (WebbException ex) {
        		handleWebbException(ex);
        	}
        }

        return rMap;

    }
    
    /***
     * Retrieve all items from a title (i.e. a series)
     * 
     * This is a call to https://www.biodiversitylibrary.org/api3?op=GetTitleMetadata&id=<titleID>&items=t&apikey=<KEY>
     * @param titleID The title ID to search for
     * @return A list of items included in the given title. The list is empty is none were found.
     * @throws AuthenticationException
     * @throws ItemDoesNotExistException
     */
    public List<Long> getItemsFromTitle(long titleID) 
    		throws AuthenticationException, ItemDoesNotExistException {
    	Map<String, Object> params = new HashMap<>(0);
    	
    	logger.info("Resolving items of title ID {}", titleID);

        params.put(API_KEY, apiKey);
        params.put(FORMAT, JSON_FORMAT);
        params.put(OPERATION, GET_TITLE_METADATA);
        params.put(ID, titleID);
        params.put(ITEMS, true);
        
        List<Long> itemsOfTitleList = new ArrayList<>();
        
        try {
            JSONObject apiResponse = RESTUtils.getObjectFromRest(BHL_API_URL, GET, params);

            if (apiResponse.has(REQUEST_STATUS) && 
            		apiResponse.getString(REQUEST_STATUS).equalsIgnoreCase(REQUEST_OK)) {
            	
            	JSONObject titleJson = getApiResultObject(apiResponse);
            	JSONArray items = titleJson.getJSONArray(ITEMS);
            	for (Object item : items.toList()) {
            		@SuppressWarnings("unchecked")
					HashMap<String, Integer> itemMap = (HashMap<String, Integer>) item;
            		itemsOfTitleList.add(itemMap.get(ITEM_ID).longValue());
            	}
            }
        }
        catch (WebbException ex) {
    		handleWebbException(ex);
    	}
        
        logger.info("Found {} items for this title!", itemsOfTitleList.size());
        
        return itemsOfTitleList;

    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Object> getListOfItems() {
    	return new ArrayList(listOfItemsToDownload);
    }
    
    public void setBhlApiKey(String apiKey) {
    	this.apiKey = apiKey;
    }
    
    @Override
    protected String getFolderName() {
    	return BHL_STRING;
    }
    
    @Override
    protected boolean nextItem(Item item) {
    	if (itemListIterator == null) {
    		logger.info("Starting BHL download...");
    		itemListIterator = listOfItemsToDownload.iterator();
    	}
    	
    	while (itemListIterator.hasNext()) {
    		Object itemObj = itemListIterator.next();
    		long itemId = Long.parseLong(itemObj.toString());
    		
    		logger.info("Processing item ID {}", itemId);
    		
    		try {
				JSONObject itemMetadata = getItemMetadata(itemId);
				logger.debug("Received metadata!");
				logger.debug("Metadata Set: {}", itemMetadata.toString(2));
				
				if (isReferencingExternalResource(itemMetadata)) {
					String externalResourceNameString = getExternalResourceNameString(itemMetadata);
					logger.info("Is external resource from {} .", externalResourceNameString);
					
					item.digestItemData(processExternalResource(itemMetadata, externalResourceNameString));
					
					// Item ID has to be the BHL ID, not an external!
					item.setItemId(itemId);
				} else {
					addMetadataToItem(item, itemMetadata);
				}
				
				return true;
				
			} catch (ItemDoesNotExistException ex) {
				logger.error("The requested item (ID {}) does not exist!", itemId);
			} catch (AuthenticationException ex) {
				logger.fatal("The given API key is invalid!");
				return false;
			}
    	}
    	
    	logger.info("Processing items complete!");
		return false;
    }
    
    private void addMetadataToItem(Item item, JSONObject itemMetadata) {
    	long itemID = itemMetadata.getLong(ITEM_ID);
    	logger.debug("Processing Item ID {}", itemID);
		item.setDataSource(BHL_STRING);
		item.setItemId(itemID);
		item.addTextFileUrl(itemMetadata.getString(ITEM_PDF_URL), Item.FileType.PDF);
		item.addTextFileUrl(itemMetadata.getString(ITEM_TXT_URL), Item.FileType.TXT);
		item.addMetdata(ITEM_COMPLETE_METADATA, itemMetadata);
    }
    
    private JSONArray getApiResultArray(JSONObject apiResponse) 
    		throws AuthenticationException, ItemDoesNotExistException {
    	JSONArray jsonApiResultArray = apiResponse.getJSONArray("Result");
    	
    	if (apiResponse.getString(REQUEST_STATUS).equals(REQUEST_UNAUTHORIZED)) {
    		throw new AuthenticationException("The given API key is not valid! Key: " + apiKey);
    	}
    	
    	if (jsonApiResultArray.length() == 0) {
    		throw new ItemDoesNotExistException("The item could not be found!");
    	}
    	
    	return jsonApiResultArray;
    }
    
    private JSONObject getApiResultObject(JSONObject apiResponse) 
    		throws AuthenticationException, ItemDoesNotExistException {
    	JSONArray resultArray = getApiResultArray(apiResponse);
    	return (JSONObject) resultArray.get(0);
    }
    
    private Configuration getDefaultHarvesterConfiguration() {
    	return new Configuration("", "", new JSONObject());
    }
    
    private String getExternalResourceNameString(JSONObject itemMetadata) {
    	return itemMetadata.getString(SOURCE);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Object> getListFromJsonKey(String jsonKey, JSONObject jsonConfiguration) {
    	if (!jsonConfiguration.has(jsonKey)) {
    		return new ArrayList<>(0);
    	}
    	
    	JSONArray itemArray = jsonConfiguration.getJSONArray(jsonKey);
    	try {
    		String firstElement = itemArray.getString(0);
    		return (List) FileHandler.readListFromFile(firstElement);
    	} catch (JSONException ex) {
    		return itemArray.toList();
    	}
    }
    
    private void handleWebbException(WebbException ex) throws AuthenticationException {
    	if (ex.getMessage().contains("401 Unauthorized")) {
			throw new AuthenticationException("The given API key is not valid! Key: " + apiKey);
		} else {
			throw ex;
		}
    }
    
    private boolean isReferencingExternalResource(JSONObject itemMetadata) {
    	String itemSource = getExternalResourceNameString(itemMetadata);
    	if (itemSource.contains(BibDigitalHarvester.BIBDIGITAL_BOTANICAL_GARDEN_MADRID_STRING)) {
    		return true;
    	}
    	// add further harvesters here with else if
    	
    	return false;
    }
    
    private Item processExternalResource(JSONObject itemMetadata, String externalResourceName) {  
    	Configuration configuration = getDefaultHarvesterConfiguration();
    	try {
	    	if (externalResourceName.contains(BibDigitalHarvester.BIBDIGITAL_BOTANICAL_GARDEN_MADRID_STRING)) {
    			logger.debug("Creating harvester for BibDigital Madrid...");
				BibDigitalHarvester harvester = new BibDigitalHarvester(configuration);
				URL itemUrl = new URL(itemMetadata.getString(EXTERNAL_URL));
				
				logger.debug("Adding URL {} to harvest...", itemUrl);
				harvester.addItemToCollect(itemUrl);
				
				Item externalItem = new Item();
				if (harvester.nextItem(externalItem)) {
					logger.debug("Created valid item!");
					return externalItem;
				} else {
					logger.debug("Could not create valid item!");
					return null;
				}
	    	}
    	} catch (UnsetHarvesterBaseDirectoryException ex) {
			// does not happen
		} catch (MalformedURLException|JSONException ex) {
			logger.warn("Could not create URL for external resource!");
		}	
    	
    	return null;
    }
 
    class ItemDoesNotExistException extends IOException {
    	
		private static final long serialVersionUID = 5468828339593866141L;

		ItemDoesNotExistException(String s) {
    		super(s);
    	}
    }
}
