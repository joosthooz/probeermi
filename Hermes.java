package probeermi;

public class Hermes extends Thread 
{
	boolean debug = true;
	
	MsgObj msg; //the message object
	int dest; //the destination for this message
	int sleepTime; //the delay for this message
	
	public Hermes(MsgObj msg, int dest, int sleepTime)
	{
		this.msg = msg;
		this.dest = dest;
		this.sleepTime = sleepTime;
	}
	
	public void run()
	{
		try 
		{
			sleep(sleepTime);
			RMI_interface destObject = (RMI_interface) java.rmi.Naming.lookup("node"+dest);
			debug("Verstuurt " + msg.getMessage());
			
			destObject.receive_msg(msg, sleepTime);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}

	private void debug(String string) 
	{
		if (debug) System.out.println(string);
	}
}
