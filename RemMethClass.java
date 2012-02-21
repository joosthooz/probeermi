package probeermi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This class is a node in the distributed system. It is runnable and contains methods that may be invoked remotely.
 * @field timeVector - the local timeVector. It is updated by {@link incTime()} and {@link deliver(msg)}
 * @field sendBuffer - contains messages to be sent by this node. they can be inserted by {@link prepareToSend()}
 * @field S_buffer - contains BufferItems (see {@link BufferItem} that will be attached to messages to other nodes.
 * @field B_buffer - contains received messages that could not be delivered yet. They will be checked every time a message is delivered.
 * @field history - when a message is delivered, it is stored in this buffer.
 * @field received - for debugging purposes; it stores all messages in the same order of receiving them
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
	 * This static method can be called from other classes to create a new timeVector of the specified size
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
	
	/*
	 * Remote invokable method that prints a string to stdout
	 * (non-Javadoc)
	 * @see probeermi.RMI_interface#print(java.lang.String)
	 */
	@Override
	public synchronized void print(String text) throws RemoteException
	{
		System.out.println(name+" : "+text);
	}
	
	/*
	 * Prints the order in which messages were delivered to stdout
	 */
	public synchronized void printHistory(){
		System.out.println("History of "+name+":");
		for (MsgObj m : history){
			m.print();
		}
		System.out.println("\n");
	}
	/*
	 * Prints the order in which message arrived at this node to stdout.
	 */
	public synchronized void printReceivedOrder(){
		System.out.println("Received order of "+name+":");
		for (MsgObj m : received){
			m.print();
		}
		System.out.println("\n");
	}
	
	/**
	 * Inserts a new message into the sendBuffer and notifies the thread so it will become runnable.
	 * This is a remote method that can be invoked by a supervising thread that instructs all the nodes
	 * to send certain messages.
	 * @see probeermi.RMI_interface#prepareToSend(int, java.lang.String, int)
	 */
	@Override
	public void prepareToSend(int destination, String message, int sleepTime)
	{
		SendIntent intent = new SendIntent(destination, message, sleepTime);
		sendBuffer.add(intent);
		this.notify(); //interrupt the sleeping thread
	}
	/**
	 * 
	 * @see probeermi.RMI_interface#receive_msg(probeermi.MsgObj)
	 */
	@Override
	public synchronized void receive_msg(MsgObj msg) throws RemoteException
	{	
		received.add(msg);
		if (debug) msg.print();
		
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
	
	/**
	 * Deliver message; for now, store in the LinkedList.
	 * We can check afterwards whether the list is ordered.
	 * 
	 * updates the timeVector with info from the delivered msg
	 * Merges the buffer from the msg with the local S_buffer
	 * @param msg - the message to be delivered 
	 */
	private void deliver(MsgObj msg)
	{
		//deliver the message
		history.add(msg);

		//merge S_buffer
		insertMax(msg);
		
		//increase local clock
		incTime();
		
		//update local timeVector
			for (int i = 0; i < Main.nrOfNodes; i++)
			{
				timeVector.set(i, (Math.max(timeVector.get(i), msg.getTimeVector().get(i))));
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
	
	/*
	 * Return a new BufferItem that contains the pairwise maximum values of parameters a and b
	 */
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
	
	/**
	 * Sends a message to another host, and updates the S_Buffer.
	 * Uses a separate thread for sending to enable the message reception
	 * by the other hosts to be delayed by an arbitrary amount of time.
	 * 
	 * This method is synchronized because it increases the local Time, and
	 * updates the S_Buffer. The TimeVector may not be changed in between these actions.
	 * @param s - the contents of the message
	 * @param destination - The destination node
	 * @param sleepTime - the minimum amount of time that the sending thread must be delayed (there is no upper bound
	 * because there is no way to tell when the thread will actually become running)
	 */
	private synchronized void send(String s, int destination, int sleepTime)
	{	
		incTime();
		MsgObj msg = new MsgObj(s,S_buffer,timeVector);

		//create a new thread that sends the message with the specified delay
		Hermes h = new Hermes(msg, destination, sleepTime);
		h.start();
		
		//add dest and timevector (bufferitem) to buffer (the constructor copies the Vector)
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
		timeVector.set(nodeNr-1, timeVector.get(nodeNr-1)+1);
	}

	/*
	 * The thread code itself; check if there are messages to be sent. if there arent any; sleep for 2 seconds.
	 * otherwise, send a message. 2 seconds is a long time but the send method notifies this thread so 
	 * it will become runnable when a message is inserted to the sendBuffer.
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
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
	
	/*
	 * Causes this thread to clean up and stop running.
	 */
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
	
	/*
	 * Prints a message to stdout
	 */
	private void debug(String msg)
	{
		if (debug) System.out.println(name+" : "+msg);
	}
	
	/*
	 * prints the contents of a concurrentlinkedqueue to stdout
	 */
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
