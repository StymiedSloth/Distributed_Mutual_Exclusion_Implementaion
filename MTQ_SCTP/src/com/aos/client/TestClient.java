package com.aos.client;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.*;
import java.net.*;

import com.sun.nio.sctp.*;

import java.nio.*;

import com.aos.common.HandlerQueueObject;
import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;


public class TestClient implements Runnable
{
	Thread t;
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private PriorityBlockingQueue<HandlerQueueObject> handlerQueue;
	private int[] quorum;
	private Boolean token;
	private int requestTime;
	private static int timestamp = 0;
	private PriorityBlockingQueue<Integer> requestTimestampQueue; 
	
	Logger logger = Logger.getLogger("MyClientLog"); 
	FileHandler fh;  
	public TestClient(int myNodeID,PriorityBlockingQueue<QueueObject> queue,PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum,Boolean token,int requestTime) {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.handlerQueue = handlerQueue;
		this.quorum = quorum;
		this.token = token;
		this.requestTime = requestTime;
		requestTimestampQueue = new PriorityBlockingQueue<Integer>();
		requestTimestampQueue.add(requestTime);
	}
	
	public static int getTimeStamp(){
		return timestamp;
	}
	
	public static void setTimestamp(int latestTimestamp){
		timestamp = latestTimestamp;
	}
	
	@Override
	public void run() 
	{
		 try {
			fh = new FileHandler("MyClientLogFile"+ myNodeID +".log");
			} catch (SecurityException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
	        logger.setUseParentHandlers(false);
	        
		while (true) {		
			if(!requestTimestampQueue.isEmpty() && requestTimestampQueue.peek() <= getTimeStamp())
			{
				requestTimestampQueue.poll();
				HandlerQueueObject handlerQueueObject = new HandlerQueueObject("execute","sendrequest", requestTime-1, myNodeID, myNodeID);
				logger.info("Queued request : (execute,sendrequest," + (requestTime-1) +"," + myNodeID + "," + myNodeID + ")");
				handlerQueue.add(handlerQueueObject);
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setTimestamp(getTimeStamp() + 1);
		}

	}

	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
}
