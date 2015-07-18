package com.aos.testbed;

import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;
import com.aos.common.QueueObject;
import com.aos.server.TestServer;

public class Test4 
{
	public static void main(String args[])
	{		
		PriorityBlockingQueue<QueueObject> sharedQueue = new PriorityBlockingQueue<QueueObject>();
		
		int[] node4Quorum = {2,3,4};
		
		System.out.println("Initiating Sequence");
		TestServer node4Server = new TestServer(4,sharedQueue,node4Quorum,false);
		TestClient node4Client = new TestClient(4,sharedQueue,node4Quorum,false);
		
		node4Server.start();
		try
		{
			Thread.sleep(50*1000);
		}
		catch(Exception ex)
		{
			System.out.println(ex);
		}
		
		node4Client.start();
	}
}
