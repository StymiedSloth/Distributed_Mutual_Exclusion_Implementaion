package com.aos.server;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
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
	private int myNodeId;
	private int[] quorum;
	private Boolean token;
	private Boolean critical_section;
	
	MessagePassingRemote(PriorityBlockingQueue<QueueObject> queue, int[] quorum) throws RemoteException 
	{
		super();
		//TODO intialize myNodeId
		this.queue = queue;
		this.quorum  = quorum;
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
		
		if(token && !critical_section)
		{
			 QueueObject queueObject = queue.poll();
			 
			 int TokenRequestor = queueObject.getSender();
			 
			 //TODO Send Token to Requestor. To do this you have implement the receive token method
		}
		else if(!token)
		{
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeId)
				{
					//TODO implement request token function
					MessagePassing stub;
					try {
						stub = (MessagePassing) Naming.lookup("rmi://localhost:"+ (5000 + QuorumMember) +"/mutex");

						stub.receiveRequest(1, 1);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NotBoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
}
