package edu.wit.yeatesg.mps.network.packets;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class AsymmetricEncryptionTool
{	
	private boolean connectionEstablished;
	
	/**
	 * This is the key that this AsymmetricEncryptionTool will use to decrypt packets that are
	 * sent from other clients. The clients who are sending encrypted packets to this tool should
	 * use this tools {@link #sharingKey} to encrypt the packets.
	 */
	private PrivateKey decryptKey;

	/**
	 * This key is the one that other clients will use to encrypt data that they are sending to
	 * this AsymmetricEncryptionTool
	 */
	private PublicKey sharingKey;

	/**
	 * This is the key that this client is using to encrypt data that it is sending to another
	 * AsymmetricEncryptionTool
	 */
	private PublicKey usingKey;

	/**
	 * The KeyPairGenerator used to generate the private key that this instance uses to decrypt, along
	 * with the public key that is shared with the AsymmetricEncryptionTool on the other end
	 */
	private KeyPairGenerator keyGen;
	
	/**
	 * The Cipher instance that this tool is using to encrypt data
	 */
	private Cipher cipher;

	public AsymmetricEncryptionTool(int keyLength)
	{
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keyLength);
			KeyPair pair = keyGen.generateKeyPair();
			decryptKey = pair.getPrivate();
			sharingKey = pair.getPublic();
			cipher = Cipher.getInstance("RSA");			
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to create ASymmetricEncryptionTool");
		}
	}

	public void sendSharableKey(DataOutputStream toWho)
	{
		try
		{
			byte[] keyBytes = sharingKey.getEncoded();
			toWho.writeInt(keyBytes.length);
			toWho.write(keyBytes);
		}
		catch (IOException e) { System.out.println("Socket closed before/while trying to send sharable key"); }
	}

	public void setEncryptionKey(byte[] keyBytes) 
	{
		try
		{
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			usingKey = kf.generatePublic(spec);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			e.printStackTrace();
			System.out.println("[ASymmetricEncryptionTool] Failed to set an instance's key using setEncryptionKey(byte[])");
		}

	}

	public byte[] encrypt(String s)
	{
		if (usingKey == null)
		{
			System.out.println("[ASymmetricEncryptionTool] Can't encrypt messages without an encryption key from another AsymmetricEncryptionTool!");
		}
		else
		{
			try
			{
				cipher.init(Cipher.ENCRYPT_MODE, usingKey);
				return cipher.doFinal(s.getBytes());
			} 
			catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException e)
			{
				System.out.println("[ASymmetricEncryptionTool] An instance failed to encrypt using the key set from setEncryptionKey(byte[])");
			}
		}
		return null;
	}
	
	public String decrypt(byte[] s)
	{
		try
		{
			cipher.init(Cipher.DECRYPT_MODE, decryptKey);
			return new String(cipher.doFinal(s));
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
		{
			System.out.println("[ASymmetricEncryptionTool] An instance failed to decrypt data");
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isConnectionEstablished()
	{
		return connectionEstablished;
	}
	
	public void setConnectionEstablished(boolean connectionEstablished)
	{
		this.connectionEstablished = connectionEstablished;
	}
	
}