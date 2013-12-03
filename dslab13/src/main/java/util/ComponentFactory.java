package util;

import cli.Shell;
import client.IClientCli;
import proxy.IProxyCli;
import server.IFileServerCli;
import your.Client;
import your.Fileserver;
import your.Proxy;
import your.runClient;

/**
 * Provides methods for starting an arbitrary amount of various components.
 */
public class ComponentFactory {
	/**
	 * Creates and starts a new client instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IClientCli startClient(Config config, Shell shell) throws Exception {
		// TODO: create a new client instance (including a Shell) and start it
		Client client = new Client(config, shell);
		Thread clientthread = new Thread(client);
		clientthread.start();
		return client;
		//return null;
	}

	/**
	 * Creates and starts a new proxy instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IProxyCli startProxy(Config config, Shell shell) throws Exception {
		// TODO: create a new proxy instance (including a Shell) and start it
		Proxy proxy = new Proxy(config, shell);
		Thread proxythread = new Thread(proxy);
		proxythread.start();
		return proxy;
		//return null;
	}

	/**
	 * Creates and starts a new file server instance using the provided {@link Config} and {@link Shell}.
	 *
	 * @param config the configuration containing parameters such as connection info
	 * @param shell  the {@code Shell} used for processing commands
	 * @return the created component after starting it successfully
	 * @throws Exception if an exception occurs
	 */
	public IFileServerCli startFileServer(Config config, Shell shell) throws Exception {
		// TODO: create a new file server instance (including a Shell) and start it
		Fileserver fileserver = new Fileserver(config, shell);
		Thread fileserverthread = new Thread(fileserver);
		fileserverthread.start();
		return fileserver;
		//return null;
	}
}
