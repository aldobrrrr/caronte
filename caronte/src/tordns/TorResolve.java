package tordns;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Consente di effettuare una risoluzione DNS attraverso TOR.
 * 
 * @author Emanuele Altomare
 */
public class TorResolve {

	/*
	 * porta del proxy TOR.
	 */
	public static int proxyPort = 9050;

	/*
	 * indirizzo del proxy TOR.
	 */
	public static String proxyAddr = "127.0.0.1";

	/*
	 * Costante per dire a TOR di effettuare una risoluzione DNS.
	 */
	public final static byte TOR_RESOLVE = (byte) 0xF0;

	/*
	 * Costante per dire quale versione di SOCKS utilizzare
	 */
	public final static byte SOCKS_VERSION = (byte) 0x04;

	/*
	 * separatore.
	 */
	public final static byte SOCKS_DELIM = (byte) 0x00;

	/*
	 * Settando il campo IP a 0.0.0.1 abilito l'uso di SOCKS4a.
	 */
	public final static int SOCKS4A_FAKEIP = (int) 0x01;

	/**
	 * Consente di risolvere un nome attravero TOR.
	 * 
	 * @param hostname
	 * @return
	 * @throws Exception
	 */
	public static InetAddress resolve(String hostname) throws Exception {
		if (hostname != null && !hostname.isEmpty()) {
			Socket s = new Socket(proxyAddr, proxyPort);

			DataInputStream is = new DataInputStream(s.getInputStream());
			DataOutputStream os = new DataOutputStream(s.getOutputStream());

			os.writeByte(SOCKS_VERSION);
			os.writeByte(TOR_RESOLVE);
			/*
			 * scrivo i due byte della porta, in questo caso uso 0 perchè la
			 * porta non serve per la risoluzione.
			 */
			os.writeShort(0);
			os.writeInt(SOCKS4A_FAKEIP);
			os.writeByte(SOCKS_DELIM);
			os.writeBytes(hostname);
			os.writeByte(SOCKS_DELIM);

			/*
			 * elimino il byte di versione del protocollo dallo stream.
			 */
			is.readByte();
			
			/*
			 * leggo il codice di stato.
			 */
			byte status = is.readByte();
			
			/*
			 * se non è 90 --> SUCCESSO...
			 */
			if (status != (byte) 90) {
				
				/*
				 * chiudo la connessione e lancio un'eccezione dopo aver parsato
				 * il codice di stato.
				 */
				s.close();
				throw (new IOException(ParseSOCKSResponseStatus(status)));
			}

			/*
			 * leggo i due byte della porta per eliminarli dallo stream.
			 */
			is.readShort();

			/*
			 * creao un array di 4 byte per ospitare l'indirizzo risolto e lo
			 * memorizzo.
			 */
			byte[] ipAddrBytes = new byte[4];
			is.read(ipAddrBytes);

			/*
			 * creo un pratico oggetto rappresentante un indirizzo IP.
			 */
			InetAddress ia = InetAddress.getByAddress(ipAddrBytes);

			/*
			 * ho finito quindi chiudo tutto.
			 */
			is.close();
			os.close();
			s.close();

			/*
			 * ritorno l'oggetto indirizzo.
			 */
			return (ia);
		} else {
			throw new Exception("the hostname is null or empty!");
		}
	}

	static String ParseSOCKSResponseStatus(byte status) {
		String retval;
		switch (status) {
		case 90:
			retval = status + " Request granted.";
			break;
		case 91:
			retval = status + " Request rejected/failed - unknown reason.";
			break;
		case 92:
			retval = status
					+ " Request rejected: SOCKS server cannot connect to identd on the client.";
			break;
		case 93:
			retval = status
					+ " Request rejected: the client program and identd report different user-ids.";
			break;
		default:
			retval = status + " Unknown SOCKS status code.";
		}
		return (retval);

	}
}
