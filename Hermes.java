package probeermi;

public class Hermes extends Thread 
{
	boolean debug = false;
	
	MsgObj msg; //the message object
	int dest; //the destination for this message
	int sleepTime; //the delay for this message
	int counter; //testing purposes
	
	public Hermes(MsgObj msg, int dest, int sleepTime)
	{
		this.msg = msg;
		this.dest = dest;
		this.sleepTime = sleepTime;
		counter = 0;
	}
	
	public void run()
	{
		try 
		{
			sleep(sleepTime);
			RMI_interface destObject = (RMI_interface) java.rmi.Naming.lookup("node"+dest);
			debug("Verstuurt " + msg.getMessage());
			debug(""+counter);
			destObject.receive_msg(msg, sleepTime);
			counter++;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	private void debug(String string) 
	{
		if (debug) System.out.println("Hermes: " + string);
	}
}
