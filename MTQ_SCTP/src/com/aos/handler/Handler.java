package com.aos.handler;

import java.util.concurrent.PriorityBlockingQueue;
import java.io.*;
import java.net.*;
import com.sun.nio.sctp.*;
import java.nio.*;
import com.aos.common.HandlerQueueObject;
import com.aos.common.QueueObject;

public class Handler implements Runnable{

	Thread t;
	private PriorityBlockingQueue<HandlerQueueObject> handlerQueue;	
	
	public Handler(PriorityBlockingQueue<HandlerQueueObject> handlerQueue) {
		this.handlerQueue = handlerQueue;
	}
	
	@Override
	public void run() 
	{
		while (true) {		
			if(handlerQueue.size() > 0)
			{
				HandlerQueueObject handlerQueueObject = handlerQueue.poll();
				String params = handlerQueueObject.getRequestFrom() + "|";
				params += handlerQueueObject.getMethod() + "|";
				params += handlerQueueObject.getSender() + "|";
				params += handlerQueueObject.getTimestamp();
				sendMessage(params);
			}
		}

	}
	
	public static final int MESSAGE_SIZE = 100;
	public void sendMessage(String params)
	{
		//Buffer to hold messages in byte format
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);		
		try
		{
			//Create a socket address for  server at net01 at port 5000
			SocketAddress socketAddress = new InetSocketAddress("net01.utdallas.edu",6000);
			//Open a channel. NOT SERVER CHANNEL
			SctpChannel sctpChannel = SctpChannel.open();
			//Bind the channel's socket to a local port. Again this is not a server bind
			sctpChannel.bind(new InetSocketAddress(5000));
			//Connect the channel's socket to  the remote server
			sctpChannel.connect(socketAddress);
			//Before sending messages add additional information about the message
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			//convert the string message into bytes and put it in the byte buffer
			byteBuffer.put(params.getBytes());
			//Reset a pointer to point to the start of buffer 
			byteBuffer.flip();
			//Send a message in the channel (byte format)
			sctpChannel.send(byteBuffer,messageInfo);
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
