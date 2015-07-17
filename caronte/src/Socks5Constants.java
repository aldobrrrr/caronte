/**
 * Comprende tutte le costanti del protocollo SOCKS5 come da RFC.
 * 
 * @see <a href="https://www.ietf.org/rfc/rfc1928.txt">IETF rfc1928</a>
 * 
 * @author Emanuele Altomare
 */
public interface Socks5Constants {
	/**
	 * Comandi utilizzabili nella richiesta.
	 */
	public final static byte CMD_CONNECT = (byte) 0x01;
	public final static byte CMD_BIND = (byte) 0x02;

	public final static byte CMD_UDP_ASSOCIATE = (byte) 0x03;

	/**
	 * Campo reserved nella richiesta, DEVE essere impostato a 0x00.
	 */
	public final static byte RSV = (byte) 0x00;

	/**
	 * Tipo di indirizzo specificato nella richiesta.
	 */
	public final static byte ATYP_IPV4 = (byte) 0x01;
	public final static byte ATYP_DOMAINNAME = (byte) 0x03;
	public final static byte ATYP_IPV6 = (byte) 0x04;

	/**
	 * Costanti per la parte di autenticazione.
	 */
	public final static byte AUTH_NO_AUTHENTICATION_REQUIRED = (byte) 0x00;
	public final static byte AUTH_GSSAPI = (byte) 0x01;
	public final static byte AUTH_USERNAME_PASSWORD = (byte) 0x02;
	public final static byte AUTH_TO_0X7F_IANA_ASSIGNED = (byte) 0x03;
	public final static byte AUTH_TO_0XFE_RESERVED_FOR_PRIVATE_METHODS = (byte) 0x80;
	public final static byte AUTH_NO_ACCEPTABLE_METHODS = (byte) 0xff;

	/**
	 * Costanti per lo stato nella risposta del server.
	 */
	public final static byte STATUS_SUCCEEDED = (byte) 0x00;
	public final static byte STATUS_GENERAL_SOCKS_SERVER_FAILURE = (byte) 0x01;
	public final static byte STATUS_CONNECTION_NOT_ALLOWED_BY_RULESET = (byte) 0x02;
	public final static byte STATUS_NETWORK_UNREACHABLE = (byte) 0x03;
	public final static byte STATUS_HOST_UNREACHABLE = (byte) 0x04;
	public final static byte STATUS_CONNECTION_REFUSED = (byte) 0x05;
	public final static byte STATUS_TTL_EXPIRED = (byte) 0x06;
	public final static byte STATUS_COMMAND_NOT_SUPPORTED = (byte) 0x07;
	public final static byte STATUS_ADDRESS_TYPE_NOT_SUPPORTED = (byte) 0x08;
	public final static byte STATUS_TO_0XFF_UNASSIGNED = (byte) 0x09;
}
