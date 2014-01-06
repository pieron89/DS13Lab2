package your;

import java.rmi.Remote;
import java.util.ArrayList;

public interface IRemote extends Remote{
	
	public int readQuorum();
	
	public int writeQuorum();
	
	public ArrayList<String> topThreeDownloads();
	
	public void subscribe(String filename, int count, Object callback);
	
	public byte[] getProxyPublicKey();
	
	public void setUserPublicKey(String username, byte[] key);

}
