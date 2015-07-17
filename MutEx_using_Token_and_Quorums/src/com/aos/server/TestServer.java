package com.aos.server;


import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;

public class TestServer implements Runnable
{
	private Thread t;
	private PriorityBlockingQueue<QueueObject> queue;
	private int[] quorum;
	
	public TestServer(PriorityBlockingQueue<QueueObject> queue, int[] quorum) {
		this.queue = queue;		
		this.quorum = quorum;
	}
	
	@Override
	public void run()
	{
		try
		{
			LocateRegistry.createRegistry(5000);
			MessagePassing stub = new MessagePassingRemote(queue,quorum);
			Naming.rebind("rmi://localhost:5000/mutex",stub);
		}
		catch(Exception ex)
		{
			System.out.println("Error in server");
			System.out.println(ex);
		}	
	}
	
	public void start()
	{
		 t = new Thread(this);
		 t.start();
	}
}
