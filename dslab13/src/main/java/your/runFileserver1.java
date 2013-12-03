package your;

import util.Config;
import cli.Shell;

public class runFileserver1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config filesConfig = new Config("fs1");
		Shell filesShell = new Shell("fs1", System.out, System.in);
		
		Fileserver fileserver1 = new Fileserver(filesConfig, filesShell);
		fileserver1.run();

	}

}
