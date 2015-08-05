package com.aos.handler;

import java.util.concurrent.PriorityBlockingQueue;
import java.io.*;
import java.net.*;

import com.sun.nio.sctp.*;

import java.nio.*;

import com.aos.common.Functions;
import com.aos.common.HandlerQueueObject;
import com.aos.common.QueueObject;

public class Handler implements Runnable{

	Thread t;
		
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private PriorityBlockingQueue<HandlerQueueObject> handlerQueue;
	private int[] quorum;
	private static int timestamp = 0;
	private Functions functions;
	private Boolean[] hasAddressAlreadyBeenCreated = new Boolean[20];
	
	public Handler(int myNodeID,PriorityBlockingQueue<QueueObject> queue,PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum) throws IOException {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.handlerQueue = handlerQueue;
		this.quorum = quorum;
		
		functions = new Functions(myNodeID,queue,handlerQueue,quorum);
	}
	
	@Override
	public void run() 
	{
		while (true) {		
			if(handlerQueue.size() > 0)
			{
				try
				{
					
					HandlerQueueObject handlerQueueObject = handlerQueue.poll();
					String whatToDo = handlerQueueObject.getRequestFrom();
					String method = handlerQueueObject.getMethod();
					int timestamp = handlerQueueObject.getTimestamp(); 
					int sender = handlerQueueObject.getSender();
					int receiver = handlerQueueObject.getReceiver();
					System.out.println("Handler : (" + whatToDo + ","+method + "," + timestamp +"," + sender + "," + receiver + ")");
					if(whatToDo.equals("send"))
						sendMessage(whatToDo,method,timestamp,sender,receiver);
					else
						executeAppropriateFunction(method, timestamp, sender, receiver);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
					
			}
		}

	}
	
	private void executeAppropriateFunction(String method, int timestamp, int sender, int receiver) throws IOException {
		if(method.equals("sendrequest"))
			functions.sendRequest(timestamp, sender);
		else if(method.equals("receiverequest"))
			functions.receiveRequest(timestamp, sender);
		else if(method.equals("receiveLocked"))
			functions.receiveLocked(timestamp, sender);
		else if(method.equals("receiveFailed"))
			functions.receiveFailed(timestamp, sender);
		else if(method.equals("receiveInquire"))
			functions.receiveInquire(timestamp, sender);
		else if(method.equals("receiveRelinquish"))
			functions.receiveRelinquish(timestamp, sender);
		else if(method.equals("receiveReleaseMessage"))
			functions.receiveReleaseMessage(timestamp, sender);
	}

	public static final int MESSAGE_SIZE = 100;
	public void sendMessage(String whatToDO, String method, int timestamp, int sender,int receiver)
	{
		String params = whatToDO + "," + method + "," + timestamp + "," + sender +"," + receiver;
		params = params.length() + "," + params;
		//Buffer to hold messages in byte format
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);		
		try
		{
			if(hasAddressAlreadyBeenCreated[receiver] == null)
				hasAddressAlreadyBeenCreated[receiver] = false;
			SocketAddress socketAddress = new InetSocketAddress("net"+ String.format("%02d",receiver) +".utdallas.edu",(6600 + Integer.parseInt(String.format("%02d",receiver))));
			SctpChannel sctpChannel = SctpChannel.open();
			if(!hasAddressAlreadyBeenCreated[receiver])
			{
				sctpChannel.bind(new InetSocketAddress(6600 + Integer.parseInt(String.format("%02d",receiver))));
				hasAddressAlreadyBeenCreated[receiver] = true;
			}
			sctpChannel.connect(socketAddress);
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			byteBuffer.put(params.getBytes());
			byteBuffer.flip();
			sctpChannel.send(byteBuffer,messageInfo);
			byteBuffer.clear();
			sctpChannel.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}

	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
	
}
