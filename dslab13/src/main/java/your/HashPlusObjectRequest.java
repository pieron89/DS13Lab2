package your;

import message.Request;

public class HashPlusObjectRequest {
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
