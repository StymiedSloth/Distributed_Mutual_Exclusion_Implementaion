package com.utd.aos;

import java.io.InputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class TestBed {

	public static void main(String[] args) {
		
	}

	public static void startExecution()
	{
		try{
		      JSch jsch=new JSch();  
		 
		      Session session=jsch.getSession("vdr140330", "//Enter IP here" , 22);
		      
		      Channel channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand("//command goes here");
		      
		      channel.setInputStream(null);
		      
		      ((ChannelExec)channel).setErrStream(System.err);
		      
		      InputStream in=channel.getInputStream();
		 
		      channel.connect();
		 
		      byte[] tmp=new byte[1024];
		      while(true){
		        while(in.available()>0){
		          int i=in.read(tmp, 0, 1024);
		          if(i<0)break;
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
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
