package general;

import java.io.Serializable;

public record SocketData(String hostname, int port) implements Serializable {}
