package com.aos.testbed;

import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;
import com.aos.common.QueueObject;
import com.aos.server.TestServer;

public class Test3 
{
	public static void main(String args[])
	{		
		PriorityBlockingQueue<QueueObject> sharedQueue = new PriorityBlockingQueue<QueueObject>();
		
		int[] node3Quorum = {1,3,4};
		
		System.out.println("Initiating Sequence");

		TestServer node3Server = new TestServer(3,sharedQueue,node3Quorum,false);
		TestClient node3Client = new TestClient(3,sharedQueue,node3Quorum,false);

		node3Server.start();
		try
		{
			Thread.sleep(50*1000);
		}
		catch(Exception ex)
		{
			System.out.println(ex);
		}
		
		node3Client.start();
	}
}
