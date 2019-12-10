package edu.wit.yeatesg.mps.network.clientserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import edu.wit.yeatesg.mps.network.clientserver.MPSClient.NotConnectedException;
import edu.wit.yeatesg.mps.network.packets.Packet;

public abstract class SecureSocket
{	
	protected static final boolean ENCRYPT_ENABLED = true;
	
	private static final int KEY_LENGTH = 1024;
	private static final int MAX_DATA_LENGTH = KEY_LENGTH / 8 - 11;

	private static final Random R = new Random();

	/**
	 * Constructs a new SocketSecurityTool with the given key length. Upon construction, a KeyPairGenerator
	 * is created and the public/private keys for encrypting/decrypting data for this SocketSecurityTool
	 * will be initialized. The public and private keys are respectively associated with the fields
	 * {@link #myEncryptionKey} and {@link #secretDecryptionKey}. 
	 * @param keyLength
	 */
	public SecureSocket(boolean initKeys)
	{
		if (initKeys)
			initKeys();
	}

	/** The KeyPairGenerator used to generate the public and private keys */
	protected KeyPairGenerator keyGen;
	
	/** The private key of this SocketSecurityTool. It is used to decrypt data that was encrypted with {@link #myEncryptionKey} */
	protected PrivateKey secretDecryptionKey;

	/** Sockets who want to send data to the Socket secured by this instance will need this key to encrypt data */
	protected PublicKey myEncryptionKey;
	
