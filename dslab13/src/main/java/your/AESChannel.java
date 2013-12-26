package your;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class AESChannel extends ChannelDecorator {

	private SecretKey secretKey;
	private byte[] iv;
	
	public AESChannel(Channel decoratorChannel, SecretKey secretKey, byte[] iv) {
		super(decoratorChannel);
		this.secretKey = secretKey;
		this.iv = iv;
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] receive() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		try {
			Cipher crypt = Cipher.getInstance("AES/CTR/NoPadding");
			// MODE is the encryption/decryption mode 
			// KEY is either a private, public or secret key
			// IV is an init vector, needed for AES 
			crypt.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
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
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void send(byte[] message) throws IOException {
		// TODO Auto-generated method stub
		try {
			Cipher crypt = Cipher.getInstance("AES/CTR/NoPadding");
			// MODE is the encryption/decryption mode 
			// KEY is either a private, public or secret key
			// IV is an init vector, needed for AES 
			crypt.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
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
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
