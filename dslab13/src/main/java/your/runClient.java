package your;

import cli.Shell;
import util.Config;

public class runClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Config clientConfig = new Config("client");
        Shell clientShell = new Shell("client", System.out, System.in);
		Client client = new Client(clientConfig, clientShell);
		client.run();
	}

}
