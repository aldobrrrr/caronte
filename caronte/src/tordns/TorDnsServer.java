package tordns;

/**
 * TorDNS Standalone.
 * 
 * @author andrea
 *
 */
public class TorDnsServer {
	final private TorDns torDns;

	/**
	 * Costruttore.
	 */
	public TorDnsServer(final String boundIp, final int boundPort) {
		/*
		 * Creo il server e lo avvio
		 */
		torDns = new TorDns(boundIp, boundPort);
		torDns.start();
		System.out.println("TorDnsServer is started...");
	}

	/**
	 * Entry Point.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		new TorDnsServer("127.0.1.1", 5553);
	}
}
