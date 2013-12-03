package your;

import util.Config;
import cli.Shell;
public class runProxy {

	public static void main(String[] args) {
		// push test git
		Shell proxyshell = new Shell("proxy", System.out, System.in);
		Proxy proxy = new Proxy(new Config("proxy"), proxyshell);
		proxy.run();
	}

}