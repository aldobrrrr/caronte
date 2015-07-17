package proxy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Consente di stabilire una connessione al proxy TOR tramite protocollo SOCKS5.
 * 
 * @author Emanuele Altomare
 */
public class TorSocks {
	/*
	 * porta del proxy SOCKS5 TOR.
	 */
	public static int proxyPort = 9051;

	/*
	 * indirizzo del proxy.
	 */
	public static String proxyAddr = "127.0.0.1";

	/*
	 * numero di metodi di autenticazione che il client può utilizzare,
	 * specificati nel campo METHODS della richiesta.
	 */
	public static byte nmethods = (byte) 0x01;

	/*
	 * indica la versione del protocollo SOCKS utilizzato.
	 */
	public final static byte SOCKS_VER = (byte) 0x05;

	/**
	 * Apre la socket verso il proxy SOCKS e gestisce il protocollo.
	 * 
	 * @param targetHostname
	 * @param targetPort
	 * @return la socket pronta a ricevere dati.
	 * @throws IOException
	 */
	public static Socket TorSocket(String targetHostname, int targetPort)
			throws IOException {

		/*
		 * apro la socket verso il proxy e prendo gli stream per la
		 * comunicazione.
		 */
		Socket s = new Socket(proxyAddr, proxyPort);
		DataInputStream is = new DataInputStream(s.getInputStream());
		DataOutputStream os = new DataOutputStream(s.getOutputStream());

		/*
		 * creo la parte che specifica il numero ed i metodi di autenticazione
		 * che il client può utilizzare.
		 */
		byte[] clientAuthMethod = { SOCKS_VER, nmethods,
				Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED };

		/*
		 * invio...
		 */
		os.write(clientAuthMethod);

		/*
		 * attendo la risposta del server e memorizzo il primo byte che, se la
		 * risposta è corretta, DEVE rappresentare la versione del protocollo.
		 */
		byte ver = (byte) is.read();

		/*
		 * se, quindi, la versione è quella attesa...
		 */
		if (ver == SOCKS_VER) {
			/*
			 * leggo il prossimo byte che rappresenta il metodo di
			 * autenticazione scelto dal server.
			 */
			byte method = (byte) is.read();

			/*
			 * se non è quello che il client ha fornito come unico
			 * disponibile...
			 */
			if (method != Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED) {
				/*
				 * termino la connessione.
				 */
				s.close();

				/*
				 * lancio un'eccezione per notificare l'accaduto.
				 */
				throw new IOException("Server doesn't accept "
						+ Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED);
			}
		} else {
			/*
			 * chiudo la connessione: la risposta fornita dal server non è
			 * corretta.
			 */
			s.close();
			/*
			 * lancio un'eccezione per notificare l'accaduto.
			 */
			throw new IOException("Malformed server response");
		}

		/*
		 * preparo il buffer che conterrà la richiesta.
		 */
		byte[] hostnameBytes = targetHostname.getBytes();

		/*
		 * definisco la dimensione del buffer
		 */
		ByteBuffer buff = ByteBuffer
				.allocate(5 + targetHostname.getBytes().length + 2);

		/*
		 * inserisco i vari parametri.
		 */
		buff.put(SOCKS_VER);
		buff.put(Socks5Constants.CMD_CONNECT);
		buff.put(Socks5Constants.RSV);

		/*
		 * con questo specifico che gli passerò un nome di dominio e che quindi
		 * la risoluzione dovrà essere remota, in questo modo è in grado di
		 * raggiungere anche i .onion.
		 */
		buff.put(Socks5Constants.ATYP_DOMAINNAME);
		buff.put((byte) hostnameBytes.length);
		buff.put(hostnameBytes);
		buff.putShort((short) targetPort);

		/*
		 * invio...
		 */
		os.write(buff.array());

		/*
		 * aspetto la risposta del server e la processo.
		 */
		ver = (byte) is.read();
		if (ver == SOCKS_VER) {
			byte rep = (byte) is.read();
			/*
			 * se la richiesta non è andata a buon fine...
			 */
			if (rep != Socks5Constants.STATUS_SUCCEEDED) {
				/*
				 * termino la connessione.
				 */
				s.close();

				/*
				 * lancio un'eccezione per notificare l'accaduto dopo aver
				 * parsato il codice di stato contenuto nella risposta.
				 */
				throw (new IOException(ParseSOCKSResponseStatus(rep)));
			}
		} else {
			s.close();
			throw new IOException("Malformed server response");
		}

		/*
		 * leggo gli altri byte della risposta per toglierli dallo stream ma non
		 * mi interessano.
		 */
		for (int i = 0; i < 8; ++i) {
			is.read();
		}

		/*
		 * se sono arrivato qui tutto è andato bene, restituisco la socket per
		 * consentire di inviare i dati.
		 */
		return s;
	}

	/**
	 * Questa funzione consente di generare un messaggio facilmente
	 * comprensibile per un essere umano, a partire da codice di stato di una
	 * risposta SOCKS5.
	 * 
	 * @param status
	 * @return
	 */
	static String ParseSOCKSResponseStatus(byte status) {
		String retval;
		switch (status) {
		case Socks5Constants.STATUS_SUCCEEDED:
			retval = status + " Succeeded.";
			break;
		case Socks5Constants.STATUS_GENERAL_SOCKS_SERVER_FAILURE:
			retval = status + " General SOCKS server failure.";
			break;
		case Socks5Constants.STATUS_CONNECTION_NOT_ALLOWED_BY_RULESET:
			retval = status + " Connection not allowed by ruleset.";
			break;
		case Socks5Constants.STATUS_NETWORK_UNREACHABLE:
			retval = status + " Network unreachable.";
			break;
		case Socks5Constants.STATUS_HOST_UNREACHABLE:
			retval = status + " Host unreachable.";
			break;
		case Socks5Constants.STATUS_CONNECTION_REFUSED:
			retval = status + " Connection refused.";
			break;
		case Socks5Constants.STATUS_TTL_EXPIRED:
			retval = status + " TTL expired.";
			break;
		case Socks5Constants.STATUS_COMMAND_NOT_SUPPORTED:
			retval = status + " Command not supported.";
			break;
		case Socks5Constants.STATUS_ADDRESS_TYPE_NOT_SUPPORTED:
			retval = status + " Address type not supported.";
			break;
		case Socks5Constants.STATUS_TO_0XFF_UNASSIGNED:
			retval = status + " to 0xff unassigned.";
			break;
		default:
			retval = status + " Unknown SOCKS status code.";
		}
		return retval;
	}
}
