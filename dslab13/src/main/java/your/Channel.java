package your;

import java.io.IOException;

public interface Channel {
	
	public byte[] receive() throws IOException, ClassNotFoundException;
	
	public void send(byte[] message) throws IOException;

}
