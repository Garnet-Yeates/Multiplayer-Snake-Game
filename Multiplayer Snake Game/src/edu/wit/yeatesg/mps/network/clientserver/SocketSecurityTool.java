package edu.wit.yeatesg.mps.network.clientserver;

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

public class SocketSecurityTool
{	
	/** The KeyPairGenerator used to generate the public and private keys */
	private KeyPairGenerator keyGen;

	/** The Cipher instance that this tool is using to encrypt data */
	private Cipher cipher;

	/**
	 * Constructs a new SocketSecurityTool with the given key length. Upon construction, a KeyPairGenerator
	 * is created and the public/private keys for encrypting/decrypting data for this SocketSecurityTool
	 * will be initialized. The public and private keys are respectively associated with the fields
	 * {@link #myKey} and {@link #mySecretKey}. 
	 * @param keyLength
	 */
	public SocketSecurityTool(int keyLength)
	{
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keyLength);
			KeyPair pair = keyGen.generateKeyPair();
			mySecretKey = pair.getPrivate();
			myKey = pair.getPublic();
			cipher = Cipher.getInstance("RSA");			
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to create ASymmetricEncryptionTool");
		}
	}

	/** Sockets who want to send data to the Socket secured by this tool will need this key to encrypt data */
	private PublicKey myKey;

	/**
	 * Sends this SocketSecurityTool's public key to the desired DataOutputStream. Now whenever the Socket
	 * at the other end of the output stream wants to send secret data to the socket on this end, they can
	 * encrypt it with the key. This makes things really secure because now the client on this end is the
	 * only end device that knows how decrypt its relevant data.
	 * @param toWho the DataOutputStream that the public key is being sent to
	 */
	public void sendPublicKey(DataOutputStream toWho)
	{
		try
		{
			byte[] keyBytes = myKey.getEncoded();
			toWho.writeInt(keyBytes.length);
			toWho.write(keyBytes);
		}
		catch (IOException e) { System.out.println("Socket closed before/while trying to send sharable key"); }
	}

	/** The Socket secured by this tool will use this key to send data to another Socket secured by another SocketSecurityTool */
	private PublicKey partnerKey;

	public void setPartnerKey(byte[] keyBytes) 
	{
		try
		{
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			partnerKey = kf.generatePublic(spec);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			e.printStackTrace();
			System.out.println("[ASymmetricEncryptionTool] Failed to set an instance's key using setEncryptionKey(byte[])");
		}

	}
	
	public enum EncryptingFor { SELF, PARTNER }
	
	public static final EncryptingFor ME = EncryptingFor.SELF;
	public static final EncryptingFor PARTNER = EncryptingFor.PARTNER;
	
	public byte[] encryptBytes(byte[] bytes, EncryptingFor who)
	{
		try
		{
			cipher.init(Cipher.ENCRYPT_MODE, who == PARTNER ? partnerKey : myKey);
			return cipher.doFinal(bytes);
		} 
		catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException e)
		{
			System.out.println("[ASymmetricEncryptionTool] An instance failed to encrypt using the key set from setEncryptionKey(byte[])");
			return null;
		}
	}
	
	public byte[] encryptStringBytes(String s, EncryptingFor who)
	{
		return encryptBytes(s.getBytes(), who);
	}

	/**
	 * This is the key that this AsymmetricEncryptionTool will use to decrypt packets that are
	 * sent from other clients. The clients who are sending encrypted packets to this tool should
	 * use this tools {@link #myKey} to encrypt the packets.
	 */
	private PrivateKey mySecretKey;

	public byte[] decryptBytes(byte[] decrypting)
	{
		try
		{
			cipher.init(Cipher.DECRYPT_MODE, mySecretKey);
			return cipher.doFinal(decrypting);
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
		{
			System.out.println("[ASymmetricEncryptionTool] An instance failed to decrypt data");
			e.printStackTrace();
		}
		return null;
	}
	
	public String decryptStringBytes(byte[] stringBytes)
	{
		return new String(decryptBytes(stringBytes));
	}
}