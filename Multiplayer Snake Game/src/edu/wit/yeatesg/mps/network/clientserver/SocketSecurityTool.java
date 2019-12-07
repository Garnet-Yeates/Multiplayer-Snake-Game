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
import edu.wit.yeatesg.mps.network.clientserver.NetworkClient.ReceiveFailedException;

public class SocketSecurityTool
{	
	/** The KeyPairGenerator used to generate the public and private keys */
	private KeyPairGenerator keyGen;

	/** The Cipher instance that this tool is using to encrypt data */

	public static final int KEY_LENGTH = 1024;
	public static final int MAX_DATA_LENGTH = KEY_LENGTH / 8 - 11;

	public static final Random R = new Random();

	/**
	 * Constructs a new SocketSecurityTool with the given key length. Upon construction, a KeyPairGenerator
	 * is created and the public/private keys for encrypting/decrypting data for this SocketSecurityTool
	 * will be initialized. The public and private keys are respectively associated with the fields
	 * {@link #myEncryptionKey} and {@link #secretDecryptionKey}. 
	 * @param keyLength
	 */
	public SocketSecurityTool()
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
			throw new RuntimeException("Failed to create ASymmetricEncryptionTool");
		}
	}

	/** Sockets who want to send data to the Socket secured by this instance will need this key to encrypt data */
	private PublicKey myEncryptionKey;

	/**
	 * Sends this SocketSecurityTool's public key to the desired BufferedOutputStream. Now whenever the Socket
	 * encrypt it with the key. This makes things really secure because now the client on this end is the
	 * only end device that knows how decrypt its relevant data.
	 * @param to the BufferedOutputStream that the public key is being sent to
	 */
	public void sendPublicKey(BufferedOutputStream to)
	{
		SegmentedData data = new SegmentedData(myEncryptionKey.getEncoded(), MAX_DATA_LENGTH);
		data.send(to);
	}

	/** Represents the public key of some other SocketSecurityTool that this tool is sending encrypted data to */
	private PublicKey partnerEncryptionKey;

	/**
	 * Sets this SocketSecurityTool's {@link #partnerEncryptionKey} to the PublicKey represented by the given
	 * byte[]. Generally speaking, if two Sockets that are both secured using a SocketSecurityTool want to send
	 * encrypted data to each other, they should both send their public keys to each other as a byte[] by using the
	 * {@link #sendPublicKey(BufferedOutputStream)} method, and once they receive them, they should call
	 * {@link #setPartnerKey(byte[])} to set their partner key to the other client's public key. Once this is
	 * established, they are "partnered" with each other, meaning they can both encrypt and send data to each other
	 * using their partner key, and they can both decrypt data received from the other by using their private key.
	 * @param encodedKey the encoded version of the partner key
	 */
	public void setPartnerKey(byte[] encodedKey) 
	{
		try
		{
			X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			partnerEncryptionKey = kf.generatePublic(spec);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			e.printStackTrace();
			System.out.println("[ASymmetricEncryptionTool] Failed to set an instance's key using setEncryptionKey(byte[])");
		}

	}

	public enum Key { SELF, PARTNER }

	public static final Key SELF = Key.SELF;
	public static final Key PARTNER = Key.PARTNER;

	private byte[] generateRandomAESKey()
	{
		int keyLength = 32;
		byte[] key = new byte[keyLength];
		for (int i = 0; i < key.length; i++)
			key[i] = (byte) R.nextInt(10);
		return key;
	}


	public SegmentedData encryptBytes(byte[] bytes, Key whichKey) throws EncryptionFailedException
	{	
		try
		{			
			SegmentedData segmented = new SegmentedData(bytes, MAX_DATA_LENGTH);
			Cipher cipher = Cipher.getInstance("RSA");		
			cipher.init(Cipher.ENCRYPT_MODE, whichKey == PARTNER ? partnerEncryptionKey : myEncryptionKey);
			byte[][] segments = segmented.segmented;
			for (int i = 0; i < segments.length; i++)
				segments[i] = cipher.doFinal(segments[i]);
			segmented.merged = SegmentedData.merge(segments);
			segmented.encrypted = true;
			return segmented;
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			e.printStackTrace();
			throw new EncryptionFailedException(e);
			// Convert the 5 checked exceptions to 1 checked.
		}
	}
	
	
	public static class SegmentedData
	{
		public byte[] merged;
		public byte[][] segmented;
		
		private boolean encrypted;
		
		public SegmentedData(byte[] original, int maxLength)
		{
			segmented = SegmentedData.segment(original, maxLength);
			encrypted = false;
		}
		
		public boolean isEncrypted()
		{
			return encrypted;
		}
		
		public SegmentedData()
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
			catch (IOException e)
			{
				System.out.println("Could not send segmented data");
			}
		}
		
		public static SegmentedData receiveSegmentedData(BufferedInputStream in) throws ReceiveFailedException
		{
			try
			{
				boolean encrypted = in.read() == 1 ? true : false;
				int numSegments = in.read();
				byte[][] segments = new byte[numSegments][];
				System.out.println(numSegments);
				for (int i = 0; i < segments.length; i++)
				{
					int segmentLength = in.read();
					byte[] segment = new byte[segmentLength];
					in.read(segment);
					segments[i] = segment;
				}
				SegmentedData received = new SegmentedData();
				received.merged = merge(segments);
				for (byte b : received.merged)
					System.out.print(" " + b);
				System.out.println();
				received.segmented = segments;
				received.encrypted = encrypted;
				return received;
			}
			catch (IOException e)
			{
				System.out.println("Could not receive segmented data");
				throw new ReceiveFailedException();
			}
		}
		
		public static byte[][] segment(byte[] data, int maxLength)
		{
			byte[][] outerArr = new byte[(int) Math.ceil((double) data.length / (double) maxLength)][];
			int dataIndex = 0;
			for (int outerIndex = 0; outerIndex < outerArr.length; outerIndex++)
			{
				byte[] inner = new byte[data.length - dataIndex < maxLength ? data.length - dataIndex : maxLength];
				for (int innerIndex = 0; innerIndex < inner.length; innerIndex++)
				{
					inner[innerIndex] = data[dataIndex];
					dataIndex++;	
				}
				outerArr[outerIndex] = inner;
			}
			return outerArr;
		}
		
		public static byte[] merge(byte[][] segmented)
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

		public byte[] getOriginalByteArray()
		{
			return merged;
		}
		
		public byte[][] getSegmentedArray()
		{
			return segmented;
		}		
	}

	/**
	 * Converts the given String to an array of bytes using {@link String#getBytes()} and calls
	 * {@link #encryptBytes(byte[], Key)} on the byte array.
	 * @param string the String that is being encrypted.
	 * @param whichKey determines whether {@link #myEncryptionKey} or {@link #partnerEncryptionKey} is used.
	 * @return a byte[] representing the encrypted bytes of the String.
	 * @throws EncryptionFailedException if it is thrown by {@link #encryptBytes(byte[], Key)}
	 */
	public SegmentedData encryptString(String string, Key whichKey) throws EncryptionFailedException
	{
		return encryptBytes(string.getBytes(), whichKey);
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

	/** The private key of this SocketSecurityTool. It is used to decrypt data that was encrypted with {@link #myEncryptionKey} */
	private PrivateKey secretDecryptionKey;

	public synchronized byte[] decryptBytes(SegmentedData data) throws DecryptionFailedException
	{
		try
		{
			if (data.isEncrypted())
			{
				Cipher cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.DECRYPT_MODE, secretDecryptionKey);
				byte[][] segmentedArray = data.getSegmentedArray();
				for (int i = 0; i < segmentedArray.length; i++)
					segmentedArray[i] = cipher.doFinal(segmentedArray[i]);
				return SegmentedData.merge(segmentedArray);
			}
			else return data.getOriginalByteArray();
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			e.printStackTrace();
			throw new DecryptionFailedException(e);
			// Convert the 5 checked exceptions to 1 unchecked.
		}
	}

	/**
	 * Treats the given byte[] as an encoded String that was encrypted, decrypts it using {@link #decryptBytes(byte[])},
	 * and converts it back into a String and returns it.
	 * @param encryptedStringData the byte[] that is being decrypted and converted into a String.
	 * @return the byte[] that was decrypted using {@link #secretDecryptionKey}, converted to a String.
	 */
	public String decryptString(SegmentedData encryptedStringData) throws DecryptionFailedException
	{
		String ret = new String(decryptBytes(encryptedStringData));
		System.out.println("Bytes after decryption:");
		for (byte b : ret.getBytes())
			System.out.print(" " + b);
		System.out.println("Converted to string: " + ret);
		return ret;
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
