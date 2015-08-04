package com.aos.common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
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
	private Boolean criticalSection;
	private int timestamp;
	private Boolean amILocked; 
	private ArrayList<QueueObject> lockedMembers;
	private ArrayList<QueueObject> inquiredMembers;
	private BufferedWriter writer;
	Logger logger = Logger.getLogger("MyFuncLog"); 
	FileHandler fh;
	
	public Functions(int myNodeID,PriorityBlockingQueue<QueueObject> queue,PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum) throws IOException
	{
		this.myNodeID = myNodeID;
		this.queue = queue;
		this.handlerQueue = handlerQueue;
		this.quorum  = quorum;
		this.criticalSection = false;
		fh = new FileHandler("MyFuncLogFile"+ myNodeID +".log");
		logger.addHandler(fh);
	    SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);
        logger.setUseParentHandlers(false);
        amILocked = false;
        lockedMembers = new ArrayList<QueueObject>();
	}

	public void sendRequest(int receivedTimestamp, int sender) throws IOException
	{
		this.timestamp = receivedTimestamp + 1;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		amILocked = true;
		
		lockedMembers.add(queueObject);
		
		String temp = "";
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				temp += QuorumMember + " , ";
				handlerQueue.add(new HandlerQueueObject("send", "receiverequest", timestamp, sender, QuorumMember));
			}
		}
		logger.info("SRequest: Sending request to my quorum members " + temp);
	}
	
	
	public void receiveRequest(int receivedTimestamp, int sender)			
	{
		logger.info("RRequest: From " + sender);
		if(!criticalSection)
		{
			if(amILocked)
			{
				QueueObject currentHighestRequest = queue.peek();
				if(receivedTimestamp < currentHighestRequest.getTimestamp() || 
						(receivedTimestamp == currentHighestRequest.getTimestamp() &&  sender < currentHighestRequest.getSender()))
				{
					handlerQueue.add(new HandlerQueueObject("send", "receiveInquire", timestamp, myNodeID, 
							currentHighestRequest.getSender() ));
				}
				else
				{
					handlerQueue.add(new HandlerQueueObject("send", "receiveFailed", timestamp, myNodeID, sender));
				}
				
			}
			else
			{
				handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, sender));
			}
		}
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
	}

	
	public void receiveLocked(int receievedTimestamp,int sender) throws IOException
	{
		QueueObject queueObject = new QueueObject(receievedTimestamp, sender);
		
		lockedMembers.add(queueObject);
		
		if(lockedMembers.size() == quorum.length)
		{
			criticalSection = true;
			logger.info("RLocked: I have locked all other nodes, I enter my CS");
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
			logger.info("RLocked: I am done with my CS, sending release to all");
			writer.write("Node " + myNodeID + " Exits;\n");
			writer.close();
			
			amILocked = false;
			lockedMembers.clear();
			
			for(QueueObject q : queue)
			{
				if(q.getTimestamp()== timestamp && q.getSender()== myNodeID)
					queue.remove(q);
			}
			
			if(!queue.isEmpty())
			{
				queueObject = queue.peek();
				handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, 
							queueObject.getSender()));
			}
			
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID)
				{
					handlerQueue.add(new HandlerQueueObject("send", "receiveReleaseMessage", timestamp, myNodeID, QuorumMember));
				}
			}
			
		}
	}
	
	

	public void receiveFailed(int receivedTimestamp,int sender)
	{
		for(QueueObject queueObject : inquiredMembers)
			handlerQueue.add(new HandlerQueueObject("send", "receiveRelinquish", timestamp, myNodeID, queueObject.getSender()));
	}
	
	public void receiveInquire(int timestamp,int sender)
	{
		QueueObject queueObject = new QueueObject(timestamp, sender);
		inquiredMembers.add(queueObject);
	}

	
	public void receiveRelinquish(int receivedTimestamp,int sender)
	{
		QueueObject queueObject = queue.peek();
		handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp ,
				myNodeID, queueObject.getSender()));
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
			logger.info("RRleaseMessage: My queue " + temp + " is not empty, so I lock to next item");						
			QueueObject queueObject = queue.peek();
			handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, 
						queueObject.getSender()));				

		}
	}

}
