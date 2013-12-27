package your;

import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.openssl.PEMReader;

public class RSAChannel extends ChannelDecorator{
	
	private String pathToPublicKey;

	public RSAChannel(Channel decoratorChannel) {
		super(decoratorChannel);
		//this.pathToPublicKey = pathToPublicKey;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public byte[] receive() throws IOException, ClassNotFoundException {
		//TODO decipher
		try {
			Cipher crypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			PEMReader in = new PEMReader(new FileReader(pathToPublicKey)); 
			PublicKey publicKey = (PublicKey) in.readObject();
			in.close();
			// MODE is the encryption/decryption mode 
			// KEY is either a private, public or secret key
			crypt.init(Cipher.DECRYPT_MODE, publicKey);
			return crypt.doFinal(super.receive());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.receive();
	}

	@Override
	public void send(byte[] message) throws IOException {
		// TODO encrypt
		try {
			Cipher crypt = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			PEMReader in = new PEMReader(new FileReader(pathToPublicKey)); 
			PublicKey publicKey = (PublicKey) in.readObject();
			in.close();
			// MODE is the encryption/decryption mode 
			// KEY is either a private, public or secret key
			crypt.init(Cipher.ENCRYPT_MODE, publicKey);
			super.send(crypt.doFinal(message));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void setPathtoPublicKey(String pathToPublicKey){
		this.pathToPublicKey = pathToPublicKey;
	}

}
