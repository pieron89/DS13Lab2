package your;

import util.Config;
import cli.Shell;

public class runFileserver2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config filesConfig = new Config("fs2");
		Shell filesShell = new Shell("fs2", System.out, System.in);
		
		Fileserver fileserver2 = new Fileserver(filesConfig, filesShell);
		fileserver2.run();

	}

}
