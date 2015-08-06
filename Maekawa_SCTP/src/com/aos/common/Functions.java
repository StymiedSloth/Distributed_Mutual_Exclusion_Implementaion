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
	public static int messagecount;
	
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
        inquiredMembers = new ArrayList<QueueObject>();
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
				messagecount++;
				handlerQueue.add(new HandlerQueueObject("send", "receiverequest", timestamp, sender, QuorumMember));
			}
		}
		logger.info("Message count:"+ myNodeID +":" +messagecount +"\n SRequest: Sending request to my quorum members " + temp);
	}
	
	
	public void receiveRequest(int receivedTimestamp, int sender)			
	{
		logger.info("RRequest: From " + sender);
		if(!criticalSection)
		{
			logger.info("RRequest: Am I locked " + amILocked);
			if(amILocked)
			{
				QueueObject currentHighestRequest = queue.peek();
				if(receivedTimestamp < currentHighestRequest.getTimestamp() || 
						(receivedTimestamp == currentHighestRequest.getTimestamp() &&  sender < currentHighestRequest.getSender()))
				{
					logger.info("RRequest: I got the higher priority request from " + sender);
					messagecount++;
					if( currentHighestRequest.getSender() == myNodeID )
					{
						messagecount++;
						logger.info("RRequest: I am the first one if my request queue, so I send locked to higher priority " + sender);
						lockedMembers.remove(currentHighestRequest);
						handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, sender));
					}
					else
					{
						logger.info("RRequest: I am locked by someone else, sending inquire to  " + currentHighestRequest.getSender());
						handlerQueue.add(new HandlerQueueObject("send", "receiveInquire", timestamp, myNodeID, 
							currentHighestRequest.getSender() ));
					}
				}
				else
				{
					messagecount++;
					logger.info("RRequest: Lower priority request got, sending failed to " + sender);
					handlerQueue.add(new HandlerQueueObject("send", "receiveFailed", timestamp, myNodeID, sender));
				}
				
			}
			else
			{
				messagecount++;
				amILocked = true;
				logger.info("RRequest: Sending locked to " + sender);
				handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, sender));
			} 
		}
		if(!queue.contains(new QueueObject(receivedTimestamp, sender)))
			queue.add(new QueueObject(receivedTimestamp, sender));
		
		logger.info("Message count:"+ myNodeID +":" +messagecount + "\n");
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
			criticalSection = false;
			if(!queue.isEmpty())
			{
				messagecount++;
				amILocked = true;
				queueObject = queue.peek();
				handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, 
							queueObject.getSender()));
			}
			
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID)
				{
					messagecount++;
					handlerQueue.add(new HandlerQueueObject("send", "receiveReleaseMessage", timestamp, myNodeID, QuorumMember));
				}
			}
			logger.info("Message count:"+ myNodeID +":" +messagecount + "\n");
		}
	}
	
	

	public void receiveFailed(int receivedTimestamp,int sender)
	{
		logger.info("RInquire: Received Failed from " + sender);
		for(QueueObject queueObject : inquiredMembers)
		{
			messagecount++;
			handlerQueue.add(new HandlerQueueObject("send", "receiveRelinquish", timestamp, myNodeID, queueObject.getSender()));
		}
		logger.info("Message count:"+ myNodeID +":" +messagecount + "\n");
	}
	
	public void receiveInquire(int timestamp,int sender)
	{
		logger.info("RInquire: Received inquire from " + sender);
		QueueObject queueObject = new QueueObject(timestamp, sender);
		inquiredMembers.add(queueObject);
	}

	
	public void receiveRelinquish(int receivedTimestamp,int sender)
	{
		logger.info("RInquire: Received relinquish from " + sender);
		
		messagecount++;
		QueueObject queueObject = queue.peek();
		handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp ,
				myNodeID, queueObject.getSender()));
		
		logger.info("Message count:"+ myNodeID +":" +messagecount + "\n");
	}


	public void receiveReleaseMessage(int timestampReceived, int sender) throws IOException
	{
		String temp = "";
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestampReceived && q.getSender()==sender)
				queue.remove(q);
			temp +="("+ q.getTimestamp() + " , " + q.getSender() + "),";
		}
		logger.info("RReleaseMessage: Queue is " + temp);
		this.timestamp = Math.max(this.timestamp , timestampReceived);
		TestClient.setTimestamp(this.timestamp);
		logger.info("RRleaseMessage: Release got from " + sender + " , I have maxxed my timestamp to " + this.timestamp);
				
		if(queue.size() > 0)
		{
			logger.info("RRleaseMessage: My queue " + temp + " is not empty, so I lock to next item");						
			QueueObject queueObject = queue.peek();
			if(queueObject.getSender() == myNodeID)
			{
				amILocked = true;
				receiveLocked(queueObject.getTimestamp(), queueObject.getSender());
			}
			else
			{
				messagecount++; 
				amILocked = true;
				handlerQueue.add(new HandlerQueueObject("send", "receiveLocked", timestamp, myNodeID, 
					queueObject.getSender()));				
			}

		}
		
		amILocked = false;
		
		logger.info("Message count:"+ myNodeID +":" +messagecount + "\n");
	}

}
