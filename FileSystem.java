/**
 * @author Devin Held ID: 26883102
 *
 */

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

//Invalid operations: (have to report an error)
//- create existing file names x
//- create a file name exceeding the maximum file name length x
//- delete non-existing file names x 
//- open non-existing file names x 
//- read/write/close non-existing fds x 
//- delete an opened file x
//- lseek a position exceeding the maximum file length x
//
//Valid operations:
//- read/write a file exceeding the maximum file length: read and write as many as possible, discard the rest and report how many bytes are done
//- lseek a file position exceeding the file length but within the maximum file length: 
//-- later, if something has been written to the file, then fill in 0s in the gap between new file position and previous end-of-file position.

// Each descriptor: data (64) - position (4) - descriptor index (4) - descriptor length (4)
// 6 files allowed in io
// 3 open files

// Core of the project. Handles interaction with the io system, as well as all file interactions
public class FileSystem {
	private IOSystem ioSystem;
	private BitMap bitmap;
	private ArrayList<OpenFile> openFiles;

	// Works as a buffer for ease of copying between io and filesystem
	// interfaces
	private byte[][] buffer;

	// Initializer called during FIRST init. Otherwise, it is populated through
	// the postInitialize function
	public FileSystem() {
		this.ioSystem = new IOSystem();
		this.bitmap = new BitMap();
		this.buffer = new byte[Info.BUFFER_SIZE][Info.OPEN_FILE_TABLE_SPACE_SIZE];
		this.openFiles = new ArrayList<OpenFile>();

		// At the beginning, we need to set the bitmaps position in the bit map
		// to occupies as well as null all bytes in the open file table for
		// future comparison
		setOpeningValues();
	}

	// Handles the creation of a new file, if it doesnt exist and if there is a
	// place to put it
	public int create(String file) {
		// Searching to see if the file is already in the system
		file = file.trim();
		int pos = fileSearch(file.getBytes());

		// Searching to see if there is an empty location in the system
		int indexInIOSystem = findEmptyPlaceForNewDescriptor();

		// If the file is present, or there isnt an open position
		// then return an error!!!!!!
		if ((pos != Errors.FAIL) || indexInIOSystem == Errors.FAIL
				|| file.length() > 4)
			return Errors.FAIL;

		// Finds the position where the filename will reside
		int position = fileSearch(initializeEmpty(Info.SINGLE_ENTRY_SIZE));

		// Writes the new file descriptor and corresponding data into file
		// system at this point we know there is a space and the file is not
		// already in there so the function ends in success.
		return writeDescriptorToIOSystem(indexInIOSystem, file, position);
	}

	// Handles the destroying of files in the system
	public int destroy(String file) {
		// Finds the position of the specified file in the directory
		int position = fileSearch(file.getBytes()), open = checkIfAlreadyOpen(file);

		// Handles the case where the file doesn't exist
		if (position == Errors.FAIL || open == Errors.SUCCESS)
			return Errors.FAIL;

		// Seek until where the file begins and read the file name and index
		lseek(0, position * Info.WIDTH_OF_FILE_NAME_AND_INDEX);

		// Closes the specified file
		closeDestroyedFile(file);

		// resets the entry in the position of the file that is destroyed
		lseek(0, position * Info.WIDTH_OF_FILE_NAME_AND_INDEX);
		byte[] empty = initializeEmpty(Info.WIDTH_OF_FILE_NAME_AND_INDEX);
		write(0, empty, 0);

		// If it gets here then the file was destroyed. The only way the file
		// would not have been destroyed was if the file specified was not
		// found, which is handled above.
		return Errors.SUCCESS;

	}

