package probeermi;

import java.io.Serializable;

public class SendIntent implements Serializable 
{

	private final int destination;
	private final int sleepTime;
	private final String message;
	
	SendIntent(int destination, String message, int sleepTime)
	{
		this.destination = destination;
		this.message = message;
		this.sleepTime = sleepTime;
	}
	
	public int getDestination() {
		return destination;
	}

	public int getSleepTime()
	{
		return sleepTime;
	}
	public String getMessage() {
		return message;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8418168121762874268L;

}
