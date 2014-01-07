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
	}

	@Override
	public int writeQuorum() {
		return proxy.getWriteQuorum();
	}

	@Override
	public String topThreeDownloads() {
		return proxy.getTopThreeDownloads();
	}

	@Override
	public void subscribe(String username, String filename, int count, ICallback callback) {
		proxy.subscribe(username, filename, count, callback);
	}

	@Override
	public byte[] getProxyPublicKey() {
		return proxy.getProxyPublicKey();
	}

	@Override
	public void setUserPublicKey(String username, byte[] key) {
		proxy.setUserPublicKey(username, key);
	}

}
