/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

// Important constants for use throughout the program
public class Info {
	// Name of the output file = my student number
	public static final String OUTPUT_FILE = "26883102.txt";

	// Name of the input file
	public static final String INPUT_FILE = "input.txt";

	// The empty byte is represented by -1, so it isn't confused with a block
	// that is occupied
	public static final byte NULL = -1;

	// Number of blocks in the ldisk
	public static final int LOGICAL_BLOCKS = 64;

	// Length of individual blocks in the ldisk
	public static final int BLOCK_LENGTH = 64;

	// Number of descriptors: 4 per block (6 blocks total)
	public static final int TOTAL_DESC_SPACE = 24;

	// Each descriptor is 16 bytes
	public static final int DESC_SIZE = 16;

	// four total files
	public static final int OPEN_FILE_TABLE_SIZE = 4;

	// Each space consists of a buffer, position, index, and length
	public static final int OPEN_FILE_TABLE_SPACE_SIZE = 64 + 4 + 4 + 4;

	// Position of file
	public static final int POS = 64;

	// Index of file
	public static final int DESC_INDEX = 68;

	// Length of file
	public static final int DESC_LENGTH = 72;

	// Number of blocks belonging to files
	public static final int FILE_BLOCKS = 3;

	// Size of file
	public static final int FILE_SIZE = 64 * 3;

	// Within each descriptor, the index is located at the 4th position
	public static final int LOCATION_OF_INDEX = 4;

	// Width of file name and index together is 4 bytes + 4 bytes
	public static final int WIDTH_OF_FILE_NAME_AND_INDEX = 8;

	// Size of single int / char is 4 bytes
	public static final int SINGLE_ENTRY_SIZE = 4;

	// Size of buffer
	public static final int BUFFER_SIZE = 5;

	// Number of open files allowed
	public static final int MAX_OPEN_FILES = 3;

}