	// Tries to open the specified file, returns the number of the file in the
	// open file table if successful. Index is later used for closing the file.
	public int open(String file) {
		// Finds the position of the file in the directory, whether the file is
		// already open and a free space in the open file table
		int position = fileSearch(file.getBytes()), open = checkIfAlreadyOpen(file);

		// The next open file table index is located at the next null byte
		int bufferIndex = bitmap.findEmptyLocation();

		// This method fails whenever the file is not present in the system or
		// there isn't an open spot in the open file table to store the file and
		// also if the file is already open. This will also fail if there are
		// already 3 open files
		if (openFiles.size() == Info.MAX_OPEN_FILES || open == Errors.SUCCESS
				|| position == Errors.FAIL || bufferIndex == Errors.FAIL)
			return Errors.FAIL;

		// Occupies the position in the open file table
		bitmap.fillPosition(bufferIndex);

		// Seek to the beginning of the current file
		lseek(0, position * Info.WIDTH_OF_FILE_NAME_AND_INDEX);

		OpenFile openFile = new OpenFile();
		openFile.fileName = file;
		openFile.indexInBuffer = bufferIndex;

		// Find the index in io system and other variables for open file
		openHelper(openFile);

		return bufferIndex;

	}

	// Closes the index of the file specified by the user input
	public int close(int indexInBuffer) {
		// Find open file and close it
		for (int i = 0; i < openFiles.size(); i++) {
			OpenFile openFile = openFiles.get(i);

			if (openFile.indexInBuffer == indexInBuffer) {
				// Removes the file at the indexes from the open index storage
				// array and writes the updated information to the system
				handleClosing(openFile.indexInIOSystem, indexInBuffer);

				// Updates length of descriptor if modified, adds it back to the
				// iosystem, and clears its spot in the open file table
				writeToIOAndDeleteFromOpenFileTable(openFile.descIndex,
						openFile.indexInIOSystem, indexInBuffer);

				openFiles.remove(i);
				return Errors.SUCCESS;
			}
		}

		// If we make it here, file was not found
		return Errors.FAIL;
	}

	// Handles reading entries in the file system
	public byte[] read(int location, byte[] bytesRead, int count) {

		// Position of file in system
		int position = readDescriptorData(location, Info.POS);

		// Read all of the bytes available up until the cap
		for (int i = 0; i <= count; i++) {

			// Finds the new block from the position and the jump
			int nextBlock = (position + i) / Info.LOGICAL_BLOCKS;

			// Allows us to see if position and the jump move on to next block
			int block = i > 0 ? (i - 1 + position) / Info.LOGICAL_BLOCKS
					: position / Info.LOGICAL_BLOCKS;

			// If this condition holds, the position and the jump indicate we
			// are in the next block. It also makes sure that we don't go out of
			// bound ... that the new index isnt greater than the number of
			// blocks we have
			if ((nextBlock != block) && nextBlock < Info.FILE_BLOCKS)
				// Writes current block to the temp buffer and reads next
				refreshTempBuffer(location, block + 1, nextBlock + 1);

			// If we have reached the end, then set the new position into the
			// system and return the read array
			if (i == count) {
				setDescriptorData(location, position + i, Info.POS);
				return bytesRead;
			}

			// The byte is ready to be read into the array to return
			bytesRead[i] = addToReadByteArray(location, position, i);
		}

		// If it gets here, it never went through the for loop and therefore the
		// case where read(0,0) etc was called
		return bytesRead;
	}

	// handles writing information to descriptors
	public int write(int location, byte[] toWrite, int count) {
		// Position of file in system
		int position = readDescriptorData(location, Info.POS);

		// DESC index of the descriptor
		int num = readDescriptorData(location, Info.DESC_INDEX);

		if (num == Errors.FAIL)
			createEmptyDescriptorBlock(location);

		// Write until we hit the specified length
		for (; count <= toWrite.length; count++) {

			// Finds the new block from the position and the jump
			int nextBlock = (count + position) / Info.LOGICAL_BLOCKS;

			// Allows us to see if position and the jump move on to next block
			int block = count > 0 ? (count - 1 + position)
					/ Info.LOGICAL_BLOCKS : position / Info.LOGICAL_BLOCKS;

			// The block we were on previously does not equal the one now
			if (nextBlock != block) {

				// Creates new block if the next one doesnt exist
				refreshCurrentBlock(nextBlock, num);

				// writes old temp buffer and reads new one
				refreshTempBuffer(location, block + 1, nextBlock + 1);
			}

			// Return if either reach the end of blocks or end of write length
			if (((nextBlock + 1) > Info.FILE_BLOCKS) || count >= toWrite.length)
				return updateDescInfo(location, position, count);

			// Add the current written index to the open file table
			addToBuffer(location, position + count, toWrite[count]);

		}
		return Errors.FAIL;
	}

