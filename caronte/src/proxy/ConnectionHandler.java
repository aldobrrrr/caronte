package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Rappresenta il gestore di una connesione al server.
 * 
 * @author Emanuele Altomare
 */
public class ConnectionHandler extends Thread {
	private static final int REQ_DIM = 1024;
	private static final int REP_DIM = 4096;
	private static final int TRY_CONNECT_LIMIT = 5;

	private final Socket connection;
	private Socket server = null;

	public ConnectionHandler(Socket connection) {
		this.connection = connection;
	}

	public void run() {
		try {
			/*
			 * dichiaro ed inizializzo la stringa che conterrà i messaggi di
			 * informazione.
			 */
			String message = "";

			/*
			 * creo il buffer per la risposta verso il client.
			 */
			byte[] reply = new byte[REP_DIM];

			/*
			 * prendo gli streams.
			 */
			final InputStream fromClient = connection.getInputStream();
			OutputStream toClient = connection.getOutputStream();

			/*
			 * creo un BufferedReader e gli passo l'input stream, questo mi
			 * consente di creare un buffer sul quale poter inserire un
			 * marcatore per poter "riportare indietro" l'input quando ho finito
			 * di leggere l'header HTTP e poter leggere quest'ultimo riga per
			 * riga.
			 */
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(fromClient));

			/*
			 * setto il mark nel punto iniziale, la dimensione massima che può
			 * essere letta prima di poter riportare indietro in questo punto è
			 * fissata da REQ_DIM.
			 */
			reader.mark(REQ_DIM);

			/*
			 * creo una mappa per inserire gli elementi dell'header HTTP.
			 */
			HashMap<String, String> header = new HashMap<String, String>();

			/*
			 * leggo l'header riga per riga...
			 */
			String line = null;
			String[] couple;

			/*
			 * fino a che il buffer ha dei dati da leggere...
			 */
			while ((line = reader.readLine()) != null) {
				/*
				 * se la riga letta è vuota, l'header è finito ed esco.
				 */
				if (line.equals("")) {
					break;
				}

				/*
				 * stampo la riga per informazione.
				 */
				// System.out.println(line);
				message += line + "\n";

				/*
				 * prendo la riga e creo un array di stringhe con due elementi:
				 * il nome dell'attributo dell'header ed il suo valore. Uso i
				 * primi ":" che vengono letti, come separatore.
				 */
				couple = line.split("\\:", 2);

				/*
				 * se ho due elementi, elimino eventuali spazi ad inizio e fine
				 * e li inserisco nella mappa.
				 */
				if (couple.length == 2) {
					header.put(couple[0].trim(), couple[1].trim());
				}

			}

			/*
			 * ho finito di leggere l'header e quindi resetto il buffer
			 * riportandolo al punto marcato, cioè all'inizio.
			 */
			reader.reset();

			/*
			 * prendo l'host al quale il client si vuole connettere.
			 */
			String host = header.get("Host") != null ? header.get("Host") : "";

			/*
			 * se l'host è vuoto...
			 */
			if (host.isEmpty()) {
				/*
				 * stampo tutto l'header per il debug.
				 */
				// System.out
				// .println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				message += "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";

				Iterator<Entry<String, String>> it = header.entrySet()
						.iterator();

				while (it.hasNext()) {
					Entry<String, String> entry = it.next();
					// System.out
					// .println(entry.getKey() + ": " + entry.getValue());
					message += entry.getKey() + ": " + entry.getValue() + "\n";
				}

				// System.out
				// .println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				message += "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";
			} else {
				/*
				 * altrimenti stampo un separatore.
				 */
				// System.out
				// .println("------------------------------------------------------------------------------------------");
				message += "------------------------------------------------------------------------------------------\n";
			}

			/*
			 * stampo il messaggio contenente tutte le stampe precedenti,
			 * sincronizzandomi con gli altri threads.
			 */
			synchronized (System.out) {
				System.out.println(message);
			}

			/*
			 * è inutile che vado avanti, l'ho messo perchè mi ritrovo delle
			 * connessioni il cui contenuto è vuoto.
			 */
			if (host.isEmpty()) {
				return;
			}

			/*
			 * effettuo la connessione attraverso tor all'host letto in
			 * precedenza per un certo numero di volte specificate da
			 * TRY_CONNECT_LIMIT.
			 */
			server = connect(host, 80, TRY_CONNECT_LIMIT);

			/*
			 * prendo gli streams da e verso il server.
			 */
			InputStream fromServer = server.getInputStream();
			final OutputStream toServer = server.getOutputStream();

			/*
			 * creo un thread per inviare la richiesta del client, in modo tale
			 * da poterlo fare contemporaneamente alla lettura della risposta.
			 */
			Thread t = new Thread() {
				public void run() {
					try {
						String line = null;

						/*
						 * finchè il buffer ha dati da leggere...
						 */
						while ((line = reader.readLine()) != null) {
							/*
							 * riaggiungo il fine linea eliminato dall readLine.
							 */
							line += "\n";

							/*
							 * invio subito al server i byte che compongono la
							 * linea letta.
							 */
							toServer.write(line.getBytes());
							toServer.flush();
						}
						/*
						 * ho svuotato il buffer, quindi lo chiudo.
						 */
						reader.close();
					} catch (IOException e) {
					}

					/*
					 * ho finito di inviare la richiesta del client, quindi
					 * chiudo lo stream con il server.
					 */
					try {
						toServer.close();
					} catch (IOException e) {
					}
				}
			};

			/*
			 * avvio il thread per la scrittura.
			 */
			t.start();

			/*
			 * mentre il thread scrive la richiesta, inizio a leggere la
			 * risposta.
			 */
			int bytes_read;
			try {
				/*
				 * finchè il server invia bytes...
				 */
				while ((bytes_read = fromServer.read(reply)) != -1) {
					/*
					 * invio subito la risposta al client.
					 */
					toClient.write(reply, 0, bytes_read);
					toClient.flush();
				}
			} catch (IOException e) {
			}

			/*
			 * ho finito di trasmettere la risposta al client quindi chiudo lo
			 * stream.
			 */
			toClient.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (server != null)
					server.close();
				if (connection != null)
					connection.close();
			} catch (IOException e) {
			}
		}

	}

	/**
	 * Consente di connettersi ad un particolare host su una determinata porta
	 * attraverso TOR, riprovando per un numero finito di volte pari al
	 * parametro di input LIMIT.
	 * 
	 * @param host
	 * @param port
	 * @param LIMIT
	 * @return
	 * @throws IOException
	 */
	public Socket connect(String host, int port, int LIMIT) throws Exception {
		String lastIOExceptionMessage = "";
		if (host != null && !host.isEmpty()) {
			Socket ret = null;
			for (int i = 0; ret == null && i < LIMIT; ++i) {
				try {
					/*
					 * tento la connessione attraverso TOR.
					 */
					ret = TorSocks.TorSocket(host, port);
				} catch (IOException e) {
					/*
					 * se fallisce memorizzo l'eccezione.
					 */
					lastIOExceptionMessage = e.getMessage();
					ret = null;
				}
			}
			/*
			 * se non sono riuscito a connettermi dopo aver provato LIMIT
			 * volte...
			 */
			if (ret == null) {
				/*
				 * lancio un'eccezione per notificarlo.
				 */
				throw new IOException(
						String.format(
								"Cannot connect to %s on port %s for %d times, the last IOException is: %s",
								host, port, LIMIT, lastIOExceptionMessage));
			}
			/*
			 * sono riuscito a connettermi quindi restituisco la connessione.
			 */
			return ret;
		} else {
			throw new Exception("Parameters error: host field is null or empty");
		}
	}
}