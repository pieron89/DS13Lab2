package your;

import java.io.Serializable;

import message.Request;

public class HashPlusObjectRequest implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7708844363361222978L;
	private final Request request;
	private final byte[] hash;
	
	public HashPlusObjectRequest(byte[] hash, Request reqest){
		this.hash = hash;
		this.request = reqest;
	}

	public Request getRequest() {
		return request;
	}

	public byte[] getHash() {
		return hash;
	}
}
