package your;

public class Callback implements ICallback {

	@Override
	public void notifyMe(String notify) {
		System.out.println(notify);		
	}

}
