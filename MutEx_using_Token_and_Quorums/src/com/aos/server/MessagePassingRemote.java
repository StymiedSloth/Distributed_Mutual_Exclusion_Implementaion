package com.aos.server;
import java.rmi.RemoteException;
import java.rmi.server.*;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;

public class MessagePassingRemote extends UnicastRemoteObject implements MessagePassing
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PriorityBlockingQueue<QueueObject> queue;
	
	
	MessagePassingRemote(PriorityBlockingQueue<QueueObject> queue) throws RemoteException 
	{
		super();
		this.queue = queue;
	}

	@Override
	public void MessagePass(int sender, String message) throws RemoteException
	{
		System.out.println("Message got from:" + sender);
		System.out.println("Message is " + message);
	}

	@Override
	public void receiveRequest(int timestamp, int sender)
			throws RemoteException {
		queue.add(new QueueObject(timestamp, sender));
		System.out.println("Object added to queue");
		
	}
}
