package com.utd.aos;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginView {

	private static JTextField userText;
	private static JPasswordField passwordText;
	public static JFrame frame;
	
	
	public static String[] getCredentials()
	{
		return new String[]{userText.getText() , String.valueOf(passwordText.getPassword()) };
	}
	
	public static void getView() {
		frame = new JFrame("Enter your credentials");
		frame.setSize(300, 150);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		frame.add(panel);
		placeComponents(panel);

		frame.setVisible(true);
		
	}

	private static void placeComponents(JPanel panel) {

		panel.setLayout(null);

		JLabel userLabel = new JLabel("UTD Net Id");
		userLabel.setBounds(10, 10, 80, 25);
		panel.add(userLabel);

		userText = new JTextField(20);
		userText.setBounds(100, 10, 160, 25);
		panel.add(userText);

		JLabel passwordLabel = new JLabel("Password");
		passwordLabel.setBounds(10, 40, 80, 25);
		panel.add(passwordLabel);

		passwordText = new JPasswordField(20);
		passwordText.setBounds(100, 40, 160, 25);
		panel.add(passwordText);

		JButton loginButton = new JButton("login");
		loginButton.setBounds(10, 80, 80, 25);
		panel.add(loginButton);
		
		ActionListener myButtonListener = new MyButtonListener();
		loginButton.addActionListener(myButtonListener);
		
	}

}

class MyButtonListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton source = (JButton) e.getSource();
		String[] credentials = LoginView.getCredentials();
		LoginView.frame.setVisible(false);
		LoginView.frame.dispose();
		
		TestBed.startExecution(credentials[0] , credentials[1]);
	}
}
