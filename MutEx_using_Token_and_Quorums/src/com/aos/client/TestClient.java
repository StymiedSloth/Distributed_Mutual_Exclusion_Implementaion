package com.aos.client;
import java.rmi.Naming;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;


public class TestClient implements Runnable
{
	Thread t;
	private PriorityBlockingQueue<QueueObject> queue;
	private int[] quorum;
	
	public TestClient(PriorityBlockingQueue<QueueObject> queue,int[] quorum) {
		this.queue = queue;		
		this.quorum = quorum;
	}
	
	@Override
	public void run() 
	{
		int i = 1;
		while(true)
		{
			try
			{
				MessagePassing stub = (MessagePassing) Naming.lookup("rmi://localhost:5000/mutex");
				//System.out.println("I am client passing the parameter");
				//stub.MessagePass(1, "This is test");
				stub.receiveRequest(i, 1);
				i++;
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
