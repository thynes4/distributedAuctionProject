/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * SocketData Record
 */
package general;

import java.io.Serializable;

/**
 * Record class that holds data for a socket. Super simple. Holds the hostname and port number
 */
public record SocketData(String hostname, int port) implements Serializable {}
