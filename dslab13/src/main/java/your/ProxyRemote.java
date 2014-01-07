package your;

import java.util.ArrayList;
import java.rmi.server.UnicastRemoteObject;

public class ProxyRemote extends UnicastRemoteObject implements IRemote {
	
	Proxy proxy;

	public ProxyRemote(Proxy proxy) throws java.rmi.RemoteException{
		super();
		this.proxy=proxy;
	}
	
	@Override
	public int readQuorum() {
		return proxy.getReadQuorum();
		//return 0;
	}

	@Override
	public int writeQuorum() {
		return proxy.getWriteQuorum();
		//return 0;
	}

	@Override
	public ArrayList<String> topThreeDownloads() {
		//return proxy.getTopThreeDownloads();
		return null;
	}

	@Override
	public void subscribe(String username, String filename, int count, Object callback) {
		//proxy.subscribe(filename, count, callback);
	}

	@Override
	public byte[] getProxyPublicKey() {
		return proxy.getProxyPublicKey();
		//return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key) {
		proxy.setUserPublicKey(username, key);
	}

}
