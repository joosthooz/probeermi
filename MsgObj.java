package probeermi;

import java.util.LinkedList;

/**
 * Objects of this class will contain a message, including the buffer and timestamp.
 * @author Joost
 *
 */
public class MsgObj
{
	private final String message;
	private final LinkedList<BufferItem> buffer;
	private final int[] timeVector;
	
	MsgObj(String msg, LinkedList<BufferItem> buf, int[] t)
	{
		message = msg;
		buffer = buf;
		timeVector = t;
	}
	
	public final String getMessage()
	{
		return message;
	}

	public final LinkedList<BufferItem> getBuffer()
	{
		return buffer;
	}

	public final int[] getTimeVector()
	{
		return timeVector;
	}
	
}
