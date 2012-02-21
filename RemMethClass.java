package probeermi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This class will contain remote methods.
 * @author Joost
 * 
 * 
 * 
 * TODO: Pending msg buffer B
 * TODO: Random Delay in send
 *
 */
public class RemMethClass extends UnicastRemoteObject implements RMI_interface, Runnable
{
	String name;
	int nodeNr;
	boolean debug = true;
	boolean running = true;
	Vector<Integer> timeVector;
	
	ConcurrentLinkedQueue<SendIntent> sendBuffer; //msgs to be sent by this thread
	ConcurrentLinkedQueue<BufferItem> S_buffer;
	ConcurrentLinkedQueue<MsgObj> B_buffer;
	ConcurrentLinkedQueue<MsgObj> history;
	ConcurrentLinkedQueue<MsgObj> received;
	
	/*
	 * Constructor - Initialize all the fields, and register the remote interface
	 */
	protected RemMethClass(int nodeNr) throws RemoteException
	{
		this.name = "node"+nodeNr;
		this.nodeNr = nodeNr;
		timeVector = initTimeVector(Main.nrOfNodes);
		
		sendBuffer = new ConcurrentLinkedQueue<SendIntent>();
		S_buffer = new ConcurrentLinkedQueue<BufferItem>();
		B_buffer = new ConcurrentLinkedQueue<MsgObj>();
		history = new ConcurrentLinkedQueue<MsgObj>(); //for delivery
		received = new ConcurrentLinkedQueue<MsgObj>();
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
	
	/*
	 * this static method can be called from other classes to create a new timeVector
	 */
	public static Vector<Integer> initTimeVector(int n)
	{
		Vector<Integer> t = new Vector<Integer>(n);
		for (int i = 0; i < n; i++)
		{
			t.add(0);
		}
		return t;
	}
	
	@Override
	public synchronized void print(String text) throws RemoteException
	{
		System.out.println(name+" : "+text);
	}
	
	public synchronized void printHistory(){
		System.out.println("History of "+name+":");
		for (MsgObj m : history){
			m.print();
		}
		System.out.println("\n");
	}
	public synchronized void printReceivedOrder(){
		System.out.println("Received order of "+name+":");
		for (MsgObj m : received){
			m.print();
		}
		System.out.println("\n");
	}
	
	/*
	 * (non-Javadoc)
	 * @see probeermi.RMI_interface#prepareToSend(int, java.lang.String, int)
	 */
	@Override
	public void prepareToSend(int destination, String message, int sleepTime)
	{
		SendIntent intent = new SendIntent(destination, message, sleepTime);
		sendBuffer.add(intent);
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
			e.printStackTrace();
		}
		received.add(msg);
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
	private boolean deliverable(MsgObj msg)
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
	private boolean vectorLTE(Vector<Integer> bufitmtime)
	{
		boolean ret = true;
		for (int i = 0; i < Main.nrOfNodes; i++)
		{
			if (bufitmtime.get(i) > timeVector.get(i))
			{
				debug("i = " + i + ", Buf: " + bufitmtime.get(i) + ", timeVector: " + timeVector.get(i));
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
	private void deliver(MsgObj msg)
	{
		//deliver the message
		history.add(msg);

		//merge S_buffer
		insertMax(msg);
		
		//update local timeVector
		incTime();
		//increase local clock
			for (int i = 0; i < Main.nrOfNodes; i++)
			{
				timeVector.set(i, (Math.max(timeVector.get(i)+1, msg.getTimeVector().get(i))));
			}		
	}
	
	/*
	 * Insertmax merges the buffer in the msg with the local S_buffer
	 */
	private void insertMax(MsgObj msg)
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
	
	private BufferItem max(BufferItem a, BufferItem b)
	{
		Vector<Integer> v = initTimeVector(Main.nrOfNodes);
		
		BufferItem n = new BufferItem(v, a.destination);
		for (int i = 0; i < Main.nrOfNodes; i++)
		{
			n.timeVector.set(i, Math.max(a.timeVector.get(i), b.timeVector.get(i)));
		}
		return n;
	}
	/*
	 * store a msg in the buffer 
	 */
	private void store(MsgObj msg)
	{
		B_buffer.add(msg);
	}
	
	/*
	 * Sends a message to another host, and updates the S_Buffer.
	 * Uses a separate thread for sending to enable the message reception
	 * by the other hosts to be delayed by an arbitrary amount of time.
	 * 
	 * This method is synchronized because it increases the local Time, and
	 * updates the S_Buffer. The TimeVector may not be changed in between these actions.
	 */
	private synchronized void send(String s, int destination, int sleepTime)
	{	
		incTime();
		MsgObj msg = new MsgObj(s,S_buffer,timeVector);

		//create a new thread that sends the message with the specified delay
		Hermes h = new Hermes(msg, destination, sleepTime);
		h.start();
		
		//add dest and timevector (bufferitem) to buffer
		BufferItem itm = new BufferItem(timeVector, destination);
		bufferInsert(itm);
	}
	
	/*
	 * Insert new item into the S_Buffer, remove any old elements
	 */
	private void bufferInsert(BufferItem itm)
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
	private synchronized void incTime()
	{
		int time = timeVector.get(nodeNr-1);
		time++;
		timeVector.set(nodeNr-1, time);
	}

	@Override
	public void run()
	{
		while(running)
		{
				
			try
			{
				if (sendBuffer.isEmpty()) Thread.sleep(2000);
				else
				{
					//send messages in the sendBuffer
					SendIntent s = sendBuffer.poll();
					send(s.getMessage(), s.getDestination(), s.getSleepTime());
				}
				
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
				debug(b.timeVector.toString());
			}
		}
	}
	
	private static final long serialVersionUID = -4676446028805100856L;
}
