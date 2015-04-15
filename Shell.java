/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

// Reads file, and seperates lines and commands accordingly.
public class Shell {
	String inputFile;
	TaskAnalyzer taskAnalyzer;

	// Shell is initialized with an input file.
	// Initializes new task analyzer which will handle the interaction
	// between the FileSystem and the commands.
	public Shell(String inputFile) {
		this.inputFile = inputFile;
		this.taskAnalyzer = new TaskAnalyzer();
	}

	// Calls appropriate function to analyze input
	public String readLine(String inputLine) throws IOException {

		// Creates the original file system when given the first in command
		if (inputLine.equals("in"))
			return taskAnalyzer.generateNewFileSystem();

		if (!taskAnalyzer.inCalled())
			return Errors.ERROR;

		// Index 0 will be the command, the rest will be the extra information
		String[] line = inputLine.split(" ");

		try {
			switch (line[0]) {
			case "cr":
				return taskAnalyzer.crCommand(line);

			case "de":
				return taskAnalyzer.deCommand(line);

			case "op":
				return taskAnalyzer.opCommand(line);

			case "cl":
				return taskAnalyzer.clCommand(line);

			case "rd":
				return taskAnalyzer.rdCommand(line);

			case "wr":
				return taskAnalyzer.wrCommand(line);

			case "sk":
				return taskAnalyzer.skCommand(line);

			case "dr":
				return taskAnalyzer.drCommand(line);

			case "in":
				return taskAnalyzer.inCommand(line);

			case "sv":
				return taskAnalyzer.svCommand(line);

			default:
				return Errors.ERROR;
			}
		} catch (Exception e) {
			return Errors.ERROR;
		}

	}

	// Receives the results from the line evaluation and creates output string
	private String parseCommands(BufferedReader buffReader) throws IOException {
		StringBuilder builder = new StringBuilder();
		String line = buffReader.readLine();

		while (line != null) {
			if (line.equals("") || line == null) {
				builder.append("\n");
			} else {
				builder.append(readLine(line.trim()));
				builder.append("\n");
			}
			line = buffReader.readLine();
		}

		return builder.toString();

	}

	// Beings parsing file, initializing readers and saves file to disk
	public void parseFile() {
		BufferedReader buffReader = null;
		FileReader file = null;
		String out = "";

		try {
			file = new FileReader(inputFile);
			buffReader = new BufferedReader(file);
			out = parseCommands(buffReader);
			buffReader.close();
			file.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fileSave(out);
		}
	}

	// Saves file holding all outputs to disk
	public void fileSave(String fileBody) {
		try {
			PrintWriter out = new PrintWriter(Info.OUTPUT_FILE);
			out.write(fileBody);
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
