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
	public static int messagecount;
	
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
        messagecount = 0;
	}

	public void sendRequest(int receivedTimestamp, int sender) throws IOException
	{
		this.timestamp = receivedTimestamp + 1;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		QueueObject obj = queue.peek();
		
		if(obj.getSender() == myNodeID && token == true)
		{
			logger.info("SRequest: I have token so I enter Critical Section");
			
			writer = new BufferedWriter( new FileWriter( myNodeID +".txt"));
			//new Timestamp(System.currentTimeMillis())
			writer.write("\n" + this.timestamp +
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
			logger.info("SRequest: Exit my Critical Section");
			
			writer.write("Node " + myNodeID + " Exits;\n");
			writer.close();
			return;

		}
		
		String temp = "";
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				temp += QuorumMember + " , ";
				messagecount++;
				handlerQueue.add(new HandlerQueueObject("send", "receiverequest", timestamp, sender, QuorumMember));
			}
		}
		logger.info("Message count:"+ myNodeID +":" +messagecount + "\n SRequest: Don't have token, sending request to my quorum members " + temp);
	}
	
	public void receiveRequest(int timestamp, int sender)			
	{
		logger.info("RRequest: From " + sender);
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
		
		if(token && !criticalSection)
		{	
			QueueObject queueObject = queue.peek();			 
			int TokenRequestor = queueObject.getSender();
			logger.info("RRequest:I have token and am not in CS so sending token to " + TokenRequestor);
			token = false;
			messagecount++;
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID, TokenRequestor));
		}
		else if(!token)
		{
			String temp = "";
			
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID && QuorumMember != sender)
				{
						temp += QuorumMember + " , ";
						messagecount++;
						handlerQueue.add(new HandlerQueueObject("send", "asktoken", timestamp, myNodeID, QuorumMember));
				}
			}
			logger.info("Message count:"+ myNodeID +":" +messagecount + "\n RRequest:I don't have token so I ask my Quorum members "+ temp);
		}
	}

	public void askToken(int timestamp, int tokenRequestor)
	{
		if(token && !criticalSection)
		{	
			messagecount++;
			logger.info("Message count:"+ myNodeID +":" + messagecount + "\nAskTOken: I have token and am not in CS so sending token to " +tokenRequestor);
			token = false;
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID , tokenRequestor));
		}		
	}


	public void receiveToken(int sender) throws IOException
	{
		token = true;
		logger.info("RToken: From " + sender);
		if(!queue.isEmpty())
		{
			int TokenRequestor = queue.peek().getSender() ;
			 
			if(TokenRequestor == myNodeID)
			{
				criticalSection = true;
				logger.info("RToken: I am the requestor for token, I enter my CS");
				writer = new BufferedWriter( new FileWriter( myNodeID +".txt"));
				writer.write("\n" + this.timestamp +
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
			messagecount++;
			logger.info("Message count:"+ myNodeID +":" + messagecount + "\nRToken: I am not the first token requestor, I pass it on to the token Reqestor " + TokenRequestor);
			handlerQueue.add(new HandlerQueueObject("send", "receivetoken", timestamp, myNodeID , TokenRequestor));
				
		}
	}


	public void receiveReleaseMessage(int timestampReceived, int tokenHolder)
	{
		String temp = "";
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestampReceived && q.getSender()==tokenHolder)
				queue.remove(q);
			temp +="("+ q.getTimestamp() + " , " + q.getSender() + "),";
		}

		this.timestamp = Math.max(this.timestamp , timestampReceived);
		TestClient.setTimestamp(this.timestamp);
		logger.info("RRleaseMessage: Release got from " + tokenHolder + " , I have maxxed my timestamp to " + this.timestamp);
				
		if(queue.size() > 0)
		{
			messagecount++;
			logger.info("Message count:"+ myNodeID +":" + messagecount + "\n RRleaseMessage: My queue " + temp + " is not empty, so I ask " + tokenHolder + " to give me the token if it has it");						
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
		
		
		logger.info("Message count:"+ myNodeID +":" + (messagecount+ quorum.length - 1) + "\n RCriticalSection: I dequeued myself, sending release messsages to ");
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				messagecount++;
				logger.info(QuorumMember + " , ");
				handlerQueue.add(new HandlerQueueObject("send", "receiveReleaseMessage", timestamp, myNodeID , QuorumMember));
			}
		}
		criticalSection = false;

	}
}
