package de.biofid.services.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileHandler {
	
	private FileHandler() {}
	
	/***
	 * Returns the content of a given file.
	 * @param filePath The String of the path to a file.
	 * @return A String representation of the file content.
	 */
	public static String getFileContent(String filePath) {
		
		String content = "";
		Scanner scanner = null;
		try {
			File file = new File(filePath);
			scanner = new Scanner(file);
			
			StringBuilder stringBuilder = new StringBuilder();
			while (scanner.hasNext()) {
				stringBuilder.append(scanner.nextLine() + "\n");
			}
		
			content = stringBuilder.toString();
			
		} catch (FileNotFoundException e) {
			System.out.println("Could not find file " + filePath);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
		return content;
	}
	
	/***
	 * Evaluates if a given String resolves to a file or a path.
	 * This method does NOT check, if the given file or path exists!
	 * @param pathOrFile The String to evaluate.
	 * @return True, if the given string is a path or a file. False, otherwise.
	 */
	public static boolean isStringPathOrFile(String pathOrFile) {
        try {
            Paths.get(pathOrFile);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }
	
	/***
	 * Reads a list from the given file.
	 * The given file is read and its content is converted to a list. This method assumes
	 * one list element per line.
	 * @param filePath A file to read the list from.
	 * @return A list of String elements.
	 */
	public static List<String> readListFromFile(String filePath) {
		String defaultSeperator = "\\n";
		return readListFromFile(filePath, defaultSeperator);
	}
	
	/***
	 * Reads a list from the given file.
	 * The given file is read and its content is converted to a list. This method takes a 
	 * separator as second argument. Please note, that you have to escape backslashes. Hence,
	 * it is "\\n" for "\n" (newline) or "\\t" for "\t" (tab).
	 * @param filePath A file to read the list from.
	 * @param separator A String that separates two elements.
	 * @return A list of String elements.
	 */
	public static List<String> readListFromFile(String filePath, String separator) {
		String fileContent = getFileContent(filePath);
		return new ArrayList<>(Arrays.asList(fileContent.split("\\s*" + separator + "\\s*"))); 
	}
}
