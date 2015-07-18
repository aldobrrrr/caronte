package proxy.socks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import proxy.Socks5Constants;

/**
 * 
 * @author andrea
 *
 */
public class TorSocket extends Socket {

	/**
	 * TODO: Descrizione del costruttore.
	 * 
	 * @param proxyHost
	 * @param proxyPort
	 * @param targetHost
	 * @param targetPort
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public TorSocket(final String proxyHost, int proxyPort,
			final String targetHost, int targetPort)
			throws UnknownHostException, IOException {
		/*
		 * La chiamata al proxy
		 */
		super(proxyHost, proxyPort);
		/*
		 * Si ottengono gli stream di input e di output.
		 */
		final DataInputStream in = new DataInputStream(getInputStream());
		final DataOutputStream out = new DataOutputStream(getOutputStream());
		/*
		 * Scrivo la versione del protocollo, il numero di metodi di
		 * autenticazione e il metodo di nessuna autenticazione.
		 */
		out.write(new byte[] { Socks5Constants.SOCKS_VER_5, 0x01,
				Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED });

		switch (in.read()) {
		case Socks5Constants.SOCKS_VER_5:
			/*
			 * Leggo il metodo di ritorno che deve essere quello scelto
			 * inizialmente, e dunque siamo con:
			 * 
			 * AUTH_NO_AUTENTICATION_REQUIRED
			 */
			switch (in.readByte()) {
			case Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED:
				/* Tutto ok, non bisogna fare nulla qui. */
				break;

			default:
				/*
				 * Il caso di default indica che c'è stato un errore nel metodo
				 * di autenticazione
				 */
				close();
				throw new IOException(String.format(
						"SOCKS Server does not accept auth method: %s",
						Socks5Constants.AUTH_NO_AUTHENTICATION_REQUIRED));
			}

			break;
		default:
			close();
			throw new IOException("Malformed Socks response.");
		}

		/*
		 * Si prepara la richiesta di connessione.
		 * 
		 * I primi 5 byte sono VER, CMD, RSV, ATYP, DOMAIN_LENGTH. Gli utlimi 2
		 * byte sono la dimensione della porta.
		 */
		final ByteBuffer byteBuffer = ByteBuffer.allocate(0x05 + targetHost
				.length() + 0x02);
		/*
		 * Si scrive sul buffer
		 */
		byteBuffer.put(Socks5Constants.SOCKS_VER_5);
		byteBuffer.put(Socks5Constants.CMD_CONNECT);
		byteBuffer.put(Socks5Constants.RSV);
		byteBuffer.put(Socks5Constants.ATYP_DOMAINNAME);
		byteBuffer.put((byte) targetHost.length());
		byteBuffer.put(targetHost.getBytes());
		byteBuffer.putShort((short) targetPort);

		/* Si scrive sull'output stream */
		out.write(byteBuffer.array());
		out.flush();

		/* Attendo la risposta del server. Il primo byte è la versione del SOCKS */
		switch (in.read()) {
		case Socks5Constants.SOCKS_VER_5:
			/*
			 * Il secondo byte che si riceve è la risposta del server alla
			 * richiesta precedente.
			 */
			byte replyStatus = (byte) 0xFF;
			switch ((replyStatus = (byte) in.read())) {
			case Socks5Constants.STATUS_SUCCEEDED:
				/*
				 * La richiesta è andata a buon fine, posso scartare i byte
				 * successivi che non mi servono a nulla. Li leggo e li scarto.
				 * QUESTO PERCHE SONO:
				 * 
				 * RSV, AYTP, 4 BYTE DI IPV4 (AYTP == IPV4), 2 PORT = 8 BYTE
				 */
				in.read(new byte[8]);
				break;

			default:
				/*
				 * C'è stato un errore durante la richiesta di CONNECT, si
				 * lancia una eccezione che ritorna lo stato nella risposta.
				 */
				close();
				throw new IOException(socksReplyErrnoDescription(replyStatus));
			}
			break;

		default:
			/* La versione del socks è sconosciuta. */
			close();
			throw new IOException("Unknown SOCKS version.");
		}
	}

	/**
	 * A partire dal codice di ritorno contenuto in una risposta del server si
	 * ottiene la sua descrizione.
	 * 
	 * @param status
	 * @return
	 */
	private String socksReplyErrnoDescription(final byte status) {
		String s = null;
		switch (status) {
		case Socks5Constants.STATUS_SUCCEEDED:
			s = "Succeded.";
			break;
		case Socks5Constants.STATUS_GENERAL_SOCKS_SERVER_FAILURE:
			s = "General SOCKS server failure.";
			break;
		case Socks5Constants.STATUS_CONNECTION_NOT_ALLOWED_BY_RULESET:
			s = "Connection not allowed by ruleset.";
			break;
		case Socks5Constants.STATUS_NETWORK_UNREACHABLE:
			s = "Network unreachable.";
			break;
		case Socks5Constants.STATUS_HOST_UNREACHABLE:
			s = "Host unreachable.";
			break;
		case Socks5Constants.STATUS_CONNECTION_REFUSED:
			s = "Connection refused.";
			break;
		case Socks5Constants.STATUS_TTL_EXPIRED:
			s = "TTL expired.";
			break;
		case Socks5Constants.STATUS_COMMAND_NOT_SUPPORTED:
			s = "Command not supported.";
			break;
		case Socks5Constants.STATUS_ADDRESS_TYPE_NOT_SUPPORTED:
			s = "Address type not supported.";
			break;
		case Socks5Constants.STATUS_TO_0XFF_UNASSIGNED:
			s = "0xFF unassigned.";
			break;
		default:
			s = "Unknown SOCKS status.";
		}
		/* Ritorna la stringa formattata con errore testuale e codice. */
		return String.format("%s Code:%x", s, (byte) status);
	}

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		throw new UnsupportedOperationException();
	}

	public static void main(String args[]) {
		try {
			// Socket socket = TorSocks.TorSocket("digiamici.it", 80);
			Socket socket = new TorSocket("127.0.0.1", 9051, "digiamici.it", 80);
			System.out.println(socket.isConnected());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
