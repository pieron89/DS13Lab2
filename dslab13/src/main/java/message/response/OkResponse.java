package message.response;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import message.Response;

public class OkResponse implements Response {
	private static final long serialVersionUID = -8738194278649227047L;
	
	private final byte[] clientChallenge;
	private final byte[] proxyChallenge;
	private final byte[] secretKey;
	private final byte[] iv;

	public OkResponse(byte[] clientChallenge, byte[] proxyChallenge, byte[] secretKey, byte[] iv) {
		this.clientChallenge = clientChallenge;
		this.proxyChallenge = proxyChallenge;
		this.secretKey = secretKey;
		this.iv = iv;
	}

	public byte[] getClientChallenge() {
		return clientChallenge;
	}
	
	public byte[] getProxyChallenge() {
		return proxyChallenge;
	}
	
//	public SecretKey getSecretKey() {
//		return secretKey;
//	}
	
	public byte[] getSecretKey() {
		return secretKey;
	}
	
	public byte[] getIv() {
		return iv;
	}
	
	@Override
	public String toString() {
		return String.format("!ok  %s %s %s %s", getClientChallenge().toString(), 
				getProxyChallenge().toString(), 
				getSecretKey().toString(), 
				getIv().toString());
	}

}
