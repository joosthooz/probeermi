package probeermi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the remote interface class that declares the methods
 * that are available at the remote system.
 * @author Joost
 *
 */
public interface RMI_interface extends Remote
{
	/*
	 * Use this method to send a message to the remote host
	 */
	void receive_msg(MsgObj msg, int sleepTime) throws RemoteException;
	
	/*
	 * method used for testing RMI
	 */
	public void print(String text) throws RemoteException;
	
	/*
	 * Adds a new message into the sendBuffer that will be sent by this thread the next time it runs.
	 * Arguments are the destination node, the message, and the time this message must be delayed.
	 */
	public void prepareToSend(int destination, String message, int sleepTime) throws RemoteException;
}
