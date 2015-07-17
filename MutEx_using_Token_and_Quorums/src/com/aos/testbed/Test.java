package com.aos.testbed;

import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;
import com.aos.common.QueueObject;
import com.aos.server.TestServer;

public class Test 
{
	public static void main(String args[])
	{		
		PriorityBlockingQueue<QueueObject> sharedQueue = new PriorityBlockingQueue<QueueObject>();
		
		int[] quorum = new int[7]; 
		
		System.out.println("Initiating Sequence");
		TestServer ts = new TestServer(sharedQueue,quorum);
		TestClient tc = new TestClient(sharedQueue, quorum);
		
		ts.start();
	
		tc.start();
	}
}
