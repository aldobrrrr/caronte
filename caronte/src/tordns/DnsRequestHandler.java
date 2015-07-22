package tordns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

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
	private final DatagramPacket requestPacket;

	private final DatagramSocket server;

	/*
	 * Verificare che l'hostname non sia relativo ad un hidden service e
	 * risolvere tramite TOR l'hostname per ottenere l'oggetto InetAddress con
	 * l'IP dell'host e generare una risposta DNS infilandoci dentro l'indirizzo
	 * risolto.
	 */

	/*
	 * se l'hostname è relativo ad un hidden service, da approfondire...
	 */

	public DnsRequestHandler(DatagramPacket p, DatagramSocket server) {
		requestPacket = p;
		this.server = server;
	}

	public void run() {

		/*
		 * dichiaro il contenitore per la richiesta DNS.
		 */
		Message m;

		try {

			/*
			 * creo il contenitore a partire dai bytes della richiesta.
			 */
			m = new Message(requestPacket.getData());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		/*
		 * prendo il primo record nella sezione Questions.
		 */
		Record question = m.getQuestion();

		/*
		 * prendo l'hostname dal name nel record.
		 */
		Name hostname = question.getName();

		/*
		 * dichiaro il record di tipo A che conterrà l'IP risolto oltre agli
		 * altri attributi tipici di un record A.
		 */
		ARecord answer;

		/*
		 * verifico che l'hostname non sia relativo ad un hidden service.
		 */
		if (hostname.toString().matches(".*\\.onion\\.$")) {
			try {
				answer = new ARecord(hostname, 1, 86400,
						InetAddress.getByName("127.0.2.1"));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return;
			}
		} else {

			/*
			 * risolvo l'hostname in IP tramite TOR.
			 */
			InetAddress addr;
			try {
				addr = TorResolve.resolve(hostname.toString());
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			/*
			 * creo un record di tipo A per la risposta, con classe IN e TTL di
			 * un giorno.
			 */
			answer = new ARecord(hostname, 1, 86400, addr);
		}

		/*
		 * trasformo la richiesta in una risposta aggiungendo il record A nella
		 * sezione Answers.
		 */
		m.addRecord(answer, Section.ANSWER);

		/*
		 * creo il pacchetto di risposta.
		 */
		DatagramPacket responsePacket = new DatagramPacket(m.toWire(),
				m.toWire().length, requestPacket.getAddress(),
				requestPacket.getPort());

		/*
		 * invio...
		 */
		try {
			server.send(responsePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		/*
		 * stampo.
		 */
		synchronized (System.out) {
			// System.out.println(hostname.toString());
			// System.out.println(addr.getHostAddress());
			// System.out.println(answer.toString());
			System.out.println(m.toString());
		}

		// /*
		// * inizio a processare la richiesta è una prova, dovrebbe restituire
		// * sempre 1 e l'hostname da risolvere, con l'ip risolto.
		// */
		// ByteBuffer buff = ByteBuffer.wrap(requestData);
		// short questionsRRs = buff.getShort(4);
		//
		// ArrayList<Byte> arrayList = new ArrayList<Byte>();
		//
		// /*
		// * parto dal byte iniziale del campo name della sezione queries.
		// */
		// int i = 12;
		//
		// /*
		// * per la lunghezza del pacchetto...
		// */
		// while (i < requestData.length) {
		//
		// /*
		// * se il byte è nullo, l'hotname è finito ed esco.
		// */
		// if (requestData[i] == (byte) 0x00) {
		// break;
		// }
		//
		// /*
		// * prendo il byte che determina la lunghezza della parte di hostname
		// * da leggere e che viene sempre prima di quest'ultima.
		// */
		// int tempLength = (int) requestData[i];
		//
		// /*
		// * adesso parto a leggere dal successivo byte la parte di hostname
		// * attuale.
		// */
		// int j = i + 1;
		//
		// /*
		// * finchè dura...
		// */
		// while (j <= i + tempLength) {
		//
		// /*
		// * aggiungo il byte alla lista.
		// */
		// arrayList.add(requestData[j]);
		//
		// /*
		// * passo al prossimo.
		// */
		// ++j;
		// }
		//
		// /*
		// * ho finito di leggere la parte di hostname attuale, aggiungo un
		// * '.' alla lista di byte.
		// */
		// arrayList.add((byte) 0x2e);
		//
		// /*
		// * sistemo i per poter andare alla parte successiva nell'iterazione
		// * successiva.
		// */
		// i = j;
		// }
		//
		// /*
		// * mi faccio dare l'array dalla lista.
		// */
		// Object[] hostBytes = arrayList.toArray();
		//
		// /*
		// * creao un array di byte (tipo primitivo).
		// */
		// byte[] hostBytesPrimitive = new byte[hostBytes.length];
		//
		// /*
		// * converto i Byte in byte (cioè nel tipo primitivo)
		// */
		// for (i = 0; i < hostBytes.length; ++i) {
		// hostBytesPrimitive[i] = (Byte) hostBytes[i];
		// }
		//
		// /*
		// * inizializzo la stringa che conterrà l'hostname letto
		// precedentemente
		// * dalla richiesta.
		// */
		// String host = "";
		// try {
		//
		// /*
		// * creo la stringa a partire dall'array di byte.
		// */
		// host = new String(hostBytesPrimitive, "ASCII");
		// } catch (UnsupportedEncodingException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// synchronized (System.out) {
		//
		// /*
		// * stampo il contatore delle richieste di risoluzione.
		// */
		// System.out.println(questionsRRs);
		//
		// /*
		// * stampo l'hostname.
		// */
		// System.out.println(host);
		//
		// try {
		//
		// /*
		// * risolvo l'hostname in IP tramite TOR.
		// */
		// System.out.println(TorResolve.resolve(host).getHostAddress());
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

	}
}
