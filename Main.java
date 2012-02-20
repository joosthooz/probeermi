package probeermi;

import java.rmi.RemoteException;



public class Main
{

	public static int nrOfNodes; //public field to store the nr of threads in the system 
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try {
		java.rmi.registry.LocateRegistry.createRegistry(1099);
		} catch (RemoteException e) {
		e.printStackTrace();
		}
		
		nrOfNodes= 3;
		RemMethClass a1;
		RemMethClass a2;
		RemMethClass a3;
		
		try
		{
			a1 = new RemMethClass(1);
			a2 = new RemMethClass(2);
			a3 = new RemMethClass(3);
			
			new Thread(a1).start();
			new Thread(a2).start();
			new Thread(a3).start();
			
			Thread.sleep(2000);
			
			/**
			 * Example from slides
			 */
			for(int i = 0;i<3; i++)
			{
				a1.send("m"+i,2, 0);
				a1.send("n"+i,3,0);
				a3.send("o"+i,2,0);
			}
			
			/*
			 * Send msgs to random nodes and delay them for a random period of time between 0 and 2000 ms.
			 */
//			for(int i = 0;i<100; i++)
//			{
//				a1.send("m"+i,((int)Math.random()*3)+1, (int)Math.random()*2000);
//				a1.send("n"+i,((int)Math.random()*3)+1,(int)Math.random()*2000);
//				a3.send("o"+i,((int)Math.random()*3)+1,(int)Math.random()*2000);
//			}
			
			Thread.sleep(2000);
			
			a1.printHistory();
			a1.printReceivedOrder();
			a2.printHistory();
			a2.printReceivedOrder();
			a3.printHistory();
			a3.printReceivedOrder();
			
			a1.DIE();
			a2.DIE();
			a3.DIE();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			System.exit(-99);
		}
		System.out.println("\n"+"Done.");
		System.exit(0);
	}

}
