package cwrapper;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class CWrapperProxy {
	public static void main(String[] args) throws IOException {
		@SuppressWarnings("resource")
		ServerSocket ss = new ServerSocket(4567);

		final byte[] request = new byte[4096];
		byte[] reply = new byte[4096];

		System.out.println("Started...");
		while (true) {
			Socket client = null;
			Socket server = null;
			try {
				client = ss.accept();
				System.out.println("Accept....");

				final InputStream from_client = client.getInputStream();
				final OutputStream to_client = client.getOutputStream();
				to_client.flush();

				final DataInputStream dataIn = new DataInputStream(from_client);
				short sa_family = dataIn.readShort();
				int ip = dataIn.readInt();
				short port = dataIn.readShort();

				byte[] bytes = BigInteger.valueOf(ip).toByteArray();
				InetAddress address = InetAddress.getByAddress(bytes);
				System.out.println("Sa_familty:" + sa_family);
				System.out.println(address.getHostAddress());
				System.out.println(port);

				try {
					server = new Socket(address, port);
				} catch (IOException e) {
					server.close();
					continue;
				}

				// Get server streams.
				final InputStream from_server = server.getInputStream();
				final OutputStream to_server = server.getOutputStream();

				// Make a thread to read the client's requests and pass them to
				// the
				// server. We have to use a separate thread because requests and
				// responses may be asynchronous.
				Thread t = new Thread() {
					public void run() {
						int bytes_read;
						try {
							while ((bytes_read = from_client.read(request)) != -1) {
								to_server.write(request, 0, bytes_read);
								to_server.flush();
							}
						} catch (IOException e) {
						}

						// the client closed the connection to us, so close our
						// connection to the server. This will also cause the
						// server-to-client loop in the main thread exit.
						try {
							to_server.close();
						} catch (IOException e) {
						}
					}
				};

				// Start the client-to-server request thread running
				t.start();

				// Meanwhile, in the main thread, read the server's responses
				// and pass them back to the client. This will be done in
				// parallel with the client-to-server request thread above.
				int bytes_read;
				try {
					while ((bytes_read = from_server.read(reply)) != -1) {
						to_client.write(reply, 0, bytes_read);
						to_client.flush();
					}
				} catch (IOException e) {
				}

				// The server closed its connection to us, so close our
				// connection to our client. This will make the other thread
				// exit.
				to_client.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (server != null)
						server.close();
					if (client != null)
						client.close();
				} catch (IOException e) {
				}
			}
		}
	}
}