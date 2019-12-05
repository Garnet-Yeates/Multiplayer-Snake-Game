package edu.wit.yeatesg.mps.network.clientserver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import javax.crypto.spec.SecretKeySpec;

public class SocketSecurityTool
{	
	/** The KeyPairGenerator used to generate the public and private keys */
	private KeyPairGenerator keyGen;

	/** The Cipher instance that this tool is using to encrypt data */
	private Cipher cipher;

	public static final int KEY_LENGTH = 1024;
	public static final int MAX_DATA_LENGTH = KEY_LENGTH / 8 - 11;
	
	public static final Random R = new Random();
	
	public static void main(String[] args) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		
		byte[] key = "12345678123456783456567812343454".getBytes("UTF-8");
		
		SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
		
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        cipher.doFinal("I am encrypting this string".getBytes("UTF-8"));
	}
	
	
	private byte[] generateRandomAESKey()
	{
		int keyLength = 32;
		byte[] key = new byte[keyLength];
		for (int i = 0; i < key.length; i++)
			key[i] = (byte) R.nextInt(10);
		return key;
	}
	
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
			cipher = Cipher.getInstance("RSA");		
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to create ASymmetricEncryptionTool");
		}
	}

	/** Sockets who want to send data to the Socket secured by this tool will need this key to encrypt data */
	private PublicKey myEncryptionKey;

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
			byte[] keyBytes = myEncryptionKey.getEncoded();
			toWho.writeInt(1);
			toWho.writeInt(keyBytes.length);
			toWho.write(keyBytes);
		}
		catch (IOException e) { System.out.println("Socket closed before/while trying to send sharable key"); }
	}

	/** Represents the public key of some other SocketSecurityTool that this tool is sending encrypted data to */
	private PublicKey partnerEncryptionKey;

	/**
	 * Sets this SocketSecurityTool's {@link #partnerEncryptionKey} to the PublicKey represented by the given
	 * byte[]. Generally speaking, if two Sockets that are both secured using a SocketSecurityTool want to send
	 * encrypted data to each other, they should both send their public keys to each other as a byte[] by using the
	 * {@link #sendPublicKey(DataOutputStream)} method, and once they receive them, they should call
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
	
	public static byte[] merge(byte[][] blocks)
	{
		int totalLength = 0;
		for (int b = 0; b < blocks.length; totalLength += blocks[b].length, b++);
		byte[] merged = new byte[totalLength];
		int mergedIndex = 0;
		for (int b = 0; b < blocks.length; b++)
			for (int i = 0; i < blocks[b].length; i++, mergedIndex++)
				merged[mergedIndex] = blocks[b][i];
		return merged;
	}
	
	public enum Key { SELF, PARTNER }

	public static final Key SELF = Key.SELF;
	public static final Key PARTNER = Key.PARTNER;

	/**
	 * Encrypts the given array of bytes using either {@link #myEncryptionKey} or {@link #partnerEncryptionKey}.
	 * <br><br>Before being encrypted, the byte[] is segmented into an array of smaller arrays. This is because a byte[]
	 * can have a length of no more than {@value #MAX_DATA_LENGTH} if it is being encrypted. So a byte[y] with length
	 * y would be split into a 2D byte[x][y] with length x where<br><br> 
	 * <code>x = (int) Math.ceil(y / {@value #MAX_DATA_LENGTH})<br><br>
	 * The data can be encrypted using either {@link Key#SELF} or {@link Key#PARTNER}. If the self key is used, then
	 * the data is encrypted using the public key of this SocketSecurityTool. If the partner key is used, then the
	 * data is encrypted using the key that was received from another SocketSecurityTool on another end device (if
	 * you are trying to send encrypted data to another end device using asymmetric encryption, you must use 
	 * {@link #setPartnerKey(byte[])} to set the partner key of this SocketSecurityTool to the public key of the
	 * SocketSecurityTool you are sending data to.
	 * <br><br><i>This method is synchronized along with {@link #decryptBytes(byte[])} on each instance. This prevents the
	 * rare case where, from different threads, both {@link #encryptBytes(byte[], Key)} and {@link #decryptBytes(byte[])} are
	 * called at the same time. If this were to happen, then there is a possibility that the cipher will be in encryption mode
	 * while trying to decrypt, or vice versa, which can cause numerous errors.
	 * running simultaneously.</i>
	 * @param bytes the byte[] that is being encrypted.
	 * @param whichKey determines whether {@link #myEncryptionKey} or {@link #partnerEncryptionKey} is used.
	 * @return a byte[] representing the encrypted version of the given array using the selected key.
	 * @throws EncryptionFailedException if the key is invalid or if some other relevant exception is thrown.
	 */
	public synchronized byte[][] encryptBytes(byte[] bytes, Key whichKey) throws EncryptionFailedException
	{	
		try
		{
			cipher.init(Cipher.ENCRYPT_MODE, whichKey == PARTNER ? partnerEncryptionKey : myEncryptionKey);

			System.out.println("Raw Bytes (len = " + bytes.length + "):");
			for (byte b : bytes)
				System.out.print(" " + b);
			
			byte[][] byteSegments = segment(bytes, MAX_DATA_LENGTH);
			System.out.println("\nSegmented Bytes:");
			for (int s = 0; s < byteSegments.length; s++)
			{
				byte[] segment = byteSegments[s];
				System.out.print("[" + s + "] [len = " + segment.length + "]");
				for (byte b : segment)
					System.out.print(" " + b);
				System.out.println();
			}
			
			byte[][] encryptedSegments = new byte[byteSegments.length][];
			for (int s = 0; s < byteSegments.length; s++)
				encryptedSegments[s] = cipher.doFinal(byteSegments[s]);
			System.out.println("Encrypted Bytes:");
			for (int s = 0; s < encryptedSegments.length; s++)
			{
				byte[] encryptedSegment = encryptedSegments[s];
				System.out.print("[" + s + "] [len = " + encryptedSegment.length + "]");
				for (byte b : encryptedSegment)
					System.out.print(" " + b);
				System.out.println();
			}
			return encryptedSegments;
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
		{
			/* InvalidKeyException:
			 *   will be thrown in the case where this method was called with the PARTNER key but it is currently set
			 *   to null. Should never be thrown with the SELF key since it is initialized upon construction.
			 * BadPaddingException:
			 *   is thrown in the case where the cipher is in decrypt mode while trying to encrypt. So it is expecting
			 *   encrypted data, but gets plain data. THIS EXCEPTION SHOULD NEVER BE THROWN HERE: it is caused by
			 *   decryptBytes() and encryptBytes() running concurrently, and I synchronized them on each SocketSecurityTool
			 *   instance to avoid this.
			 * InvalidBlockSizeException:
			 *   if this exception is thrown, then there is something wrong with the input bytes, "the length of the
			 *   input data does not match the block size of the cipher" */
			e.printStackTrace();
			throw new EncryptionFailedException(e);
//			Convert the 3 checked exceptions to 1 unchecked. We shouldn't EVER into any of those 3 unless programming error, so if we do, crash program
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
	public byte[][] encryptString(String string, Key whichKey) throws EncryptionFailedException
	{
		System.out.println("Original String: " + string);
		return encryptBytes(string.getBytes(), whichKey);
	}
	
	/**
	 * This exception is thrown when {@link #encryptBytes(byte[], Key)} is called using {@value Key#PARTNER} but the
	 * partner key is either null or invalid.
	 * @author yeatesg
	 */
	public static class EncryptionFailedException extends RuntimeException
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
		
		@Override
		public void printStackTrace()
		{
			System.out.println(getMessage());
			super.printStackTrace();
			System.out.println("\nCAUSED BY:");
			causedBy.printStackTrace();
		}
	}

	/** The private key of this SocketSecurityTool. It is used to decrypt data that was encrypted with {@link #myEncryptionKey} */
	private PrivateKey secretDecryptionKey;

	/**
	 * Decrypts the given byte array using the PrivateKey of this SocketSecurityTool, represented by {@link #secretDecryptionKey}.
	 * This key was generated upon initialization by the {@link #keyGen}.
	 * <br><br><i>This method is synchronized along with {@link #encryptBytes(byte[], Key)} on each instance. This prevents the
	 * rare case where, from different threads, both {@link #encryptBytes(byte[], Key)} and {@link #decryptBytes(byte[])} are
	 * called at the same time. If this were to happen, then there is a possibility that the cipher will be in encryption mode
	 * while trying to decrypt, or vice versa, which can cause numerous errors.
	 * running simultaneously.</i>
	 * @param encryptedBytes the byte[] that is being decrypted
	 * @return a byte[] representing the decrypted version of the input byte[] using {@link #secretDecryptionKey} 
	 * @throws RuntimeException if the decryption failed
	 */
	public synchronized byte[] decryptBytes(byte[][] encryptedByteBlocks) throws DecryptionFailedException
	{
		try
		{
			cipher.init(Cipher.DECRYPT_MODE, secretDecryptionKey);
			byte[][] decryptedByteBlocks = new byte[encryptedByteBlocks.length][];
			for (int b = 0; b < encryptedByteBlocks.length; b++)
				decryptedByteBlocks[b] = cipher.doFinal(encryptedByteBlocks[b]);
			return merge(decryptedByteBlocks);
		}
		catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			/* InvalidKeyException
			 *   should never be thrown for decryption so if that is caught, something is wrong with the program
			 * BadPaddingException
			 *   is thrown in the case where the cipher is expecting encrypted data but gets plain data. This exception
			 *   is the reason why the server must NOT decrypt data from a given client unless they KNOW that 1) they shared
			 *   their encryption key with that client, and 2) that client is actually using the key and sending encrypted data
			 * IllegalBlockSizeException
			 *   if this exception is thrown, then the cipher is in encryption mode while trying to decrypt. THIS EXCEPTION 
			 *   NEVER BE THROWN: it is caused by decryptBytes() and encryptBytes() running concurrently, and I synchronized
			 *   them on each SocketSecurityTool instance to avoid this */
			throw new DecryptionFailedException(e);
//			Convert the 3 checked exceptions to 1 unchecked. We shouldn't EVER into any of those 3 unless programming error, so if we do, crash program
		}
	}

	/**
	 * Treats the given byte[] as an encoded String that was encrypted, decrypts it using {@link #decryptBytes(byte[])},
	 * and converts it back into a String and returns it.
	 * @param encryptedStringBytes the byte[] that is being decrypted and converted into a String.
	 * @return the byte[] that was decrypted using {@link #secretDecryptionKey}, converted to a String.
	 */
	public String decryptString(byte[][] encryptedStringBytes) throws DecryptionFailedException
	{
		return new String(decryptBytes(encryptedStringBytes));
	}
	
	/**
	 * This exception is thrown when {@link #decryptBytes(byte[])} is called but a
	 * @author yeatesg
	 */
	public static class DecryptionFailedException extends RuntimeException
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
		
		@Override
		public void printStackTrace()
		{
			System.out.println(getMessage());
			super.printStackTrace();
			System.out.println("\n\nCAUSED BY:\n\n");
			causedBy.printStackTrace();
		}
	}
	
}
