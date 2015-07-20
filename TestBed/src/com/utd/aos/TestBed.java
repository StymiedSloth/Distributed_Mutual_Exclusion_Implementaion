package com.utd.aos;

import java.io.InputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class TestBed {

	private final static String COMPILE_COMMAND = "javac -d MutEx_using_Token_and_Quorums/bin/ MutEx_using_Token_and_Quorums/src/com/*/*/*.java";
	private final static String EXECUTE_COMMAND= "java -cp .:MutEx_using_Token_and_Quorums/bin/ com.aos.testbed.Test";
	private final static int TOTAL_NUMBER_OF_NODES = 16;
	private final static String SERVER_ADDRESS_PREFIX = "net";
	private final static String SERVER_ADDRESS_SUFFIX = ".utdallas.edu";
	private final static String COMMAND_TO_UNIX = "exec";
	
	
	public static void main(String[] args) {
		LoginView.getView();
	}

	public static void startExecution(String username, String password)
	{
		try{
				
		      JSch jsch=new JSch();  
		      for(int i=1;i<= TOTAL_NUMBER_OF_NODES;i++)
		      {
			      Session session = jsch.getSession(username, SERVER_ADDRESS_PREFIX +  String.format("%02d",i) + SERVER_ADDRESS_SUFFIX, 22);
	              session.setConfig("StrictHostKeyChecking", "no");
	              session.setPassword(password);
	              session.setConfig("PreferredAuthentications","password");
	              session.connect();
	              
			      
			      Channel channel=session.openChannel(COMMAND_TO_UNIX);
			      //Java arguments myNodeId, totalNumberofNodes,requestTimeStamp, token
			      ((ChannelExec)channel).setCommand(COMPILE_COMMAND + " \n" + EXECUTE_COMMAND + " " + "1 " + TOTAL_NUMBER_OF_NODES + " " + 2 + " " + "true");
			      
			      channel.setInputStream(null);
			      
			      ((ChannelExec)channel).setErrStream(System.err);
			      
			      InputStream in=channel.getInputStream();
			 
			      channel.connect();
			 
			      byte[] tmp=new byte[1024];
			      while(true){
			        while(in.available()>0){
			          int response=in.read(tmp, 0, 1024);
			          if(response<0)break;
			          System.out.print(new String(tmp, 0, i));
			        }
			        if(channel.isClosed()){
			          if(in.available()>0) continue; 
			          System.out.println("exit-status: "+channel.getExitStatus());
			          break;
			        }
			        try{Thread.sleep(1000);}catch(Exception ee){}
			      }
			      channel.disconnect();
			      session.disconnect();
		      }
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
