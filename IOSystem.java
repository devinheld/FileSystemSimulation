/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

// Back end of the file system. Interactions with the ldisk
public class IOSystem {
	// LDisk is a nested byte array
	private byte[][] lDisk;

	// Initialize new IO
	IOSystem() {
		// number of blocks is 64, with each having 64 length
		this.lDisk = new byte[Info.LOGICAL_BLOCKS][Info.BLOCK_LENGTH];

		// fill the entire ldisk with null bytes so comparison is easy
		for (int a = 0; a < Info.LOGICAL_BLOCKS; a++) {
			for (int b = 0; b < Info.BLOCK_LENGTH; b++)
				lDisk[a][b] = Info.NULL;
		}
	}

	// Initialize the IO given a file string input
	IOSystem(String file) {
		// number of blocks is 64, with each having 64 length
		this.lDisk = new byte[Info.LOGICAL_BLOCKS][Info.BLOCK_LENGTH];

		// Initialize entire ldisk to NULL bytes so comparison is easy
		for (int a = 0; a < Info.LOGICAL_BLOCKS; a++) {
			for (int b = 0; b < Info.BLOCK_LENGTH; b++)
				lDisk[a][b] = Info.NULL;
		}

		// Gather the data from the specified file and populates the ldisk
		// accordingly
		fillldiskFromFile(file);
	}

	// Read entire block and return it
	public byte[] read_block(int i) {
		return lDisk[i];
	}

	// Write block p to ldisk in position i
	public void write_block(int i, byte[] p) {
		lDisk[i] = p;
	}

	// Saves LDisk to specified file.
	public void saveLDisk(String file) {
		PrintWriter printer = null;
		try {
			// Sets up the printwriter to write to the specified file
			printer = new PrintWriter(file);

			// Generates the body of the file
			String body = createFileBody();

			// Writes body to the specified file using the printwriter
			printer.write(body);
			printer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Creates the body of the file
	private String createFileBody() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < Info.LOGICAL_BLOCKS; i++) {
			for (int j = 0; j < Info.BLOCK_LENGTH; j++)
				// Adds the individual bytes in the ldisk to the string builder
				// Each byte is seperated by a single space for easy parsing
				// later on
				builder.append(lDisk[i][j] + " ");

			// This additional space will create a second space at the end of
			// the byte array, indicating a new block has been started. This
			// allows for easy parsing later on
			builder.append(" ");
		}
		return builder.toString();
	}

	// Reads through the contents of the string of the file
	private byte[][] analyzeContents(String contents) {
		byte[][] ldisk;
		// Splitting around the double space will get each individual block
		String[] logicalBlocks = contents.split("  ");

		// Initialize new ldisk with 64 blocks of size 64.
		ldisk = new byte[Info.LOGICAL_BLOCKS][Info.BLOCK_LENGTH];

		for (int i = 0; i < logicalBlocks.length; i++) {
			// Splitting by a single space will create an array of the
			// individual bytes in the block
			String[] block = logicalBlocks[i].split(" ");

			// Populate the ldisk array by parsing each individual byte from the
			// split string
			for (int j = 0; j < Info.BLOCK_LENGTH; j++)
				ldisk[i][j] = Byte.parseByte(block[j]);

		}
		return ldisk;

	}

	// Retrieves the body of the specified file
	private String getContents(BufferedReader buffer) throws IOException {
		StringBuilder builder = null;
		builder = new StringBuilder();
		String line = buffer.readLine();

		// Keep reading until there are no more lines left
		while (line != null) {
			builder.append(line);
			line = buffer.readLine();
		}

		return builder.toString();

	}

	// Creates the ldisk from the specified file
	byte[][] fillldiskFromFile(String file) {
		byte[][] ldisk = null;
		BufferedReader buffer = null;
		FileReader reader = null;

		try {
			reader = new FileReader(file);
			buffer = new BufferedReader(reader);

			// Gets contents of file in one big string
			String contents = getContents(buffer);

			// Creates the ldisk through parsing the file string
			ldisk = analyzeContents(contents);

			buffer.close();
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ldisk;
	}

}