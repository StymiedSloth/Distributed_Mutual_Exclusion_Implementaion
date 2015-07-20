package com.aos.testbed;

import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;
import com.aos.common.QueueObject;
import com.aos.server.TestServer;

public class Test2 
{
	public static void main(String args[])
	{		
		PriorityBlockingQueue<QueueObject> sharedQueue = new PriorityBlockingQueue<QueueObject>();
		
		int[] node2Quorum = {1,2,4};
		
		System.out.println("Initiating Sequence");	
		TestServer node2Server = new TestServer(2,sharedQueue,node2Quorum,false);
		TestClient node2Client = new TestClient(2,sharedQueue,node2Quorum,false);
		
		node2Server.start();
				try
		{
			Thread.sleep(50*1000);
		}
		catch(Exception ex)
		{
			System.out.println(ex);
		}
		
		node2Client.start();
	}
}
