package de.biofid.services.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.biofid.services.crawler.Item.DownloadFailedException;
import de.biofid.services.crawler.Item.UnsupportedOutputFormatException;

/***
 * A Harvester crawls the data of some website and draws the literature items from it.
 * 
 * This is an abstract class; subclasses must override {@link #nextItem(Item item)} and {@link #getFolderName()}.
 * 
 * @author Adrian Pachzelt (University Library Johann Christian Senckenberg, Frankfurt)
 * @author https://www.biofid.de
 * @version 1.0
 */
public abstract class Harvester {

	private static String baseOutputDirectory = null;
	
	protected Configuration configuration;
	protected long millisecondsDelayBetweenRequests = 0;

	// Logging for all sub-classes
	protected Logger logger = LogManager.getLogger(LiteratureHarvester.LOGGER_NAME);
		
	/***
	 * Subclasses HAVE TO have a constructor that takes a Configuration object as the only parameter!
	 * @param configuration The configuration of this harvester.
	 * @throws UnsetHarvesterBaseDirectoryException
	 */
	protected Harvester (Configuration configuration) throws UnsetHarvesterBaseDirectoryException {
		if (baseOutputDirectory == null) {
			throw new UnsetHarvesterBaseDirectoryException("The base output directory has to be set!");
		}
		
		this.configuration = configuration;
	}

	/***
	 * Set the directory where to write all downloaded data.
	 */
	public static void setOutputDirectory(String outputDirectory) {
		Harvester.baseOutputDirectory = outputDirectory;
	}
	
	public final Path getWorkingDirectory() {
		return Paths.get(baseOutputDirectory, getFolderName().toLowerCase());
	}

	/***
	 * This function can be called to start the harvesting of a specific internet source.
	 * @throws CouldNotCreateDirectoryException
	 */
	public final void run() {
		try {
			createWorkingDirectory();
			createOutputDirectory();
		} catch (IOException e) {
			logger.fatal("Could not create working directories for '" + this.getClass().getName() + "'!");
			return;
		}
		
		while (true) {
			Item item = new Item();
			
			pause();
			boolean next = nextItem(item);
			if (next) {
				processItem(item);
			} else {
				break;
			}
		}
	}
	
	public void setRequestDelayInMilliseconds(long millisecondsDelay) {
		millisecondsDelayBetweenRequests = millisecondsDelay;
	}
	
	/***
	 * A function that simply returns the name of the folder where the Items should be stored.
	 * @return A folder name
	 */
	protected abstract String getFolderName();
	
	/***
	 * This function produces a single item per call.
	 * 
	 * Child classes should provide the necessary metadata to fill the given, empty Item object. However,
	 * the given Item object is not returned! Instead is returned a boolean that indicates whether or not
	 * the item was filled. If the returned boolean is false, the stream stops and no further items are requested.
	 * This also means that even if the given item object was filled and the function returns false, this item
	 * object is NOT processed!
	 * 
	 * I decided for this approach, because it gives a better granularity and higher flexibility in the data.
	 * If you would return an item, the harvester might not be able to tell if this item was filled with all
	 * necessary data and when to stop. If you would set some variable within the Item object, guess what, you
	 * also can return a boolean from the function and it is more explicit.
	 * 
	 * @param item An Item object to be filled.
	 * @return A boolean to indicate if there are more items to come. If false, the stream stops and the function
	 * is not called anymore.
	 * @throws Exception 
	 */
	protected abstract boolean nextItem(Item item);
	
	protected void pause() {
		try {
			Thread.sleep(millisecondsDelayBetweenRequests);
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage());
		}
	}
	
	private boolean createDirectoryIfNotExisting(Path pathToCreate) {
		File pathFile = pathToCreate.toFile();
		if (!pathFile.exists()) {
			return pathFile.mkdirs();
		} else {
			return true;
		}
	}
	
	private boolean createOutputDirectory() throws IOException {
		Path baseDirectory = getWorkingDirectory();
		
		if (!baseDirectory.toFile().exists()) {
			throw new CouldNotCreateDirectoryException("The base directory " + baseDirectory.toString() + 
					" does not exists and so the working folders could not be created!");
		}
	
		return true;
	}
	
	private boolean createWorkingDirectory() throws IOException {
		boolean doesBaseFolderExist = createDirectoryIfNotExisting(getWorkingDirectory());
		
		if (!doesBaseFolderExist) {
			throw new CouldNotCreateDirectoryException("The directory " + getWorkingDirectory().toString() + 
					" could not be created");
		}
		
		return true;
	}
	
	private boolean processItem(Item item) {
		Path outputPath = getWorkingDirectory();
		boolean overwriteExistingFiles = configuration.isOverwrittingEnabled();
		
		String outputPathString = outputPath.toString();
		try {
			item.writeTextFiles(outputPathString, overwriteExistingFiles);
		} catch (DownloadFailedException ex) {
			logger.error("The download of a text file from item ID " + item.getItemId() + " failed!");
			logger.error(Arrays.toString(ex.getStackTrace()));
		}
		
		try {
			item.writeMetadataFile(outputPathString, Item.FileType.XML);
		} catch (UnsupportedOutputFormatException ex) {
			logger.error("Writing of the metadata of item ID " + item.getItemId() + " failed!");
			logger.error(Arrays.toString(ex.getStackTrace()));
		}
		
		return true;
	}
	
	class CouldNotCreateDirectoryException extends IOException {
		private static final long serialVersionUID = -8144628595804556669L;

		CouldNotCreateDirectoryException(String s) {
			super(s);
		}
	}
	
	class UnsetHarvesterBaseDirectoryException extends IOException {
		private static final long serialVersionUID = -235562017414278915L;

		UnsetHarvesterBaseDirectoryException(String s) {
			super(s);
		}
	}
}

enum Language {
	ENGLISH, FRENCH, GERMAN, LATIN;
}
