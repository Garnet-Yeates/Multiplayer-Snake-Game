package edu.wit.yeatesg.multiplayersnakegame.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class GameGUI extends JFrame
{
	private static final long serialVersionUID = 473354879981949828L;
	
	protected static final int SSL = 3;
	
	protected static final int NUM_HORIZONTAL_UNITS = 30;
	protected static final int NUM_HORIZONTAL_SPACES = NUM_HORIZONTAL_UNITS + 1;
	protected static final int NUM_VERTICAL_UNITS = 20;
	protected static final int NUM_VERTICAL_SPACES = NUM_VERTICAL_UNITS + 1;
	protected static final int UNIT_SIZE = 30; // Pixels
	protected static final int SPACE_SIZE = 3; 
	
	protected static final int WIDTH = NUM_HORIZONTAL_UNITS * UNIT_SIZE + NUM_HORIZONTAL_SPACES * SPACE_SIZE;
	protected static final int HEIGHT = NUM_VERTICAL_UNITS * UNIT_SIZE + NUM_VERTICAL_SPACES * SPACE_SIZE;
	
	private JPanel contentPane;	

	public GameGUI()
	{
		initWindow();
	}
	
	private void initWindow()
	{
		setBounds(100, 100, WIDTH + 5, HEIGHT + 5);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		setVisible(true);
	}
}
