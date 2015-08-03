package com.aos.threads;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.utd.aos.LoginView;
import com.utd.aos.TestBed;

public class TerminateThread implements Runnable {
	private Thread t;
	
	public TerminateThread()
	{

	}
	
	@Override
	public void run()
	{
		Scanner s = new Scanner(System.in);
		if(s.hasNext())
		{
			if(s.next().equals("x"))
				TestBed.stopExecution();
		}
		s.close();
		
		try
		{
			String files = "";
			for(int i=1;i<= TestBed.TOTAL_NUMBER_OF_NODES;i++)
		      {
				files +=  i +".txt "; 
		      }
			JSch jsch=new JSch();    
			String[] cred = LoginView.getCredentials();
			
			Session session = jsch.getSession(cred[0], "net01.utdallas.edu", 22);
			
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(cred[1]);
			session.setConfig("PreferredAuthentications","password");
			session.connect();
			
			Channel channel=session.openChannel("exec");
			
			((ChannelExec)channel).setCommand("head -c -1 -q "  + files +"> combined.txt");
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			InputStream ins = channel.getInputStream();

			channel.connect();
			
			
			ChannelSftp sftpchannel = (ChannelSftp) session.openChannel("sftp");
			sftpchannel.connect();
			sftpchannel.cd("/home/004/v/vd/vdr140330/");

			InputStream in = sftpchannel.get("a.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
            while ((line = br.readLine()) != null || !channel.isClosed()) {
                if (line != null) {
                    System.out.println(line);
                }
            }
			in.close();
            br.close();
            channel.disconnect();
            sftpchannel.disconnect();
			session.disconnect();
		}
		catch(Exception ex)
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
