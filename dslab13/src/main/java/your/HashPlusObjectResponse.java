package your;

import message.Response;

public class HashPlusObjectResponse {
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
