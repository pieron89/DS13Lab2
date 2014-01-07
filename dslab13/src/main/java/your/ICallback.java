package your;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICallback extends Remote{
	
	public void notifyMe(String notify) throws RemoteException;

}