	// seeks to the corresponding location in the file
	public int lseek(int indexInBuffer, int goTo) {
		// Get files current position
		int location = readDescriptorData(indexInBuffer, Info.POS);

		// If trying to seek to a spot beyond the max size then return error
		if (goTo > Info.FILE_SIZE)
			return Errors.FAIL;

		// Update open file table with where the current file position is
		updateOpenFileTableEntry(indexInBuffer, (goTo / Info.POS) + 1,
				(location / Info.POS) + 1);

		// Set new position in the open file table
		for (int i = 0; i < openFiles.size(); i++) {
			OpenFile openFile = openFiles.get(i);

			if (openFile.indexInBuffer == indexInBuffer)
				openFiles.get(i).position = goTo;
		}

		// Add new position into the location in the open file table
		setDescriptorData(indexInBuffer, goTo, Info.POS);

		return Errors.SUCCESS;
	}

	// Adds all the file names to a nice printable string
	public ArrayList<String> directory() {
		ArrayList<String> files = new ArrayList<String>();
		lseek(0, 0);

		// Loop through each of the file names currently stored
		for (int i = 0; i < Info.TOTAL_DESC_SPACE; i++)
			files.add(generateFileNameIfNotNull());

		// errors occur if not set back at end
		lseek(0, 0);
		return files;
	}

	// Initializes new file system by
	// populating from disk as specified by command
	public String init(String file) {

		// otherwise, there was a file specified and we load data from the file
		byte[][] updatedLDisk = ioSystem.fillldiskFromFile(file);

		// Resetting the open file table from the new io
		resetOpenFileTable();

		// Resets the disk, writing the storage entries to the io
		// and updates the bitmap
		resetDisk(updatedLDisk);

		return Outputs.RESTORED;
	}

	// Saves current filesystem, writing the ldisk to file
	public void save(String file) throws FileNotFoundException {
		for (int i = 0; i < Info.OPEN_FILE_TABLE_SIZE; i++) {

			// To get the normalized block index, we must read the position of
			// data i, divide by the position of the 'location' in the file and
			// add one because it wouldn't work otherwise, unfortunately -_-
			int blockLocation = (readDescriptorData(i, Info.POS) / Info.POS) + 1;
			writeToDirectory(i, blockLocation);
			close(i);
		}

		openFiles = new ArrayList<OpenFile>();
		ioSystem.saveLDisk(file);
	}

	/**
	 * 
	 * ALL HELPER FUNCTIONS FALL BELOW THIS LINE THEY ARE ALL CRUCIAL TO THE
	 * MAIN CLASS METHODS ABOVE THEIR PURPOSE IS TO SIMPLIFY THE READABILITY OF
	 * THE ABOVE METHODS AS WELL AS INCREASE THE EFFECTIVENESS OF DEBUGGING
	 * 
	 * NOTE: MANY OF THE BELOW FUNCTIONS COULD EASILY BE WRITTEN INSIDE THE MAIN
	 * FUNCTIONS, BUT I LIKE BREAKING THEM UP; IT'S MORE VISUALLY PLEASING
	 * 
	 */

	// Retrieves indexes to write the descriptor index to the open file table
	private void openHelper(OpenFile openFile) {
		// Pull the index number from the name - file combination that is read
		byte[] empty = initializeEmpty(Info.WIDTH_OF_FILE_NAME_AND_INDEX);
		int indexInIOSystem = PackableMemory.modifiedUnpack(
				read(0, empty, Info.WIDTH_OF_FILE_NAME_AND_INDEX),
				Info.LOCATION_OF_INDEX);

		byte[] descriptor = ioSystem.read_block(indexInIOSystem);

		// add the file index into the table
		setDescriptorData(openFile.indexInBuffer, indexInIOSystem,
				Info.DESC_INDEX);

		// This is the index of the file in the system
		int indexLoc = PackableMemory.modifiedUnpack(descriptor,
				Info.LOCATION_OF_INDEX);

		openFile.descIndex = 0;
		openFile.indexInIOSystem = indexInIOSystem;
		openFiles.add(openFile);

		// gets the bytes from the file being opened, adds it to open file table
		writeDataToBuffer(indexLoc, openFile.indexInBuffer);

	}

