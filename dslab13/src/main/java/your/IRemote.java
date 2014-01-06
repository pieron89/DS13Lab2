package your;

import java.rmi.Remote;
import java.util.ArrayList;

public interface IRemote extends Remote{
	
	public Object getReadQuorum();
	
	public Object getWriteQuorum();
	
	public ArrayList<String> getTopThree();
	
	public void subscribe(String user, String file);
	
	public byte[] gatherPublicKey();
	
	public void transmitPublicKey(byte[] key);

}
