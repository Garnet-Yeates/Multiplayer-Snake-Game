package edu.wit.yeatesg.mps.network.clientserver;
import java.io.DataOutputStream;

public interface ClientListener
{
	public void onReceive(String data);
	public void setOutputStream(DataOutputStream out);
}
