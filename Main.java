package probeermi;

import java.rmi.RemoteException;



public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		RemMethClass a1;
		RemMethClass a2;
		RemMethClass a3;
		
		try
		{
			a1 = new RemMethClass(1, 3);
			a2 = new RemMethClass(2, 3);
			a3 = new RemMethClass(3, 3);
			
			new Thread(a1).start();
			new Thread(a2).start();
			new Thread(a3).start();
			
			Thread.sleep(2000);
			
			/**
			 * Example from slides
			 */
			for(int i = 0;i<100; i++){
				a1.send("m1",2, 0);
				a1.send("m2",3,0);
				a3.send("m3",2,0);
			}

			
			Thread.sleep(2000);
			
			a1.printHistory();
			a2.printHistory();
			a3.printHistory();
			
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
