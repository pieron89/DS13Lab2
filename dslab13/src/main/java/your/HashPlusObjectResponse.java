package your;

import java.io.Serializable;

import message.Response;

public class HashPlusObjectResponse implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4312613797942122759L;
	private final Response response;
	private final byte[] hash;
	
	public HashPlusObjectResponse(byte[] hash, Response response){
		this.hash = hash;
		this.response = response;
	}

	public Response getResponse() {
		return response;
	}

	public byte[] getHash() {
		return hash;
	}
}
