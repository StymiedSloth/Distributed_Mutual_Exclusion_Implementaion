package com.aos.client;
import java.rmi.Naming;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;


public class TestClient implements Runnable
{
	Thread t;
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private int[] quorum;
	private Boolean token;
	
	public TestClient(int myNodeID,PriorityBlockingQueue<QueueObject> queue,int[] quorum,Boolean token) {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.quorum = quorum;
		this.token = token;
	}
	
	@Override
	public void run() 
	{
		int i = 0;
		
		System.out.println("Client Thread start " + myNodeID + " with " + quorum.length);
		
		while(i < quorum.length)
		{
			try
			{
				MessagePassing stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",quorum[i])+".utdallas.edu:5000/mutex");
				
				if(quorum[i] != myNodeID)
					stub.receiveRequest(1, myNodeID);
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