	// Converts the specified number to the correpsonding index in the io system
	private int getIndexFromNum(int indexInIOSystem, int num) {

		// Read the block from the io system
		byte[] descriptor = ioSystem.read_block(indexInIOSystem);

		// Unpack the information at the corresponding area. blockNum * location
		// gets the beginning of the block we wont. the desc block pos brings us
		// to the correct index we want
		return PackableMemory.modifiedUnpack(descriptor,
				(num * Info.LOCATION_OF_INDEX));
	}

	// Reads data from the io system and adds it to the open file table
	private void writeDataToBuffer(int indexLoc, int bufferIndex) {
		// Read the block from the system
		byte[] blockData = ioSystem.read_block(indexLoc);

		// Write data from the system into the open file table
		for (int b = 0; b < blockData.length; b++)
			buffer[bufferIndex][b % Info.LOGICAL_BLOCKS] = blockData[b];

	}

	// Creates an empty block to populate with data
	private int createEmptyDescriptorBlock(int indexInIOSystem) {
		int index = findEmptyBlock(indexInIOSystem), emptyBlock = bitmap
				.findEmptyBlock();

		if (emptyBlock == Errors.FAIL || index == Errors.FAIL)
			return Errors.FAIL;

		// populate bitmap
		bitmap.fillPosition(emptyBlock);

		// put index into descriptor read from the io system
		byte[] descriptor = ioSystem.read_block(indexInIOSystem);
		PackableMemory.modifiedPack(descriptor, emptyBlock, index
				* Info.LOCATION_OF_INDEX);

		// rewrite the block back to the io system
		ioSystem.write_block(indexInIOSystem, descriptor);

		return emptyBlock;
	}

	// Turns the bytes in the file array to a string
	private String replaceArray(byte[] files, int begin, int end) {
		String finalStr = "";
		finalStr += new String(Arrays.copyOfRange(files, 0,
				Info.LOCATION_OF_INDEX));
		return finalStr.trim();
	}

	// Generates one file name nicely
	private String generateFileNameIfNotNull() {
		String fileNames = "";
		byte[] empty = initializeEmpty(Info.WIDTH_OF_FILE_NAME_AND_INDEX);
		byte[] files = read(0, empty, Info.WIDTH_OF_FILE_NAME_AND_INDEX);

		// Name of file is four bytes long, need to make a nice string
		if (files[0] != Info.NULL)
			fileNames += replaceArray(files, 0, Info.LOCATION_OF_INDEX) + " ";
		return fileNames;
	}

	// writes information to io system
	private void writeToDirectory(int index, int locationOfBlock) {
		// if the open block is greater than the number of file blocks
		// then we dont want to create a new one!!!!
		if (locationOfBlock > Info.FILE_BLOCKS)
			return;

		// Create a temporary buffer to help write to the io system
		byte[] temp = populateTempByteArray(index, 0, Info.POS);

		// Find the correct index
		int blockEntryNum = 0;
		for (int i = 0; i < openFiles.size(); i++) {
			if (index == openFiles.get(i).indexInIOSystem)
				blockEntryNum = openFiles.get(i).indexInBuffer;
		}

		// we need the descriptor location, a temp ldisk index to verify
		// and a verified ldisk index in order to populate the open file table
		int location = getLocation(blockEntryNum, locationOfBlock);

		// And finally, lets write the data into the io system!!!!
		ioSystem.write_block(location, temp);
	}

