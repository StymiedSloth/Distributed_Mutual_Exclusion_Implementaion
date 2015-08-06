package com.utd.aos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.aos.threads.ExecuteThread;
import com.aos.threads.TerminateThread;
import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class TestBed {

	
	private final static String COMPILE_COMMAND = "javac -d MTQ_SCTP/bin/ MTQ_SCTP/src/com/*/*/*.java";
//	private final static String COMPILE_COMMAND = "javac -d Maekawa_SCTP/bin/ Maekawa_SCTP/src/com/*/*/*.java";
//	private final static String EXECUTE_COMMAND = "java -cp .:MTQ_SCTP/bin/ com.aos.testbed.Test";
	private final static String EXECUTE_COMMAND = "java -cp .:Maekawa_SCTP/bin/ com.aos.testbed.Test";
	public final static int TOTAL_NUMBER_OF_NODES = 16;
	private final static String SERVER_ADDRESS_PREFIX = "net";
	private final static String SERVER_ADDRESS_SUFFIX = ".utdallas.edu";
	private final static String COMMAND_TO_UNIX = "exec";
	private static final String CONFIG_FILEPATH = "nodesettings.config";
	public static Boolean isTerminated = false;

	public static void main(String[] args) {
		ExecuteThread executeThread = new ExecuteThread();
		TerminateThread terminateThread = new TerminateThread();
		executeThread.start();
		terminateThread.start();
	}

	private static BufferedReader getBufferedReader(String filePath) {
		try {
			return new BufferedReader(new FileReader(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void startExecution(String username, String password) {
		try {
			Gson gson = new Gson();

			JSch jsch = new JSch();
			File file = new File(CONFIG_FILEPATH);
			if (!file.exists()) {
				System.out.print("Please configure the Node Settings File");
				return;
			}

			NodeSetting[] nodeSettings = gson.fromJson(
					getBufferedReader(CONFIG_FILEPATH), NodeSetting[].class);

			for (int i = 1; i <= TOTAL_NUMBER_OF_NODES; i++) {
				Session session = jsch.getSession(username,
						SERVER_ADDRESS_PREFIX + String.format("%02d", i)
								+ SERVER_ADDRESS_SUFFIX, 22);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword(password);
				session.setConfig("PreferredAuthentications", "password");
				session.connect();

				Channel channel = session.openChannel(COMMAND_TO_UNIX);
				// Java arguments myNodeId, totalNumberofNodes,requestTimeStamp,
				// token

				((ChannelExec) channel).setCommand(EXECUTE_COMMAND + " "
						+ nodeSettings[i - 1].getId() + " "
						+ TOTAL_NUMBER_OF_NODES + " "
						+ nodeSettings[i - 1].getTimestamp() + " "
						+ nodeSettings[i - 1].getToken());

				channel.setInputStream(null);

				((ChannelExec) channel).setErrStream(System.err);

				InputStream in = channel.getInputStream();

				channel.connect();

				// byte[] tmp=new byte[1024];
				// while(true){
				// while(in.available()>0){
				// int response=in.read(tmp, 0, 1024);
				// if(response<0)break;
				// System.out.print(new String(tmp, 0, i));
				// }
				// if(channel.isClosed()){
				// if(in.available()>0) continue;
				// System.out.println("exit-status: "+channel.getExitStatus());
				// break;
				// }
				// try{Thread.sleep(1000);}catch(Exception ee){}
				// }
				channel.disconnect();
				session.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopExecution() {
		try {
			JSch jsch = new JSch();
			String[] cred = LoginView.getCredentials();
			for (int i = 1; i <= TOTAL_NUMBER_OF_NODES; i++) {
				Session session = jsch.getSession(cred[0],
						SERVER_ADDRESS_PREFIX + String.format("%02d", i)
								+ SERVER_ADDRESS_SUFFIX, 22);

				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword(cred[1]);
				session.setConfig("PreferredAuthentications", "password");
				session.connect();
				Channel channel = session.openChannel(COMMAND_TO_UNIX);

				((ChannelExec) channel).setCommand("killall -9 java\n");
				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);

				InputStream in = channel.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));

				channel.connect();

				String line;
				while ((line = br.readLine()) != null || !channel.isClosed()) {
					if (line != null) {
						System.out.println(line + '\n');
					}
				}

				in.close();
				br.close();
				channel.disconnect();
				session.disconnect();
			}
			isTerminated = true;
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
	}

}
