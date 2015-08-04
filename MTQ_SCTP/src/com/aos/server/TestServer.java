package com.aos.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.aos.common.HandlerQueueObject;
import com.aos.common.QueueObject;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class TestServer implements Runnable
{
	private Thread t;
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private PriorityBlockingQueue<HandlerQueueObject> handlerQueue;
	private int[] quorum;
	private Boolean token;
	
	public TestServer(int myNodeID,PriorityBlockingQueue<QueueObject> queue, PriorityBlockingQueue<HandlerQueueObject> handlerQueue,
			int[] quorum, Boolean token) {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.handlerQueue = handlerQueue;
		this.quorum = quorum;
		this.token = token;
	}
	
	@Override
	public void run()
	{
		try
		{
			ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
			String message;
			try
			{
				SctpServerChannel sctpServerChannel = SctpServerChannel.open();
				InetSocketAddress serverAddr = new InetSocketAddress(6500 + Integer.parseInt(String.format("%02d",myNodeID)));
				
				sctpServerChannel.bind(serverAddr);			
				while(true)
				{
					SctpChannel sctpChannel = sctpServerChannel.accept();
					
					MessageInfo messageInfo = sctpChannel.receive(byteBuffer,null,null);
					message = byteToString(byteBuffer);
					byteBuffer.flip();
					String[] splits = message.split(",");
					int messageLength = Integer.parseInt(splits[0]);
					message = message.substring(3,messageLength+3);
					splits = message.split(",");
					HandlerQueueObject handlerQueueObject = new HandlerQueueObject("execute", splits[1].trim() , Integer.parseInt(splits[2].trim()), Integer.parseInt((splits[3].trim())),
							Integer.parseInt((splits[4].trim())));
					handlerQueue.add(handlerQueueObject);
					
				}

			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
			
		}
		catch(Exception ex)
		{
			System.out.println("Error in server");
		}	
	}
	
	public static final int MESSAGE_SIZE = 100;

	public String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
	}
	
	public void start()
	{
		 t = new Thread(this);
		 t.start();
	}
}