	// Helper function to find the smaller of the byte arrays. Sometimes the
	// file names are different lengths and therefore will run into an error
	// when comparing them byte for byte.
	private int getSmallestSize(byte[] file, byte[] fileSearch) {
		int shortest = 0;
		byte temp = 0;
		try {
			for (int i = 0; i < Info.LOCATION_OF_INDEX; i++) {
				// Silly way to do so, but tries to index the file up until the
				// max file name length (Info.LOCATION_OF_INDEX)
				temp = file[i];
				temp = fileSearch[i];
				shortest++;
			}

			// this exception is caught when the index is out of bounds
		} catch (Exception e) {
			return shortest;
		}
		return shortest;
	}

	// writes to the io system and reads into the open file table
	private void updateOpenFileTableEntry(int indexInBuffer, int blockLocation,
			int blockIndex) {
		// write the block index data to the index in the open file table
		writeToDirectory(indexInBuffer, blockIndex);

		// read the block index data to the index in the open file table
		readToDirectory(indexInBuffer, blockLocation);
	}

	// Sets all information in the specified location in the open file table to
	// null. Used to delete files
	private void nullOpenFileTable(int indexInBuffer) {
		for (int i = 0; i < Info.OPEN_FILE_TABLE_SPACE_SIZE; i++)
			buffer[indexInBuffer][i] = Info.NULL;
	}

	// Generate a temp buffer to help write to io
	private byte[] populateTempByteArray(int index, int begin, int end) {
		byte[] temp = new byte[Info.POS];
		for (int i = begin; i < end; i++)
			temp[i] = buffer[index][i];
		return temp;
	}

	// Reads information from io system to open file table
	private void readToDirectory(int index, int locationOfBlock) {
		// we need the descriptor location, a temp ldisk index to verify
		// and a verified ldisk index in order to populate the open file table
		int location = getLocation(index, locationOfBlock);

		// Reads from the ldisk and puts the data into our open file table
		byte[] newTableEntry = ioSystem.read_block(location);

		// Put the io system entry into the buffer
		for (int i = 0; i < Info.BLOCK_LENGTH; i++)
			buffer[index][i] = newTableEntry[i];
	}

	// Compares file names returns true if equal false otherwise
	boolean compareFileNames(byte[] file, byte[] fileSearch) {
		boolean same = true;
		int smallestLen = getSmallestSize(file, fileSearch);

		// compares the names bit by bit
		for (int j = 0; j < smallestLen; j++) {
			if (file[j] != fileSearch[j])
				same = false;
		}
		return same;
	}

	// Resets the table and restores from the refreshed directory
	private void resetOpenFileTable() {
		// Creates a new dir, new io, new openfiletable
		byte[] dir = refreshDir(0, 0, Info.POS);
		ioSystem = new IOSystem();
		buffer = new byte[Info.OPEN_FILE_TABLE_SIZE + 1][Info.OPEN_FILE_TABLE_SPACE_SIZE];
		openFiles = new ArrayList<OpenFile>();
		// Sets the open file table bytes to what is in the dir
		resetTableBytes(dir);

		// updates the prev save data
		resetDataFromPrevSave();
	}

	// puts the information from the new dir into
	// the io system and resets bitmap
	private void resetDisk(byte[][] dir) {
		// Write everything in the temp dir to the io
		for (int i = 0; i < dir.length; i++)
			ioSystem.write_block(i, dir[i]);

		// delete other files currently in bitmap
		for (int i = 1; i <= 3; i++)
			bitmap.deletePosition(i);
	}

	// Scans the io system until there is an empty space available
	private int findEmptyBlock(int indexInIOSystem) {
		for (int index = 1; index < Info.OPEN_FILE_TABLE_SIZE; index++) {
			byte[] descriptor = ioSystem.read_block(indexInIOSystem);

			// If the space doesn't exist yet then we want to create it
			// index * Info.LOCATION_OF_INDEX gets to the index we want
			// jumping over bytes we don't care about .. did have descBlockPos +
			if (descriptor[index * Info.LOCATION_OF_INDEX] == Info.NULL)
				return index;
		}

		// hit the end and there's nothing left to store
		return Errors.FAIL;
	}

