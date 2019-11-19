package edu.wit.yeatesg.mps.phase3.play;


import javax.swing.JFrame;

import edu.wit.yeatesg.mps.phase1.connect.ConnectGUI;

public class SnakeGameGUI extends JFrame
{	
	private static final long serialVersionUID = -1155890718213904522L;
	private SnakeClient contentPane;	
	
	public SnakeGameGUI(SnakeClient contentPane)
	{
		this.contentPane = contentPane;
		initWindow();
		contentPane.setFrame(this);
		ConnectGUI.setLookAndFeel();
	}
	
	private void initWindow()
	{
		setBounds(100, 100, SnakeClient.WIDTH + 6, SnakeClient.HEIGHT + 29);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(contentPane);
		setResizable(false);
	}
}
