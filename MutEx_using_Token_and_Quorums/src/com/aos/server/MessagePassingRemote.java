package com.aos.server;
import java.rmi.RemoteException;
import java.rmi.server.*;

import com.aos.common.MessagePassing;

public class MessagePassingRemote extends UnicastRemoteObject implements MessagePassing
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	MessagePassingRemote() throws RemoteException 
	{
		super();
	}

	@Override
	public void MessagePass(int sender, String message) throws RemoteException
	{
		System.out.println("Message got from:" + sender);
		System.out.println("Message is " + message);
	}
}