	// Uses the int unpack to retrieve necessary data
	// depending on position and what is needed
	private int readDescriptorData(int indexInBuffer, int goTo) {
		return PackableMemory.modifiedUnpack(buffer[indexInBuffer], goTo);
	}

	// Uses the int pack to pack necessary data into the buffer array
	private void setDescriptorData(int indexInBuffer, int begin, int goTo) {
		PackableMemory.modifiedPack(buffer[indexInBuffer], begin, goTo);
	}

	// Saves the open file table for later refreshing.
	private byte[] refreshDir(int loc, int begin, int end) {
		byte[] dir = new byte[Info.BLOCK_LENGTH];
		for (int i = begin; i < end; i++)
			dir[i] = buffer[loc][i];

		return dir;
	}

	// Initializes the open file table to the bytes in dir
	private void resetTableBytes(byte[] dir) {
		for (int i = 0; i < dir.length; i++)
			buffer[0][i] = dir[i];
	}

	// Resets data in the open file table.
	// reads from the saved table; first reads data from the descriptor
	// then places the appropriate information back into the descriptor
	private void resetDataFromPrevSave() {
		// file position
		setDescriptorData(0, readDescriptorData(0, Info.POS), Info.POS);

		// descriptor index
		setDescriptorData(0, readDescriptorData(0, Info.DESC_INDEX),
				Info.DESC_INDEX);

		// descriptor length
		setDescriptorData(0, readDescriptorData(0, Info.DESC_INDEX),
				Info.DESC_LENGTH);
	}

	// Initializes a byte array containing only the NULL bytes
	public byte[] initializeEmpty(int size) {
		byte[] empty = new byte[size];
		for (int i = 0; i < size; i++) {
			empty[i] = Info.NULL;
		}
		return empty;
	}

	// Checks to see if the file is already in open file table
	private int checkIfAlreadyOpen(String file) {
		for (int i = 0; i < openFiles.size(); i++) {
			if (file.equals(openFiles.get(i).fileName))
				return Errors.SUCCESS;
		}
		return Errors.FAIL;
	}

	// Tries to match file param to a file already in the system
	private int fileSearch(byte[] fileSearch) {
		lseek(0, 0);

		// Search through names and indexes until an equivalent name is reached
		for (int i = 0; i < 6; i++) {
			byte[] file = read(0,
					initializeEmpty(Info.WIDTH_OF_FILE_NAME_AND_INDEX),
					Info.WIDTH_OF_FILE_NAME_AND_INDEX);

			byte[] name = getFileName(file);

			// Get each string version of the file names
			String f1 = new String(name);
			String f2 = new String(fileSearch);

			f1 = f1.trim();
			f2 = f2.trim();

			// If they are equal, then return the index they were found at
			if (f1.equals(f2))
				return i;
		}

		// Otherwise, fail!!!!!
		return Errors.FAIL;
	}

	// Beginning initialization of open file table.
	private void initializeOpenFileTableOnce() {
		// Must set all bytes in the open file table to null
		// or else later comparison with the null byte will fail
		for (int i = 0; i < buffer.length; i++) {
			for (int j = 0; j < Info.BLOCK_LENGTH; j++)
				buffer[i][j] = Info.NULL;
		}
	}

	// Called in the original initialization to set the system up for comparison
	// and adding files
	private void setOpeningValues() {
		// Position 0 holds the bitmap!
		bitmap.fillPosition(0);

		// Must set all bytes in the open file table to null
		// or else later comparison with the null byte will fail
		initializeOpenFileTableOnce();
	}

	// Returns the index of the first place available to place a new file
	private int findEmptyPlaceForNewDescriptor() {
		for (int index = 1; index < Info.TOTAL_DESC_SPACE; index++) {
			// reads the file at the current index
			byte[] possiblePlace = ioSystem.read_block(index);

			// If the first byte is null, then theres no file name and the block
			// is therefore empty
			if (possiblePlace[0] == Info.NULL) {
				// Creates the 1-6 index from the 1-24 number.
				return ((index - 1) / Info.OPEN_FILE_TABLE_SIZE) + 1; // 1-6
			}
		}
		return Errors.FAIL;
	}

