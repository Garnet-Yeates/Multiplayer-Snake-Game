package edu.wit.yeatesg.multiplayersnakegame.phase2play;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeData;
import edu.wit.yeatesg.multiplayersnakegame.datatypes.other.SnakeList;
import edu.wit.yeatesg.multiplayersnakegame.phase1connect.ConnectGUI;

public class GameGUI extends JFrame
{	
	private static final long serialVersionUID = -1155890718213904522L;
	private Client contentPane;	
	

	public GameGUI(Client contentPane)
	{
		this.contentPane = contentPane;
		initWindow();
		contentPane.setFrame(this);
		ConnectGUI.setLookAndFeel();
	}
	
	private void initWindow()
	{
		setBounds(100, 100, Client.WIDTH + 500, Client.HEIGHT + 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(contentPane);
	}
}
