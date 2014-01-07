package your;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Callback extends UnicastRemoteObject implements ICallback {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6867074136165480976L;

	protected Callback() throws RemoteException {
		super();
	}

	@Override
	public void notifyMe(String notify) {
		System.out.println(notify);		
	}

}
