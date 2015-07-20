package com.aos.server;


import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;

public class TestServer implements Runnable
{
	private Thread t;
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private int[] quorum;
	private Boolean token;
	
	public TestServer(int myNodeID,PriorityBlockingQueue<QueueObject> queue, int[] quorum, Boolean token) {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.quorum = quorum;
		this.token = token;
	}
	
	@Override
	public void run()
	{
		try
		{
			LocateRegistry.createRegistry(5001);
			MessagePassing stub = new MessagePassingRemote(myNodeID,queue,quorum,token);
			Naming.rebind("rmi://net"+String.format("%02d",myNodeID)+".utdallas.edu:5001/mutex",stub);
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
