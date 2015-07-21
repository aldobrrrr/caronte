package tordns;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Contiene il codice per poter interpretare una richiesta DNS e fornire una
 * risposta dopo aver effettuato la risoluzione DNS attraverso TOR.
 * 
 * @author Emanuele Altomare
 */
public class DnsRequestHandler extends Thread {

	/*
	 * contiene i bytes della richiesta.
	 */
	private byte[] requestData;

	/*
	 * id della transazione, ciò che lega la richiesta con la risposta: saranno
	 * uguali. (2 bytes)
	 */
	private byte[] transactionId;

	/*
	 * contiene alcuni flags che denotano alcune caratteristiche della
	 * richiesta/risposta. (2 bytes)
	 */
	private byte[] flags;

	/*
	 * contiene il numero di entry nella sezione Queries che viene dopo. (2
	 * bytes)
	 */
	private byte[] questions;

	/*
	 * contiene il numero di entry nella sezione Answer che viene dopo. (2
	 * bytes)
	 */
	private byte[] answerRrs;

	/*
	 * contiene il numero di entry nella sezione Authoritative Nameservers che
	 * viene dopo. (2 bytes)
	 */
	private byte[] authorityRrs;

	/*
	 * contiene il numero di entry nella sezione Additional Records che viene
	 * dopo. (2 bytes)
	 */
	private byte[] additionalRrs;

	/*
	 * INIZIO SEZIONE QUERIES
	 * 
	 * contiene il nome dell'host da risolvere. Il primo byte indica la
	 * lunghezza del nome, mentre l'ultimo è un byte nullo di separazione, in
	 * mezzo vi sono i byte che rappresentano il nome.
	 */
	private byte[] name;

	/*
	 * il tipo di richiesta. (A -> 0x0001) (2 bytes)
	 */
	private byte[] type;

	/*
	 * rappresenta la classe. (IN -> 0x0001) (2 bytes)
	 */
	private byte[] clas;

	/*
	 * INIZIO SEZIONE ANSWERS
	 * 
	 * contiene il puntatore al nome nella sezione queries. (0xc00c)
	 */
	private byte[] answerName;

	/*
	 * contiene il tipo di richiesta. (2 bytes)
	 */
	private byte[] answerType;

	/*
	 * contiene la classe. (2 bytes)
	 */
	private byte[] answerClass;

	/*
	 * contiene il tempo di validità della richiesta. (4 bytes)
	 */
	private byte[] ttl;

	/*
	 * contiene la lunghezza del successivo campo Addr. (2 bytes)
	 */
	private byte[] dataLength;

	/*
	 * contiene l'indirizzo IP relativo all'host. (4 bytes per IPv4)
	 */
	private byte[] addr;

	/*
	 * nel thread di gestione della richiesta accertarsi che sia una richiesta
	 * di tipo A, dopodichè prendere l'hostname dalla sezione Queries campo
	 * name. Verificare che l'hostname non sia relativo ad un hidden service e
	 * risolvere tramite TOR l'hostname per ottenere l'oggetto InetAddress con
	 * l'IP dell'host e generare una risposta DNS infilandoci dentro l'indirizzo
	 * risolto.
	 */

	/*
	 * se l'hostname è relativo ad un hidden service, da approfondire...
	 */

	public DnsRequestHandler(DatagramPacket p) {
		requestData = p.getData();
	}

	public void start() {

		/*
		 * inizio a processare la richiesta è una prova, dovrebbe restituire
		 * sempre 1 e l'hostname da risolvere, con l'ip risolto.
		 */
		ByteBuffer buff = ByteBuffer.wrap(requestData);
		short questionsRRs = buff.getShort(4);

		ArrayList<Byte> arrayList = new ArrayList<Byte>();

		/*
		 * parto dal byte iniziale del campo name della sezione queries.
		 */
		int i = 12;

		/*
		 * per la lunghezza del pacchetto...
		 */
		while (i < requestData.length) {

			/*
			 * se il byte è nullo, l'hotname è finito ed esco.
			 */
			if (requestData[i] == (byte) 0x00) {
				break;
			}

			/*
			 * prendo il byte che determina la lunghezza della parte di hostname
			 * da leggere e che viene sempre prima di quest'ultima.
			 */
			int tempLength = (int) requestData[i];

			/*
			 * adesso parto a leggere dal successivo byte la parte di hostname
			 * attuale.
			 */
			int j = i + 1;

			/*
			 * finchè dura...
			 */
			while (j <= i + tempLength) {

				/*
				 * aggiungo il byte alla lista.
				 */
				arrayList.add(requestData[j]);

				/*
				 * passo al prossimo.
				 */
				++j;
			}

			/*
			 * ho finito di leggere la parte di hostname attuale, aggiungo un
			 * '.' alla lista di byte.
			 */
			arrayList.add((byte) 0x2e);

			/*
			 * sistemo i per poter andare alla parte successiva nell'iterazione
			 * successiva.
			 */
			i = j;
		}

		/*
		 * mi faccio dare l'array dalla lista.
		 */
		Object[] hostBytes = arrayList.toArray();

		/*
		 * creao un array di byte (tipo primitivo).
		 */
		byte[] hostBytesPrimitive = new byte[hostBytes.length];

		/*
		 * converto i Byte in byte (cioè nel tipo primitivo)
		 */
		for (i = 0; i < hostBytes.length; ++i) {
			hostBytesPrimitive[i] = (Byte) hostBytes[i];
		}

		/*
		 * inizializzo la stringa che conterrà l'hostname letto precedentemente
		 * dalla richiesta.
		 */
		String host = "";
		try {

			/*
			 * creo la stringa a partire dall'array di byte.
			 */
			host = new String(hostBytesPrimitive, "ASCII");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		synchronized (System.out) {

			/*
			 * stampo il contatore delle richieste di risoluzione.
			 */
			System.out.println(questionsRRs);

			/*
			 * stampo l'hostname.
			 */
			System.out.println(host);

			try {

				/*
				 * risolvo l'hostname in IP tramite TOR.
				 */
				System.out.println(TorResolve.resolve(host).getHostAddress());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
