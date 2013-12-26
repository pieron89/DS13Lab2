package your;

import java.io.IOException;

public abstract class ChannelDecorator implements Channel {
	
	protected Channel decoratorChannel;
	
	public ChannelDecorator(Channel decoratorChannel){
		this.decoratorChannel = decoratorChannel;
	}

	@Override
	public byte[] receive() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return decoratorChannel.receive();
	}

	@Override
	public void send(byte[] message) throws IOException {
		// TODO Auto-generated method stub
		decoratorChannel.send(message);

	}

}