	// Handles copying the name of the file and index into a new byte array,
	// making it simple to add into the IOSystem
	private byte[] copyDataIntoNewDescriptor(byte[] file, int indexInIOSystem) {
		// Create a new array of length filename + index
		byte[] newEntry = new byte[Info.WIDTH_OF_FILE_NAME_AND_INDEX];

		// This loop adds the file name into the new descriptor
		for (int i = 0; i < file.length; i++)
			newEntry[i] = file[i];

		// This adds the index of the descriptor into the new descriptor
		PackableMemory.modifiedPack(newEntry, indexInIOSystem,
				Info.LOCATION_OF_INDEX);

		return newEntry;
	}

	// Takes an io index, a write to location and the index of the descriptor to
	// write a new descriptor block into the iosystem; creates new descriptor
	private int writeDescriptorToIOSystem(int indexInIOSystem, String filename,
			int position) {
		// Create a new descriptor block at the specified index
		createEmptyDescriptorBlock(indexInIOSystem);

		// Adds the file name and location index into a new descriptor
		byte[] newEntry = copyDataIntoNewDescriptor(filename.getBytes(),
				indexInIOSystem);

		// Seek to where we want to begin writing, then write the new descriptor
		lseek(0, position * Info.WIDTH_OF_FILE_NAME_AND_INDEX);
		return write(0, newEntry, 0);

	}

	// Gets the 4 byte name of the file in the specified byte array
	private byte[] getFileName(byte[] file) {
		byte[] fileName = new byte[Info.SINGLE_ENTRY_SIZE];
		for (int i = 0; i < Info.SINGLE_ENTRY_SIZE; i++)
			fileName[i] = file[i];

		return fileName;
	}

	// Reads the length from the open file table and adds it into the descriptor
	private void addLengthOntoFileName(byte[] descriptor, int location,
			int indexInBuffer) {
		// The current length must be read from the open file table in case it
		// changed
		int length = readDescriptorData(indexInBuffer, Info.DESC_LENGTH);

		// We put the possibly modified length back into the descriptor for
		// future re-adding to the iosystem. Yay for all objects passed by
		// reference!!
		PackableMemory.modifiedPack(descriptor, length, location);

	}

	// Updates length of descriptor if modified, adds it back to the iosystem,
	// and clears its spot in the open file table
	private void writeToIOAndDeleteFromOpenFileTable(int location,
			int indexInIOSystem, int indexInBuffer) {
		// reads the original descriptor in case the one that is outstanding in
		// the open file table has been modified
		byte[] descriptor = ioSystem.read_block(indexInIOSystem);

		// Reads the length from the open file table and adds it
		// into the descriptor
		addLengthOntoFileName(descriptor, location, indexInBuffer);

		// Write the updated descriptor back to the io system
		ioSystem.write_block(indexInIOSystem, descriptor);

		// Clears the entry in the open file table
		nullOpenFileTable(indexInBuffer);

		// deletes the position in the bitmap because the file is no longer open
		bitmap.deletePosition(indexInBuffer);

	}

	// Overwrites the file in the io system, erases the bitmap placeholder, and
	// closes the specified file
	void closeDestroyedFile(String fileName) {
		// Get file name to compare with open files
		// String fileName = new String(getFileName(fileData));
		fileName = fileName.trim();

		// Search for file
		for (int i = 0; i < Info.OPEN_FILE_TABLE_SIZE; i++) {
			// We want to read the data into the open file table buffers if the
			// index is greater than one
			if (i > 1)
				readDescriptorData(i, Info.DESC_INDEX);

			// We want to search for the destroyed file
			if (i < openFiles.size()
					&& fileName.equals(openFiles.get(i).fileName.trim())) {

				// We must overwrite where the destroyed file was saved so
				// we can open new files in its place
				int indexInIOSystem = openFiles.get(i).indexInIOSystem;
				ioSystem.write_block(indexInIOSystem,
						initializeEmpty(Info.BLOCK_LENGTH));
				bitmap.deletePosition(indexInIOSystem);

				close(indexInIOSystem);
			}
		}
	}

