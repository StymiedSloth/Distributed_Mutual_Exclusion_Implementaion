package com.aos.common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
	private BufferedWriter writer;
	Logger logger = Logger.getLogger("MyFuncLog"); 
	FileHandler fh;
	
	public Functions(int myNodeID,PriorityBlockingQueue<QueueObject> queue,PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum, Boolean token) throws IOException
	{
		this.myNodeID = myNodeID;
		this.queue = queue;
		this.handlerQueue = handlerQueue;
		this.quorum  = quorum;
		this.token = token;
		this.criticalSection = false;
		fh = new FileHandler("MyFuncLogFile"+ myNodeID +".log");
		logger.addHandler(fh);
	    SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);
        logger.setUseParentHandlers(false);
	}

	public void sendRequest(int receivedTimestamp, int sender) throws IOException
	{
		logger.info("Executing Send Request");
		this.timestamp = receivedTimestamp + 1;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		QueueObject obj = queue.peek();
		
		if(obj.getSender() == myNodeID && token == true)
		{
			logger.info("I have token so I exnter Critical Section");
			
			writer = new BufferedWriter( new FileWriter( myNodeID +".txt"));
			writer.write("\n" + new Timestamp(System.currentTimeMillis()) +
					"\nNode " + myNodeID + " Enters; ");
			
			criticalSection = true;
			token = true;
			try
			{
				Thread.sleep(500);
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
			logger.info("Exit my Critical Section");
			
			writer.write("Node " + myNodeID + " Exits;\n");
			writer.close();
			return;

		}
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				logger.info("Don't have token, sending request to my quorum member " + QuorumMember);
				handlerQueue.add(new HandlerQueueObject("send", "receiverequest", timestamp, sender, QuorumMember));
			}
		}		
	}
	
	public void receiveRequest(int timestamp, int sender)			
	{
		logger.info("RRequest: I have received a request from my quorum member " + sender);
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
		
		if(token && !criticalSection)
		{	
			QueueObject queueObject = queue.peek();			 
			int TokenRequestor = queueObject.getSender();
			logger.info("RRequest:I have token and am not in CS so sending token to " + TokenRequestor);
			token = false;
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID, TokenRequestor));
		}
		else if(!token)
		{
			logger.info("RRequest:I don't have token so I ask my Quorum members");
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID && QuorumMember != sender)
				{
						handlerQueue.add(new HandlerQueueObject("send", "asktoken", timestamp, myNodeID, QuorumMember));
				}
			}
		}
	}

	public void askToken(int timestamp, int tokenRequestor)
	{
		if(token && !criticalSection)
		{	
			logger.info("AskTOken: I have token and am not in CS so sending token to " +tokenRequestor);
			token = false;
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID , tokenRequestor));
		}		
	}


	public void receiveToken(int sender) throws IOException
	{
		if(!queue.isEmpty())
		{
			int TokenRequestor = queue.peek().getSender() ;
			 
			if(TokenRequestor == myNodeID)
			{
				logger.info("RToken: I am the requestor for token, I enter my CS");
				criticalSection = true;
				token = true;
				writer = new BufferedWriter( new FileWriter( myNodeID +".txt"));
				writer.write("\n" +new Timestamp(System.currentTimeMillis()) +
						"\nNode " + myNodeID + " Enters; ");
				try
				{
					Thread.sleep(500);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				logger.info("RToken: I am done with my CS, calling release critical section");
				writer.write("Node " + myNodeID + " Exits;\n");
				writer.close();
				releaseCriticalSection();
				
				return;
			}
			else if(sender == TokenRequestor)
			{
				queue.poll();
				TokenRequestor = queue.peek().getSender();
			}
			
			token = false;
			logger.info("RToken: I am not the first token requestor, I pass it on to the token Reqestor " + TokenRequestor);
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID , TokenRequestor));
				
		}
	}


	public void receiveReleaseMessage(int timestampReceived, int tokenHolder)
	{
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestampReceived && q.getSender()==tokenHolder)
				queue.remove(q);
		}

		logger.info("RRleaseMessage: I have release message from " + tokenHolder + " , I have maxxed my timestamp.");
		
		this.timestamp = Math.max(this.timestamp , timestampReceived);
		TestClient.setTimestamp(this.timestamp);
		
		if(queue.size() > 0)
		{
			logger.info("RRleaseMessage: My queue is not empty, so I ask " + tokenHolder + " to give me the token if it has it");
			QueueObject nextRequestorInQueue = queue.peek();						
			handlerQueue.add(new HandlerQueueObject("send", "asktoken", timestamp, myNodeID , tokenHolder));				

		}
	}
	

	public void releaseCriticalSection()
	{
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestamp && q.getSender()==myNodeID)
				queue.remove(q);
		}
		
		
		logger.info("RCriticalSection: I dequeued myself, sending release messsages to all");
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				logger.info("RCriticalSection: sending release to " + QuorumMember);
				handlerQueue.add(new HandlerQueueObject("send", "receiveReleaseMessage", timestamp, myNodeID , QuorumMember));
			}
		}
		criticalSection = false;

	}
}
