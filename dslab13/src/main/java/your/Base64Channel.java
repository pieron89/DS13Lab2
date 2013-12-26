package your;

import java.io.IOException;
import org.bouncycastle.util.encoders.Base64;

public class Base64Channel extends ChannelDecorator {

	public Base64Channel(Channel decoratorChannel) {
		super(decoratorChannel);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] receive() throws IOException, ClassNotFoundException {
		//TODO decode
		// decode from Base64 format
		return Base64.decode(super.receive());
	}

	@Override
	public void send(byte[] message) throws IOException {
		// TODO encode
		// encode into Base64 format 
		//byte[] encryptedMessage = message;
		//byte[] base64Message = Base64.encode(encryptedMessage);
		super.send(Base64.encode(message));

	}

}
