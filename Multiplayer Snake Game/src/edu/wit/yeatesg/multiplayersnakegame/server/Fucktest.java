package edu.wit.yeatesg.multiplayersnakegame.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.Timer;

import org.omg.CORBA.StringHolder;

public class Fucktest
{
	public static void main(String[] args) {
		new Thread(() ->
		{
			new Server();
		}).start();;
		new Thread(() ->
		{
			new Client();
		}).start();;
	}
	
	static public class Client
	{
		public Client()
		{
			try {
				Socket s = new Socket("localhost", 8122);
				DataInputStream in = new DataInputStream(s.getInputStream());
				new Thread(() ->
				{
					while (true)
					{
						try {
							System.out.println(in.readUTF());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	static public class Server
	{
		public Server()
		{
			try
			{
				ServerSocket ss = new ServerSocket(8122);
				Socket s = ss.accept();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				final StringHolder hold = new StringHolder("<Nigget>");
				Timer t = new Timer(30, (e) -> 
				{
					try {
						out.writeUTF(hold.value);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
				t.start();
				
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
