package your;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class RSAChannel extends ChannelDecorator{

	//private String pathToPublicKey;
	private PublicKey publicKey;
	private PrivateKey privateKey;

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
			//PEMReader in = new PEMReader(new FileReader(pathToPublicKey)); 
			//PublicKey publicKey = (PublicKey) in.readObject();
			//in.close();
			// MODE is the encryption/decryption mode 
			// KEY is either a private, public or secret key
			crypt.init(Cipher.DECRYPT_MODE, privateKey);
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
			//PEMReader in = new PEMReader(new FileReader(pathToPublicKey)); 
			//PublicKey publicKey = (PublicKey) in.readObject();
			//in.close();
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

	public void setPublicKey(String pathToPublicKey) {
		try {
			PEMReader in = new PEMReader(new FileReader(pathToPublicKey));
			publicKey = (PublicKey) in.readObject();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setPrivateKey(String pathToPublicKey, final String password){
		String pathToPrivateKey = null;
		PEMReader in;
		try {
			in = new PEMReader(new FileReader(pathToPrivateKey), new PasswordFinder() {

				@Override
				public char[] getPassword() {
					// reads the password from standard input for decrypting the private key
					//System.out.println("Enter pass phrase:");
					return password.toCharArray();
				}

			});
			KeyPair keyPair = (KeyPair) in.readObject(); 
			privateKey = keyPair.getPrivate();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
