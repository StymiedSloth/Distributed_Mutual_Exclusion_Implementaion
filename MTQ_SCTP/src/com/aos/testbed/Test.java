package com.aos.testbed;

import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.aos.client.TestClient;
import com.aos.common.HandlerQueueObject;
import com.aos.common.QueueObject;
import com.aos.handler.Handler;
import com.aos.server.TestServer;

public class Test 
{
	private static int myNodeId;
	private static int totalNumberOfNodes;
	private static int requestTime;
	private static Boolean Token;
	static Logger logger = Logger.getLogger("MyTestLog"); 
	static FileHandler fh;  
	public static void main(String args[])
	{		
		PriorityBlockingQueue<QueueObject> sharedQueue = new PriorityBlockingQueue<QueueObject>();
		PriorityBlockingQueue<HandlerQueueObject> handlerQueue = new PriorityBlockingQueue<HandlerQueueObject>();
		
		myNodeId = Integer.parseInt(args[0]);
		totalNumberOfNodes = Integer.parseInt(args[1]);
		requestTime = Integer.parseInt(args[2]);
		Token = Boolean.parseBoolean(args[3]);
		

		try {
			fh = new FileHandler("MyTestLogFile"+ myNodeId +".log");
			} catch (SecurityException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
	        logger.setUseParentHandlers(false);
		
		
		int[] myQuorum = findQuorum(myNodeId, totalNumberOfNodes);
		String temp = "";
		try {
			System.out.println("Initiating Sequence with my quorum");
			for (int i = 0; i < myQuorum.length; i++) {
				temp+= myQuorum[i] + " ";
			}
			System.out.println(temp);
			TestServer node1Server = new TestServer(myNodeId,sharedQueue,handlerQueue,myQuorum,Token);
			TestClient node1Client = new TestClient(myNodeId,sharedQueue,handlerQueue,myQuorum,Token,requestTime);
			Handler handler = new Handler(myNodeId, sharedQueue, handlerQueue, myQuorum, Token);
			
			handler.start();
			node1Server.start();

//			Thread.sleep(5*1000);
			
			node1Client.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int[] findQuorum(int myNodeId,int totalNumberOfNodes)
	{
		int matrixRowSize = (int) Math.ceil(Math.sqrt(totalNumberOfNodes));
		
		int[][] nodeGrid = new int[matrixRowSize][matrixRowSize];
		
		int row = 0,column = 0, nodeId = 1;
		int myRow = 0, myColumn = 0;
		for(int[] gridColumn : nodeGrid)
		{
			for(int gridItem : gridColumn)
			{
				nodeGrid[row][column] = nodeId;
				if(nodeId == myNodeId)
					{myRow = row;myColumn=column;}
				column++;nodeId++;
				
			}
			row++;
			column = 0;
		}
		
		int[] quorum = new int[(2 * matrixRowSize) - 1];
		int j = 0;
		for(int i=0;i<2*matrixRowSize - 1;i = i+2)
		{
			quorum[i] = nodeGrid[j][myColumn];			
			if(nodeGrid[myRow][j] == myNodeId)
				i--;
			else
				quorum[i + 1] = nodeGrid[myRow][j];
			j++;
		}

		
		
		return quorum;
	}
}
