package edu.wit.yeatesg.mps.phase0.otherdatatypes;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;

import edu.wit.yeatesg.mps.network.clientserver.ConnectClient;

public class Test
{
	public static void main(String[] args) {
		new Thread(() -> {
			new ConnectClient();
		}).start();
	}
}
