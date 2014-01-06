package your;

import java.rmi.Remote;

public interface ICallback extends Remote{
	
	public void notifyMe(String notify);

}
