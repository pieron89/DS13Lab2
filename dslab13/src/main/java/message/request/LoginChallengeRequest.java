package message.request;

import message.Request;

public class LoginChallengeRequest implements Request{
	private static final long serialVersionUID = 4109424660866997667L;
	
	private final String username;
	private final byte[] clientChallenge;

	public LoginChallengeRequest(String username, byte[] clientChallenge) {
		this.username = username;
		this.clientChallenge = clientChallenge;
	}

	public String getUsername() {
		return username;
	}

	public byte[] getclientChallenge() {
		return clientChallenge;
	}

	@Override
	public String toString() {
		return String.format("!login %s %s", getUsername(), getclientChallenge().toString());
	}
	
}
