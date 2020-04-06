package de.biofid.services.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

/***
 * An item holds all metadata of a single book or monography.
 * This class is responsible for downloading and saving the item.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public class Item {
	
	public enum ItemType {
		ARTICLE, BOOK
	}
	
	public enum FileType {
		PDF, TXT, XML, JSON
	}
	
	public static String TEXT_OUTPUT_FOLDER_NAME = "text";
	public static String METADATA_OUTPUT_FOLDER_NAME = "metadata";
	
	public static String METADATA_ROOT_NAME = "Metadata";
	public static String METADATA_ITEM_ID_STRING = "ID";
	public static String METADATA_URL_STRING = "Url";
	public static String METADATA_ITEM_TEXT_URLS_PARENT_STRING = "TextUrls";
	public static String METADATA_ITEM_TEXT_URL_STRING = "TextUrl";
	public static String METADATA_ITEM_TEXT_FILE_TYPE_STRING = "Filetype";
	public static String METADATA_ITEM_SOURCE_STRING = "Source";
	
	public static final int JSON_N_SPACES_FOR_INDENTATION = 2;
	
	private static final Logger logger = Logger.getLogger(Item.class.getName());
	private static final Level LOGGER_LEVEL = Level.FINE;
		
	private long itemID = -1;
	private String dataSource = "";
	private URL itemUrl;
	private ArrayList<URL> textFileUrls = new ArrayList<>();
	private ArrayList<FileType> textFileTypes = new ArrayList<>();
	private JSONObject itemMetadata = new JSONObject();
	private HashSet<String> createdTextFiles = new HashSet<>();
	
	public Item () {
		setupLogger();
	}

	/***
	 * Add a new key with an object to the metadata.
	 */
	public void addMetdata(String key, Object value) {
		this.itemMetadata.put(key, value);
	}
	
	/***
	 * Append an Object to a list in the metadata.
	 * 
	 * If the list does not exist, it will be created.
	 * @param listName The name of the list to access.
	 * @param value The object to append to the list.
	 */
	public void addMetadataToArray(String listName, Object value) {
		JSONArray jsonArray;
		
		if (itemMetadata.has(listName)) {
			jsonArray = itemMetadata.getJSONArray(listName);
		} else {
			jsonArray = new JSONArray();
		}
		
		itemMetadata.put(listName, jsonArray.put(value));
	}
	
	/***
	 * Add a file on the list to download.
	 * @param sUrl The URL where the file can be downloaded.
	 * @param type The type of the file (e.g. PDF or TXT).
	 */
	public void addTextFileUrl(String sUrl, FileType type) {
		URL url;
		try {
			url = new URL(sUrl);
		} catch (MalformedURLException ex) {
			logger.warning(ex.getMessage());
			return;
		}
		
		if (textFileUrls.contains(url)) {
			return;
		}
	
		this.textFileUrls.add(url);
		this.textFileTypes.add(type);
	}
	
	public long getItemId() {
		return this.itemID;
	}
	
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}
	
	/***
	 * The item id HAS to be set! From the ID all file names are derived!
	 */
	public void setItemId(long id) {
		this.itemID = id;
	}
	
	public void setItemUrl(String sUrl) {
		URL url;
		try {
			url = new URL(sUrl);
		} catch (MalformedURLException ex) {
			logger.warning(ex.getMessage());
			return;
		}
		setItemUrl(url);
	}
	
	public void setItemUrl(URL url) {
		this.itemUrl = url;
	}
	
	public Path writeMetadataFile(String outputDirectory, FileType outputFormat) 
			throws UnsupportedOutputFormatException {
		addObjectVariableDataToMetadata();
		
		Path outputPath = Paths.get(outputDirectory, METADATA_OUTPUT_FOLDER_NAME);
		createDirectoryIfNotExisting(outputPath);
		Path filePath = getLocaleItemFilePath(outputPath, outputFormat);
		
		logger.info("Preparing to write metadata into " + filePath.toAbsolutePath().toString());
		
		String metadataOutputString = getMetadataStringForFileType(outputFormat);
		
		logger.info("Writing metadata");
		
		writeStringToFile(filePath, metadataOutputString);
		
		return filePath;
	}
	
	public List<Path> writeTextFiles(String outputDirectory, boolean overwriteExistingFiles) 
			throws DownloadFailedException {
		Path outputPath = Paths.get(outputDirectory, TEXT_OUTPUT_FOLDER_NAME);
		createDirectoryIfNotExisting(outputPath);

		ArrayList<Path> downloadedFiles = new ArrayList<>();
		for (int i = 0; i < textFileUrls.toArray().length; ++i) {
			URL fileUrl = textFileUrls.get(i);
			FileType fileType = textFileTypes.get(i);
			Path textFilePath = getLocaleItemFilePath(outputPath, fileType);
			
			if (textFilePath.toFile().exists() && !overwriteExistingFiles) {
				logger.info("File " + textFilePath.toString() + " exists already! Skipping!");
				continue;
			}
			
			if (downloadFile(fileUrl, textFilePath)) {
				downloadedFiles.add(textFilePath);
			}
		}
		
		return downloadedFiles;
	}
	
	private void addObjectVariableDataToMetadata() {
		itemMetadata.put(METADATA_ITEM_ID_STRING, itemID);
		itemMetadata.put(METADATA_URL_STRING, itemUrl);
		itemMetadata.put(METADATA_ITEM_SOURCE_STRING, dataSource);
		
		JSONArray textUrls = new JSONArray();
		for (int i = 0; i < this.textFileUrls.toArray().length; ++i) {
			JSONObject instanceJson = new JSONObject();
			instanceJson.put(METADATA_URL_STRING, this.textFileUrls.get(i));
			instanceJson.put(METADATA_ITEM_TEXT_FILE_TYPE_STRING, this.textFileTypes.get(i));
			textUrls.put(instanceJson);
		}
		JSONObject textUrlList = new JSONObject();
		textUrlList.put(METADATA_ITEM_TEXT_URL_STRING, textUrls);
		itemMetadata.put(METADATA_ITEM_TEXT_URLS_PARENT_STRING, textUrlList);
	}
	
	private boolean createDirectoryIfNotExisting(Path directoryPath) {
		if (!directoryPath.toFile().exists()) {
			return directoryPath.toFile().mkdirs();
		}
		return false;
	}
	
	private Path getLocaleItemFilePath(Path outputPath, FileType fileType) {
		boolean allowDuplication = false;
		String textFileName = getItemFileNameForFiletype(fileType, allowDuplication);
		createdTextFiles.add(textFileName);
		return outputPath.resolve(textFileName);
	}
	
	private String getItemFileNameForFiletype(FileType type, boolean allowDuplication) {
		String fileName = itemID + "." + type.toString().toLowerCase();
		
		if (!allowDuplication && checkFileNameForDuplication(fileName)) {
			fileName = deduplicateFileName(fileName);
		}

		return fileName;
	}
	
	private String getMetadataStringForFileType(FileType outputFormat) 
			throws UnsupportedOutputFormatException {
		switch (outputFormat) {
			case XML:
				return XML.toString(itemMetadata, METADATA_ROOT_NAME);
			case JSON: 
				return itemMetadata.toString(JSON_N_SPACES_FOR_INDENTATION);
			default: 
				throw new UnsupportedOutputFormatException("The given FileType '" + outputFormat.toString() +
						"' is not supported! Please choose: XML or JSON .");
			
		}
	}
	
	private boolean checkFileNameForDuplication(String fileName) {
		return createdTextFiles.contains(fileName);
	}
	
	private String deduplicateFileName(String fileName) {
		String fileNameSuffix = FilenameUtils.getExtension(fileName);
		String fileNameBase = FilenameUtils.getBaseName(fileName);
		
		int counter = 1;
		while (true) {
			fileName = fileNameBase + "-" + Integer.toString(counter) + "." + fileNameSuffix;
			
			if (checkFileNameForDuplication(fileName)) {
				++counter;
			} else {
				break;
			}
		}
		
		return fileName;
	}
	
	private boolean downloadFile(URL sourceUrl, Path sinkFilePath) throws DownloadFailedException {
		long nBytesTransfered = 0;
		
		ReadableByteChannel readableByteChannel = null;
		FileChannel fileChannel = null;
		try {
			readableByteChannel = Channels.newChannel(sourceUrl.openStream());
			fileChannel = FileChannel.open(sinkFilePath, StandardOpenOption.CREATE, 
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			logger.info("Downloading file '" + sinkFilePath.toString() + "' from URL: " + sourceUrl.toString());
			nBytesTransfered = fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			logger.info("Download done!");
		} catch (IOException ex) {
			throw new DownloadFailedException("An error happened while downloading from URL '" + sourceUrl + "'.\n" +
				"Error Message: " + ex.getMessage());
		} finally {
			try {
				if (readableByteChannel != null) {
					readableByteChannel.close();
				}
				
				if (fileChannel != null) {
					fileChannel.close();
				}
			} catch (IOException ex) {
				logger.warning(Arrays.toString(ex.getStackTrace()));
			}
		}
		
		return nBytesTransfered > 0;
	}
	
	private void setupLogger() {		
		logger.setLevel(LOGGER_LEVEL);
		
		Path loggerOutputDirectoryParentPath = Paths.get(LiteratureHarvester.LOGGER_OUTPUT_FILE).getParent();
		createDirectoryIfNotExisting(loggerOutputDirectoryParentPath);
		
        FileHandler fileTxt;
		try {
			fileTxt = new FileHandler(LiteratureHarvester.LOGGER_OUTPUT_FILE);
		} catch (SecurityException | IOException e) {
			logger.warning(Arrays.toString(e.getStackTrace()));
			return;
		}

        // create a TXT formatter
        SimpleFormatter formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);
	}
	
	private void writeStringToFile(Path filePath, String content) {
		try {
			Files.write(filePath, content.getBytes());
		} catch (IOException ex) {
			logger.warning(Arrays.toString(ex.getStackTrace()));
		}
	}
	
	class DownloadFailedException extends Exception {
		private static final long serialVersionUID = 558535317153362842L;

		DownloadFailedException(String s) {
			super(s);
		}
	}
	
	class UnsupportedOutputFormatException extends Exception {
		private static final long serialVersionUID = 7620152992815263665L;

		UnsupportedOutputFormatException(String s) {
			super(s);
		}
	}
}
