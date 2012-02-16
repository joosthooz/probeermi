package probeermi;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This class will contain remote methods.
 * @author Joost
 * 
 * TODO: waar moeten we de tijd increasen? send en receive waarschijnlijk.
 * Ook voor de source node bij het ontvangen van een msg, denk ik.
 * TODO: Pending msg buffer B
 * TODO: Random Delay in send
 *
 */
public class RemMethClass extends UnicastRemoteObject implements RMI_interface, Runnable
{
	String name;
	int nodeNr;
	boolean debug = false;
	boolean running = true;
	int nrOfNodes;
	int[] timeVector;
	
	ConcurrentLinkedQueue<BufferItem> S_buffer;
	ConcurrentLinkedQueue<MsgObj> B_buffer;
	ConcurrentLinkedQueue<MsgObj> history;
	
	/*
	 * Constructor - Initialize all the fields, and register the remote interface
	 */
	protected RemMethClass(int nodeNr, int n) throws RemoteException
	{
		this.name = "node"+nodeNr;
		this.nodeNr = nodeNr;
		nrOfNodes = n;
		initTimeVector(n);
		S_buffer = new ConcurrentLinkedQueue<BufferItem>();
		B_buffer = new ConcurrentLinkedQueue<MsgObj>();
		history = new ConcurrentLinkedQueue<MsgObj>(); //for delivery
		try
		{
			java.rmi.Naming.bind(name, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-2);
		}
	}
	
	private void initTimeVector(int n)
	{
		timeVector = new int[n];
		for (int i = 0; i < n; i++)
		{
			timeVector[i] = 0;
		}
	}
	
	@Override
	public void print(String text) throws RemoteException
	{
		System.out.println(name+" : "+text);
	}
	
	public void printHistory(){
		for (MsgObj m : history){
			m.print();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see probeermi.RMI_interface#receive_msg(probeermi.MsgObj)
	 */
	@Override
	public synchronized void receive_msg(MsgObj msg, int sleepTime) throws RemoteException
	{	
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("Received by " + name + ": ");
		msg.print();
		System.out.println();
		//fist, check if msg is deliverable, else store in buffer
		if (deliverable(msg)) 
		{
			deliver(msg);
			
			//check if msgs in the buffer can be delivered
			boolean changed = true;
			while (changed)
			{
				changed = false;
				for (MsgObj m : B_buffer)
				{
					if (deliverable(m))
					{
						deliver(m);
						changed = true;
					}
				}
			}
		}
		else store(msg);
	}
	
	/*
	 * Check whether a msg is deliverable or should be stored in the buffer
	 * 
	 */
	public boolean deliverable(MsgObj msg)
	{
		boolean d = true;
		for (BufferItem b : msg.getBuffer())
		{
			if (b.getDestination() == nodeNr && (!vectorLTE(b.getTimeVector())))
			{
				d = false;
			}
		}
		return d;
	}
	
	/*
	 * returns true if all timestamps in the bufferitem are smaller than or equal to the local
	 * value
	 */
	public boolean vectorLTE(int[] bufitmtime)
	{
		boolean ret = true;
		for (int i = 0; i < nrOfNodes; i++)
		{
			if (bufitmtime[i] > timeVector[i])
			{
				debug("i = " + i + ", Buf: " + bufitmtime[i] + ", timeVector: " + timeVector[i]);
				ret = false;
			}
		}
		return ret;
	}
	
	/*
	 * Deliver message; for now, store in the LinkedList.
	 * We can check afterwards whether the list is ordered.
	 * 
	 * update the timeVector with info from the delivered msg
	 * Merge buffers
	 */
	public void deliver(MsgObj msg)
	{
		//deliver the message
		System.out.println("Deliver to " + name + ": " + msg.getMessage() );
		history.add(msg);

		//merge S_buffer
		insertMax(msg);
		
		//update local timeVector
		
			for (int i = 0; i < nrOfNodes; i++)
			{
				timeVector[i] = Math.max(timeVector[i]+1, msg.getTimeVector()[i]);
			}
		
		debug("voor:"+timeVector[nodeNr-1]);
		//increase local clock
		//incTime();
		debug("na:"+timeVector[nodeNr-1]);
	}
	
	/*
	 * Insertmax merges the buffer in the msg with the local S_buffer
	 */
	public void insertMax(MsgObj msg)
	{
		boolean inlocalbuffer = false;
		for (BufferItem b : msg.getBuffer())
		{
			for (BufferItem lb : S_buffer)
			{
				if (b.destination == lb.destination)
				{
					inlocalbuffer = true;
					lb = max(b, lb); //replace the element in the S_buffer with the pairwise max
				}
			}
			//if not exist in either buffer, store the one that exists
			if (!inlocalbuffer)
			{
				S_buffer.add(b);
			}
		}
	}
	
	public BufferItem max(BufferItem a, BufferItem b)
	{
		BufferItem n = new BufferItem(new int[nrOfNodes], a.destination);
		for (int i = 0; i < nrOfNodes; i++)
		{
			n.timeVector[i] = Math.max(a.timeVector[i], b.timeVector[i]);
		}
		return n;
	}
	/*
	 * store a msg in the buffer 
	 */
	public void store(MsgObj msg)
	{
		B_buffer.add(msg);
	}
	
	public synchronized void send(String s, int destination, int sleepTime)
	{	
		incTime();
		MsgObj msg = new MsgObj(s,S_buffer,timeVector);
		try
		{
			RMI_interface destObject = (RMI_interface) java.rmi.Naming.lookup("node"+destination);
			debug("Verstuurt " + s);
			
			destObject.receive_msg(msg, sleepTime);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
		//add dest and timevector (bufferitem) to buffer
		BufferItem itm = new BufferItem(timeVector.clone(), destination);
		bufferInsert(itm);
	}
	
	
	/*
	 * Insert new item into the buffer, remove any old elements
	 */
	public void bufferInsert(BufferItem itm)
	{
		for (BufferItem b : S_buffer)
		{
			if (b.destination == itm.destination)
			{
				S_buffer.remove(b);
			}
		}

		S_buffer.add(itm);
	}
	
	/*
	 * Increase local timestamp by 1
	 */
	public void incTime()
	{
		timeVector[nodeNr-1]++;
	}

	@Override
	public void run()
	{
		while(running)
		{
				
			try
			{
				Thread.sleep(2000);
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
	public void DIE()
	{
		running = false;
		debug("Dieing...");
		try	{
			java.rmi.Naming.unbind(name);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private void debug(String msg)
	{
		if (debug) System.out.println(name+" : "+msg);
	}
	
	private void debug(ConcurrentLinkedQueue<BufferItem> list)
	{
		if (debug)
		{
			for(BufferItem b: list)
			{
				debug(Arrays.toString(b.timeVector));
			}
		}
	}
	
	private static final long serialVersionUID = -4676446028805100856L;
}
