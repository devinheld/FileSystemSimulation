/**
 * 
 * @author Devin Held ID: 26883102
 *
 */

// Entry points into the file system and parsing input files
public class Main {
	public static void main(String[] args) {
		Shell shell;

		// If no file was specified in the run configurations, then open the
		// file input.txt
		if (args.length == 0) {
			shell = new Shell(Info.INPUT_FILE);
		} else {
			// Otherwise use the specified input file
			shell = new Shell(args[0]);
		}

		// Begins parsing the file and calls corresponding commands
		shell.parseFile();
	}
}
