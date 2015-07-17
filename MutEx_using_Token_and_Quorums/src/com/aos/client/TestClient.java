package com.aos.client;
import java.rmi.Naming;

import com.aos.common.MessagePassing;


public class TestClient implements Runnable
{
	Thread t;
	@Override
	public void run() 
	{
		while(true)
		{
			try
			{
				MessagePassing stub = (MessagePassing) Naming.lookup("rmi://localhost:5000/mutex");
				System.out.println("I am client passing the parameter");
				stub.MessagePass(1, "This is test");
				Thread.sleep(2000);
			}
			catch(Exception ex)
			{
				System.out.println(ex);
			}
		}
	}
	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
}
