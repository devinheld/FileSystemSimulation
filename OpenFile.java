/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

// Simple object to track items within an open file to allow for easier search
// and modification
public class OpenFile {
	public String fileName;
	public int length;
	public int descIndex;
	public int indexInBuffer;
	public int indexInIOSystem;
	public int position = 0;

	// Buffer to allow simpler modification of file data, and to make it easier
	// to push back to the io system.
	public byte[] buffer = new byte[Info.OPEN_FILE_TABLE_SPACE_SIZE];

	OpenFile() {
		// Null everything in the buffer so we can compare it to actual bytes
		for (int i = 0; i < Info.OPEN_FILE_TABLE_SPACE_SIZE; i++)
			buffer[i] = Info.NULL;
	}

	OpenFile(String fileName, int length, int descIndex) {
		this.fileName = fileName;
		this.length = length;
		this.descIndex = descIndex;
	}

}
