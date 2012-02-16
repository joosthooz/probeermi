package probeermi;

import java.io.Serializable;
import java.util.Arrays;

public class BufferItem implements Serializable
{
	/**
	 * 
	 */

	final int[] timeVector;
	final int destination;
	
	public BufferItem(int[] time, int dest)
	{
		timeVector = time;
		this.destination = dest;
	}

	public final int[] getTimeVector()
	{
		return timeVector;
	}
	
	public final int getTimeVector(int index)
	{
		return timeVector[index];
	}

	public final int getDestination()
	{
		return destination;
	}
	
	public void print(){
		System.out.print(destination + ";" + Arrays.toString(timeVector));
	}

	
	private static final long serialVersionUID = 1436495160511918082L;
}