	protected void initKeys()
	{
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(KEY_LENGTH);
			KeyPair pair = keyGen.generateKeyPair();
			secretDecryptionKey = pair.getPrivate();
			myEncryptionKey = pair.getPublic();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to init the public/private keys of a SecureSocket");
		}
	}

	/**
	 * Sends this SocketSecurityTool's public key to the desired BufferedOutputStream. Now whenever the Socket
	 * encrypt it with the key. This makes things really secure because now the client on this end is the
	 * only end device that knows how decrypt its relevant data.
	 * @param to the BufferedOutputStream that the public key is being sent to
	 */
	public void sendPublicKey(BufferedOutputStream to)
	{
		SecureData data = new SecureData(myEncryptionKey.getEncoded(), MAX_DATA_LENGTH);
		data.send(to);
	}

	private byte[] generateRandomAESKey()
	{
		int keyLength = 32;
		byte[] key = new byte[keyLength];
		R.nextBytes(key);
		return key;
	}

	public class SecureData
	{
		public byte[] merged;
		public byte[][] segmented;

		private boolean encrypted;

		public SecureData(byte[] original, int maxLength)
		{
			merged = original;
			segmented = getSegmented(maxLength);
			encrypted = false;
		}

		public SecureData(BufferedInputStream in) throws IOException, DecryptionFailedException
		{
			initFrom(in);
			decrypt();
		}

		public void encrypt(PublicKey key) throws EncryptionFailedException
		{
			if (key != null)
			{
				try
				{			
					Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");		
					cipher.init(Cipher.ENCRYPT_MODE, key);
					for (int i = 0; i < segmented.length; i++)
						segmented[i] = cipher.doFinal(segmented[i]);
					merged = getMerged();
					encrypted = true;
				}
				catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e)
				{
					e.printStackTrace();
					throw new EncryptionFailedException(e);
					// Convert the 5 checked exceptions to 1 checked.
				}
			}
		}

		protected void decrypt() throws DecryptionFailedException
		{
			try
			{
				if (isEncrypted())
				{
					Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					cipher.init(Cipher.DECRYPT_MODE, secretDecryptionKey);
					for (int i = 0; i < segmented.length; i++)
						segmented[i] = cipher.doFinal(segmented[i]);
					merged = getMerged();
				}
			}
			catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e)
			{
				e.printStackTrace();
				throw new DecryptionFailedException(e);
				// Convert the 5 checked exceptions to 1 unchecked.
			}
		}

		public boolean isEncrypted()
		{
			return encrypted;
		}

		public SecureData()
		{
			merged = null;
			segmented = null;
		}

		public void send(BufferedOutputStream out)
		{
			try
			{
				out.write(encrypted ? 1 : 0);
				out.write(segmented.length);
				for (byte[] segment : segmented)
				{
					out.write(segment.length);
					out.write(segment);
				}
				out.flush();
			}
			catch (Exception e)
			{
				System.out.println("Could not send segmented data");
			}
		}

		public void initFrom(BufferedInputStream in) throws IOException
		{
			encrypted = in.read() == 1 ? true : false;
			byte[][] segmented = new byte[in.read()][];
			for (int i = 0; i < segmented.length; i++)
			{
				byte[] segment = new byte[in.read()];
				in.read(segment);
				segmented[i] = segment;
			}
			this.segmented = segmented;
			merged = getMerged();
		}

		public byte[][] getSegmented(int maxLength)
		{
			byte[][] outerArr = new byte[(int) Math.ceil((double) merged.length / (double) maxLength)][];
			int dataIndex = 0;
			for (int outerIndex = 0; outerIndex < outerArr.length; outerIndex++)
			{
				byte[] inner = new byte[merged.length - dataIndex < maxLength ? merged.length - dataIndex : maxLength];
				for (int innerIndex = 0; innerIndex < inner.length; innerIndex++)
				{
					inner[innerIndex] = merged[dataIndex];
					dataIndex++;	
				}
				outerArr[outerIndex] = inner;
			}
			return outerArr;
		}

		public byte[] getMerged()
		{
			int totalLength = 0;
			for (int b = 0; b < segmented.length; totalLength += segmented[b].length, b++);
			byte[] merged = new byte[totalLength];
			int mergedIndex = 0;
			for (int b = 0; b < segmented.length; b++)
				for (int i = 0; i < segmented[b].length; i++, mergedIndex++)
					merged[mergedIndex] = segmented[b][i];
			return merged;
		}

		public byte[] getMergedArray()
		{
			return merged;
		}

		public byte[][] getSegmentedArray()
		{
			return segmented;
		}		
	}

	public byte[] nextSecureBytes(BufferedInputStream from) throws NotConnectedException, IOException, DecryptionFailedException 
	{
		SecureData bytePacket = new SecureData(from);
		return bytePacket.getMerged();	
	}

	protected String nextSecureString(BufferedInputStream from) throws DecryptionFailedException, IOException
	{
		return new String(nextSecureBytes(from));
	}

	protected Packet nextPacket(BufferedInputStream from) throws DecryptionFailedException, IOException
	{
		return Packet.parsePacket(nextSecureString(from));
	}

	protected void sendPacket(Packet p, BufferedOutputStream to, PublicKey encryptingWith) throws EncryptionFailedException
	{
		SecureData data = this.new SecureData(p.getUTF().getBytes(), MAX_DATA_LENGTH);
		if (encryptingWith != null)
			data.encrypt(encryptingWith);
		data.send(to);
	}

	protected static PublicKey publicKeyFromEncoded(byte[] encoded) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}


	/**
	 * This exception is thrown when {@link #encryptBytes(byte[], Key)} is called using {@value Key#PARTNER} but the
	 * partner key is either null or invalid.
	 * @author yeatesg
	 */
	public static class EncryptionFailedException extends Exception
	{
		private static final long serialVersionUID = 5752720660752085070L;

		private Exception causedBy;

		public EncryptionFailedException(Exception causedBy)
		{
			this.causedBy = causedBy;
		}

		@Override
		public String getMessage()
		{
			return "encryptBytes(byte[]) threw " + causedBy.getClass().getSimpleName() + ".";
		}
	}
	
	/**
	 * This exception is thrown when {@link #decryptBytes(byte[])} is called but a
	 * @author yeatesg
	 */
	public static class DecryptionFailedException extends Exception
	{
		private static final long serialVersionUID = 5752720660752085070L;

		private Exception causedBy;

		public DecryptionFailedException(Exception causedBy)
		{
			this.causedBy = causedBy;
		}

		@Override
		public String getMessage()
		{
			return "decryptBytes(byte[]) threw " + causedBy.getClass().getSimpleName() + ".";
		}
	}
}