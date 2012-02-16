package probeermi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Objects of this class will contain a message, including the buffer and timestamp.
 * @author Joost
 *
 */
public class MsgObj implements Serializable 
{
	/**
	 * 
	 */

	private final String message;
	private final ConcurrentLinkedQueue<BufferItem> buffer;
	private final int[] timeVector;
	
	MsgObj(String msg, ConcurrentLinkedQueue<BufferItem> buf, int[] t)
	{
		message = msg;
		buffer = buf;
		timeVector = t;
	}
	
	public final String getMessage()
	{
		return message;
	}

	public final ConcurrentLinkedQueue<BufferItem> getBuffer()
	{
		return buffer;
	}

	public final int[] getTimeVector()
	{
		return timeVector;
	}
	
	public void print(){
		System.out.print("{" + message + ", ");
		for (BufferItem b : buffer){
			System.out.print("(");
			b.print();
			System.out.print(")");
		}
		System.out.print( ", " + Arrays.toString(timeVector));
		System.out.print("}");
	}
	
	
	private static final long serialVersionUID = 8460313687966889668L;
}

