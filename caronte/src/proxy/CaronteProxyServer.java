package proxy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Questa classe consente la creazione e l'avvio di Caronte.
 * 
 * @author Emanuele Altomare
 */
public class CaronteProxyServer {
	/**
	 * Questo main prende il parametro di input e lo passa a runServer.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int localport = (args.length != 0 && args[0] != null && !args[0]
				.isEmpty()) ? Integer.parseInt(args[0]) : 8080;
		/*
		 * avvio il server.
		 */
		System.out.println("Starting proxy on port " + localport);
		runServer(localport);
	}

	/**
	 * Questo metodo crea il server che lancia un thread per gestire ogni
	 * connessione in entrata. Non termina mai.
	 * 
	 * @param localport
	 * @throws IOException
	 */
	public static void runServer(int localport) throws IOException {
		/*
		 * crea la socket sulla quale mettersi in ascolto.
		 */
		@SuppressWarnings("resource")
		ServerSocket ss = new ServerSocket(localport);

		while (true) {
			try {
				/*
				 * attende una connessione...
				 */
				Socket client = ss.accept();
				// System.out.println("Accepted connection...");

				/*
				 * crea il thread consegnandogli il contesto del client e lo fa
				 * partire.
				 */
				ConnectionHandler connHandler = new ConnectionHandler(client);
				connHandler.start();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}