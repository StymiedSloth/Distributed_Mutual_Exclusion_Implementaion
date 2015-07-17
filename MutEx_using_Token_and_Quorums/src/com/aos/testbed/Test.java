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
		
		System.out.println("Initiating Sequence");
		TestServer ts = new TestServer(sharedQueue);
		TestClient tc = new TestClient(sharedQueue);
		
		ts.start();
	
		tc.start();
	}
}
