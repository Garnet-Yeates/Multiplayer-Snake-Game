package edu.wit.yeatesg.mps.phase1.connect;
import java.io.DataOutputStream;

public interface ClientListener {
	public void onReceive(String data);
	public void setOutputStream(DataOutputStream out);
}
