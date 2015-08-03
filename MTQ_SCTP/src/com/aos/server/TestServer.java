package com.aos.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.PriorityBlockingQueue;

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
					//System.out.println(messageInfo);
					message = byteToString(byteBuffer);
					byteBuffer.flip();
					String[] splits = message.split(",");
					System.out.println("Received and queued request : (execute,"+ splits[1] + "," + splits[2] +"," + splits[3]+ "," + splits[4] + ")");
					//System.out.println(message);
					HandlerQueueObject handlerQueueObject = new HandlerQueueObject("execute", splits[1].trim() , Integer.parseInt(splits[2].trim()), Integer.parseInt((splits[3].trim()).substring(0,1)),
							Integer.parseInt((splits[4].trim()).substring(0,1)));
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
			System.out.println(ex);
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
