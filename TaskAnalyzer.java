/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

// Handles commands read from file
public class TaskAnalyzer {
	private FileSystem fileSystem;
	private boolean hasBeenInitialized = false;

	TaskAnalyzer() {

	}

	// Called through the "in" command & initializes new filesystem. You don't
	// want to do this in the task analyzer initialization because the user
	// needs to specify the in command in order to initialize the entire system.
	public String generateNewFileSystem() {
		this.fileSystem = new FileSystem();
		this.hasBeenInitialized = true;
		return Outputs.INITIALIZED;
	}

	// Disallows use of other commands if the system has not been initialized
	public boolean inCalled() {
		return hasBeenInitialized;
	}

	// Called through the "in <fileName>" command & initializes new filesystem
	// from file
	public String inCommand(String[] line) throws IOException {
		// If the length isn't two, that means that there wasn't a file
		// specified to load from
		if (line.length != 2)
			return Errors.ERROR;
		return fileSystem.init(line[1]);
	}

	// Called through the "cr <fileName>" command and creates a new file based
	// on the input
	public String crCommand(String[] line) {
		if (line.length != 2 || line[1].length() > 4)
			return Errors.ERROR;

		// Attempts to create file in open file table and retrieves response
		int response = fileSystem.create(line[1]);

		// Returns appropriate response
		return response == Errors.FAIL ? Errors.ERROR : line[1]
				+ Outputs.CREATED;

	}

	// Called through the "de <fileName>" and deletes file based on input
	public String deCommand(String[] line) {
		// If the length is not two then the file name was not provided
		if (line.length != 2)
			return Errors.ERROR;

		// Attempts to destroy a file and retrieves the response
		int response = fileSystem.destroy(line[1]);

		// Returns appropriate response
		return response == Errors.SUCCESS ? line[1] + Outputs.DESTROYED
				: Errors.ERROR;
	}

	// Called through the "op <fileName" command and opens file based on input
	public String opCommand(String[] line) {
		// If the length is not two then a filname was not provides
		if (line.length != 2)
			return Errors.ERROR;

		// Attempts to open the specified file and returns the response
		int response = fileSystem.open(line[1]);

		// Returns the appropriate response
		return response != Errors.FAIL ? line[1] + Outputs.OPENED
				+ Integer.toString(response) : Errors.ERROR;

	}

	// Called through the "cl <fileName>" command and closes a file based on
	// input
	public String clCommand(String[] line) {
		// If the length is not two then a filename was not provided
		if (line.length != 2)
			return Errors.ERROR;

		// Attempts to close a file and retrieves the response
		int response = fileSystem.close(Integer.parseInt(line[1]));

		// Returns the appropriate response
		return response == Errors.FAIL ? Errors.ERROR : line[1]
				+ Outputs.CLOSED;

	}

	// Called through the "rd <> <>" command and reads the associated file
	public String rdCommand(String[] line) {
		// If the length is not 3, then there was not enough information to read
		// the file
		if (line.length != 3)
			return Errors.ERROR;

		// Make sure the open file exists
		int size = Integer.parseInt(line[2]);
		int index = Integer.parseInt(line[1]);
		if (index > fileSystem.numOpenFiles())
			return Errors.ERROR;

		// Retrieves the output from reading a specified portion of the file
		String str = new String(fileSystem.read(index,
				fileSystem.initializeEmpty(size), size));

		// Returns a new line if there is nothing to read else the str itself
		return str.length() == 0 ? "\r\n" : str;

	}

	// Called through the "wr" command and writes the associated file
	public String wrCommand(String[] line) {
		// If the length of the line is not 4, then not enough info is given to
		// write to the file
		if (line.length != 4)
			return Errors.ERROR;

		// Make sure the open file exists
		int index = Integer.parseInt(line[1]);
		if (index > fileSystem.numOpenFiles())
			return Errors.ERROR;

		// Generates helper array which sets up the aray to be written to the
		// file system
		// length is at position 3, and line2 get bytes is the byte to write
		byte[] helperArray = wrHelper(Integer.parseInt(line[3]),
				line[2].getBytes()[0]);

		// Attempts to write to the file and retrieves the response
		int response = fileSystem.write(index, helperArray, 0);

		// Returns the appropriate response
		return response != Errors.FAIL ? Integer.toString(response)
				+ Outputs.BYTES : Errors.ERROR;

	}

	// Called through the "sk" command and seeks according to input
	public String skCommand(String[] line) {
		// If the line length is not three then not enough info was given to
		// seek
		if (line.length != 3)
			return Errors.ERROR;

		// Make sure the open file exists
		int index = Integer.parseInt(line[1]);
		if (index > fileSystem.numOpenFiles())
			return Errors.ERROR;

		// Attempts to seek through the file and returns the appropriate
		// response
		int response = fileSystem.lseek(index, Integer.parseInt(line[2]));

		// Returns the appropriate response
		return response == Errors.SUCCESS ? Outputs.POSITION + line[2]
				: Errors.ERROR;

	}

	// Called through the "dr" command
	public String drCommand(String[] line) {
		// If there was too much info given then there was an error. Otherwise
		// it returns the names of the files in the open file table
		if (line.length != 1)
			return Errors.ERROR;

		ArrayList<String> fileNames = fileSystem.directory();
		String fileString = "";

		for (int i = 0; i < fileNames.size(); i++)
			fileString += fileNames.get(i);

		return fileString;
	}

	// Called through the "sv" command and saves the corresponding file
	public String svCommand(String[] line) throws FileNotFoundException {
		// If there is not a length of two then the correct information was not
		// provided to save the ldisk
		if (line.length != 2)
			return Errors.ERROR;

		// Saves the file system to the corresponding file
		fileSystem.save(line[1]);

		// Returns success
		return Outputs.SAVED;

	}

	// The write function in the file system requires a byte array in order to
	// write to the system. This function appears out of place, however it is
	// import in making sure the data is properly written to the system
	private byte[] wrHelper(int length, byte toWrite) {
		// Length is how many times the integer should be added to the file
		byte[] toAddToFile = new byte[length];

		// Add the bytes until we hit the length
		for (int i = 0; i < length; i++)
			toAddToFile[i] = toWrite;

		return toAddToFile;

	}

}
