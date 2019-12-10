package edu.wit.yeatesg.mps.network.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Implementation
{
	protected KeyPairGenerator keyGen;
	
	protected PrivateKey secretDecryptionKey;

	protected PublicKey myEncryptionKey;
	
	protected void initKeys()
	{
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			KeyPair pair = keyGen.generateKeyPair();
			secretDecryptionKey = pair.getPrivate();
			myEncryptionKey = pair.getPublic();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to init the"
					+ " public/private keys of a SecureSocket");
		}
	}
}