	// Finds the corresponding byte depending on the location, position, and the
	// jump from the original position. Used when reading files
	private byte addToReadByteArray(int location, int position, int move) {
		// This gives us how far from the start of the entry we are
		int posInBlock = (position + move) % Info.LOGICAL_BLOCKS;
		return buffer[location][posInBlock];
	}

	// Removes the file at the indexes from the open index storage array and
	// writes the updated information to the system
	private void handleClosing(int indexInIOSystem, int indexInBuffer) {
		for (int i = 0; i < openFiles.size(); i++) {
			OpenFile open = openFiles.get(i);
			if (open.indexInBuffer == indexInBuffer) {
				// writeToDirectory(indexInBuffer, open.position);

			}
		}

		// finds the location of the block based on the index in open file table
		int locationOfBlock = (readDescriptorData(indexInBuffer, Info.POS) / Info.POS) + 1;

		// This writes the given information from the open file table to the io
		writeToDirectory(indexInBuffer, locationOfBlock);
	}

	// Writes current block into buffer and reads the next block into the open
	// file table buffr
	private int refreshTempBuffer(int location, int block, int nextBlock) {
		// write the buffer to disk
		writeTempDescriptor(location, block);

		// read the next block into buffer
		readTempDescriptor(location, nextBlock);

		// this number is incremented before passed in and we need to use it
		return block;
	}

	// Change the block we are currently only
	private int refreshCurrentBlock(int block, int indexInIOSystem) {

		byte[] descriptor = ioSystem.read_block(indexInIOSystem);
		int jump = (block + 1) * Info.LOCATION_OF_INDEX;

		// Next block is less than total blocks & the next block doesnt exist
		if (block + 1 < Info.FILE_BLOCKS && descriptor[jump] == Info.NULL) {

			// Create new empty block
			if (createEmptyDescriptorBlock(indexInIOSystem) == Errors.FAIL)
				return Errors.FAIL;
		}

		return Errors.SUCCESS;
	}

	// Updates the descriptor position and the length stored
	private int updateDescInfo(int indexInBuffer, int position, int move) {

		// Set new position
		setDescriptorData(indexInBuffer, move + position, Info.POS);

		// Set new length based on old length
		int len = readDescriptorData(indexInBuffer, Info.DESC_LENGTH);
		setDescriptorData(indexInBuffer, len + move, Info.DESC_LENGTH);

		return move;
	}

	// Adds byte to appropriate position in the open file table
	private byte addToBuffer(int indexInBuffer, int position, byte data) {
		// Place the byte into the open file table
		// position % logical blocks give the exact index between 0 and 64
		return buffer[indexInBuffer][position % Info.LOGICAL_BLOCKS] = data;
	}

	// Finds the index where the data needs to be written
	private int getLocation(int location, int block) {

		// Retrieves descriptor index based on location
		int index = getIndexFromNum(location, block);

		// If it fails then create a new descriptor to put data
		if (index == Errors.FAIL)
			index = createEmptyDescriptorBlock(location);

		return index;
	}

	// Returns the number of files currently open
	public int numOpenFiles() {
		return openFiles.size();
	}

	// Write the updated descriptor back into the io system
	public void writeTempDescriptor(int indexInBuffer, int block) {
		for (int i = 0; i < openFiles.size(); i++) {
			OpenFile openFile = openFiles.get(i);
			if (openFile.indexInBuffer == indexInBuffer) {
			}
		}

		byte[] buf = Arrays.copyOfRange(buffer[indexInBuffer], 0,
				Info.BLOCK_LENGTH);

		// write the data block in the correct index
		ioSystem.write_block(getLocation(indexInBuffer, block), buf);
	}

	// reads data into open file table
	public void readTempDescriptor(int indexInBuffer, int block) {

		// Gets data from specified location in the system
		byte[] data = ioSystem.read_block(getLocation(indexInBuffer, block));

		// Add new data into open file table
		for (int i = 0; i < data.length; i++) {
			buffer[indexInBuffer][i] = data[i];
		}
	}

}
