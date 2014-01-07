package your;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TCPChannel implements Channel {
	
	Socket mySocket;
	ObjectOutputStream outstream;
	ObjectInputStream instream;
	
	public TCPChannel(Socket mySocket) throws IOException{
		//mySocket = new Socket(address, tcpport);
		this.mySocket = mySocket;
		outstream = new ObjectOutputStream(mySocket.getOutputStream());
		instream = new ObjectInputStream(mySocket.getInputStream());
	}

	@Override
	public byte[] receive() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return (byte[]) instream.readObject();
	}

	@Override
	public void send(byte[] message) throws IOException {
		// TODO Auto-generated method stub
		outstream.writeObject(message);

	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		mySocket.close();
		outstream.close();
		instream.close();
	}

}
