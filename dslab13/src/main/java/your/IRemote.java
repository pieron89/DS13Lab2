package your;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.ArrayList;

public interface IRemote extends Remote{
	
	public int readQuorum() throws RemoteException;
	
	public int writeQuorum() throws RemoteException;
	
	public String topThreeDownloads() throws RemoteException;
	
	public void subscribe(String username, String filename, int count, ICallback callback) throws RemoteException;
	
	public String getProxyPublicKey() throws RemoteException;
	
	public void setUserPublicKey(String username, String key) throws RemoteException;


}
