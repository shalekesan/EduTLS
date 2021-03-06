package tests;

import java.io.IOException;

import junit.framework.TestCase;
import tls.AlertException;
import tls.CipherSuite;
import tls.State;
import tls.TLSEngine;
import tls.TLSHandshake;
import tls.TLSRecord;

import common.Tools;

import crypto.IRandomGen;
import crypto.cipher.Rijndael;
import crypto.hash.SHA1;
import crypto.random.BlumBlumShub;

public class RecordTest extends TestCase {
	
	public void testCipher() throws Exception {
		State state = new State(new PeerTestImpl());
		Rijndael aes = new Rijndael();
		SHA1 sha = new SHA1();
		crypto.compression.None compress = new crypto.compression.None();
		crypto.keyexchange.None rsa = new crypto.keyexchange.None();
		CipherSuite cipherSuite = new CipherSuite("",(byte)0x1,sha,aes,rsa);
		byte[] clientRandom = new byte[TLSHandshake.RANDOM_SIZE];
		byte[] serverRandom = new byte[TLSHandshake.RANDOM_SIZE];
		IRandomGen random = new BlumBlumShub(TLSHandshake.RANDOM_SIZE);
		clientRandom = random.randBytes(clientRandom.length);
		serverRandom = random.randBytes(serverRandom.length);
		state.setCipherSuite(cipherSuite);
		state.setClientRandom(clientRandom);
		state.setServerRandom(serverRandom);
		state.setChangeCipherSpecClient();
		state.setChangeCipherSpecServer();
		
		String message = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, " +
			"sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
			"Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
			"nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
			"reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
			"pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa " +
			"qui officia deserunt mollit anim id est laborum";
		byte[] plaintext = message.getBytes(TLSEngine.ENCODING);
		TLSRecord record = new TLSRecord(state, plaintext, TLSEngine.APPLICATION);
		
		byte[] ciphertext = record.getCiphertext();
		
		TLSRecord record2 = new TLSRecord(state, ciphertext);
		
		byte[] ciphertext2 = record2.getCiphertext();
		
		Tools.print(Tools.byteArrayToString(ciphertext));
		Tools.print(Tools.byteArrayToString(ciphertext2));
		
		if(!Tools.compareByteArray(ciphertext, ciphertext2))
			fail("encryption failed");
	}
	
	public void testErrorCodes() throws IOException, AlertException {
		PeerTestImpl peer = new PeerTestImpl();
		State state = new State(peer);
		TLSRecord record;
		// Test empty message
		byte[] output = new byte[] {  };
		try {
			record = new TLSRecord(state, output);
			fail("No zero length exception thrown");
		} catch(AlertException e) {
			if(e.getAlertCode() != AlertException.unexpected_message)
				fail("Wrong error type, expected unexpected_message, received " + e.getAlertDescription());
		}
		// Test malformed message
		output = new byte[] { 'a', 'b', 'c', 'd' };
		try {
			record = new TLSRecord(state,output);
			fail("No unsupported version number exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.protocol_version)
				fail("Wrong error type, expected protocol_version, received " + e.getAlertDescription());
		}
		// Test wrong content type
		output = new byte[] { -1, (byte)TLSEngine.VERSION_TLSv1, 0, 3, 1, 1, 1 };
		try {
			record = new TLSRecord(state,output);
			fail("No wrong message exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.illegal_parameter)
				fail("Wrong error type, expected illegal_parameter, received " + e.getAlertDescription());
		}
		// Test wrong content size
		output = new byte[] { TLSEngine.APPLICATION, (byte)TLSEngine.VERSION_TLSv1, 0, 3, 1, 1 };
		try {
			record = new TLSRecord(state,output);
			fail("No illegal parameter exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.illegal_parameter)
				fail("Wrong error type, expected illegal_parameter, received " + e.getAlertDescription());
		}
		// Test wrong version number
		output = new byte[] {TLSEngine.APPLICATION, (byte)31, 2, 1, 1 };
		try {
			record = new TLSRecord(state,output);
			fail("No illegal parameter exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.protocol_version)
				fail("Wrong error type, expected protocol_version, received " + e.getAlertDescription());
		}
		// Test too large message
		output = new byte[TLSEngine.RECORD_SIZE+1];
		output[0] = TLSEngine.APPLICATION;
		output[1] = TLSEngine.VERSION_TLSv1;
		int size = output.length - TLSEngine.HEADER_SIZE;
		if(size<256) {
			output[2] = 0;
			output[3] = (byte)size;
		}
		else {
			output[2] = (byte)Math.ceil(size/256);
			output[3] = (byte)(size%256);
		}
		try {
			record = new TLSRecord(state,output);
			fail("No record_overflow exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.record_overflow)
				fail("Wrong error type, expected record_overflow, received " + e.getAlertDescription());
		}
		// Test content size not as number
		output = new byte[] { TLSEngine.APPLICATION, (byte)TLSEngine.VERSION_TLSv1, 0, 'a', 1, 1 };
		try {
			record = new TLSRecord(state,output);
			fail("No illegal parameter exception thrown");
		} catch (AlertException e) {
			if(e.getAlertCode() != AlertException.illegal_parameter)
				fail("Wrong error type, expected illegal_parameter, received " + e.getAlertDescription());
		}
	}
}
