package com.aos.common;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;

public class Functions
{
	private PriorityBlockingQueue<QueueObject> queue;
	private PriorityBlockingQueue<HandlerQueueObject> handlerQueue;
	private int myNodeID;
	private int[] quorum;
	private Boolean token;
	private Boolean criticalSection;
	private int timestamp;
	
	Functions(int myNodeID,PriorityBlockingQueue<QueueObject> queue,PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum, Boolean token) throws RemoteException 
	{
		this.myNodeID = myNodeID;
		this.queue = queue;
		this.handlerQueue = handlerQueue;
		this.quorum  = quorum;
		this.token = token;
		this.criticalSection = false;
	}

	public void sendRequest(int receivedTimestamp, int sender)
	{
		this.timestamp = timestamp + 1;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		QueueObject obj = queue.peek();
		
		if(obj.getSender() == myNodeID && token == true)
		{
			criticalSection = true;
			token = true;
			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			for(QueueObject q : queue)
			{
				if(q.getTimestamp()==this.timestamp && q.getSender()==myNodeID)
					queue.remove(q);
			}
			criticalSection = false;

			return;

		}
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				handlerQueue.add(new HandlerQueueObject("server", "receiverequest", timestamp, sender, QuorumMember));
			}
		}		
	}
	
	public void receiveRequest(int timestamp, int sender)			
	{
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
		
		if(token && !criticalSection)
		{			
			QueueObject queueObject = queue.peek();			 
			int TokenRequestor = queueObject.getSender();		
			token = false;
			handlerQueue.add(new HandlerQueueObject("server", "receivetoken", timestamp, sender, TokenRequestor));
		}
		else if(!token)
		{
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID && QuorumMember != sender)
				{
						handlerQueue.add(new HandlerQueueObject("server", "asktoken", timestamp, sender, 0));
				}
			}
		}
	}

	public void askToken(int timestamp, int tokenRequestor)
	{
		if(token && !criticalSection)
		{	
			token = false;
			handlerQueue.add(new HandlerQueueObject("server", "receivetoken", timestamp, myNodeID , tokenRequestor));
		}		
	}


	public void receiveToken(int sender)
	{
		if(!queue.isEmpty())
		{
			int TokenRequestor = queue.peek().getSender() ;
			 
			if(TokenRequestor == myNodeID)
			{
				criticalSection = true;
				token = true;
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				releaseCriticalSection();
				
				return;
			}
			
			token = false;
			handlerQueue.add(new HandlerQueueObject("server", "receivetoken", timestamp, sender , TokenRequestor));
				
		}
	}


	public void receiveReleaseMessage(int timestampReceived, int sender)
	{
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestampReceived && q.getSender()==sender)
				queue.remove(q);
		}

		this.timestamp = Math.max(this.timestamp , timestampReceived);
		TestClient.setTimestamp(this.timestamp);
		
		if(queue.size() > 0)
		{
			QueueObject nextRequestorInQueue = queue.peek();						
			handlerQueue.add(new HandlerQueueObject("server", "asktoken", timestamp, sender , 0));				

		}
	}
	

	public void releaseCriticalSection()
	{
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestamp && q.getSender()==myNodeID)
				queue.remove(q);
		}
		
		criticalSection = false;		
		
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				handlerQueue.add(new HandlerQueueObject("server", "receiveReleaseMessage", timestamp, myNodeID , QuorumMember));
			}
		}

	}
}
