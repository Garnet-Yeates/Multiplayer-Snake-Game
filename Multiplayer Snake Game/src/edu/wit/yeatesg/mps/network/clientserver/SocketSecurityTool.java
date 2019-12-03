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
	 * {@link #myEncryptionKey} and {@link #secretDecryptionKey}. 
	 * @param keyLength
	 */
	public SocketSecurityTool(int keyLength)
	{
		try
		{
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keyLength);
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
	
	public enum Key { SELF, PARTNER }

	public static final Key ME = Key.SELF;
	public static final Key PARTNER = Key.PARTNER;

	/**
	 * Encrypts the given array of bytes using either {@link #myEncryptionKey} or {@link #partnerEncryptionKey}.
	 * @param bytes the byte[] that is being encrypted.
	 * @param whichKey determines whether {@link #myEncryptionKey} or {@link #partnerEncryptionKey} is used.
	 * @return a byte[] representing the encrypted version of the given array using the selected key.
	 * @throws EncryptionFailedException if the key is invalid or if some other relevant exception is thrown.
	 */
	public byte[] encryptBytes(byte[] bytes, Key whichKey) throws EncryptionFailedException
	{	
		try
		{
			cipher.init(Cipher.ENCRYPT_MODE, whichKey == PARTNER ? partnerEncryptionKey : myEncryptionKey);
			return cipher.doFinal(bytes);
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
		{
			throw new EncryptionFailedException(e);
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
	public byte[] encryptString(String string, Key whichKey) throws EncryptionFailedException
	{
		return encryptBytes(string.getBytes(), whichKey);
	}

	/** The private key of this SocketSecurityTool. It is used to decrypt data that was encrypted with {@link #myEncryptionKey} */
	private PrivateKey secretDecryptionKey;

	/**
	 * Decrypts the given byte array using the PrivateKey of this SocketSecurityTool, represented by
	 * {@link #secretDecryptionKey}. This key was generated upon initialization by the {@link #keyGen}
	 * @param encryptedBytes the byte[] that is being decrypted
	 * @return a byte[] representing the decrypted version of the input byte[] using {@link #secretDecryptionKey} 
	 * @throws RuntimeException
	 */
	public byte[] decryptBytes(byte[] encryptedBytes)
	{
		try
		{
			cipher.init(Cipher.DECRYPT_MODE, secretDecryptionKey);
			return cipher.doFinal(encryptedBytes);
		}
		catch (Exception e)
		{
//			DecryptBytes should never normally throw an exception so if it does, crash program
			throw new RuntimeException();
		}
	}

	/**
	 * Treats the given byte[] as an encoded String that was encrypted, decrypts it using {@link #decryptBytes(byte[])},
	 * and converts it back into a String and returns it.
	 * @param encryptedStringBytes the byte[] that is being decrypted and converted into a String.
	 * @return the byte[] that was decrypted using {@link #secretDecryptionKey}, converted to a String.
	 */
	public String decryptString(byte[] encryptedStringBytes)
	{
		return new String(decryptBytes(encryptedStringBytes));
	}
}
/**
 * This exception is thrown when {@link #encryptBytes(byte[], Key)} is called using {@value Key#PARTNER} but the
 * partner key is either null or invalid.
 * @author yeatesg
 */
class EncryptionFailedException extends RuntimeException
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
		super.printStackTrace();
		System.out.println("\nCAUSED BY:");
		causedBy.printStackTrace();
	}
}