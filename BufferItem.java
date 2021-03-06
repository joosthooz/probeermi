package probeermi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.naming.InitialContext;

/**
 * Objects of this class represent an item that is stored in an S_Buffer.
 * it stores a timevector and a destination.
 */
public class BufferItem implements Serializable
{
	final Vector<Integer> timeVector;
	final int destination;
	
	public BufferItem(Vector<Integer> time, int dest)
	{
		timeVector = RemMethClass.initTimeVector(Main.nrOfNodes);
		Collections.copy(timeVector, time);
		this.destination = dest;
	}

	public final Vector<Integer> getTimeVector()
	{
		return timeVector;
	}
	
	public final int getTimeVector(int index)
	{
		return timeVector.get(index);
	}

	public final int getDestination()
	{
		return destination;
	}
	
	public void print(){
		System.out.print(destination + ";" + timeVector.toString());
	}

	
	private static final long serialVersionUID = 1436495160511918082L;
}
