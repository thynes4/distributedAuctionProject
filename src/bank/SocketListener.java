package bank;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class SocketListener implements Runnable {
    private boolean loop;
    private final ServerSocket serverSocket;
    private final BlockingQueue<Socket> sockets;

    protected SocketListener(int port, BlockingQueue<Socket> sockets) throws IOException {
        loop = true;
        this.serverSocket = new ServerSocket(port);
        this.sockets = sockets;
    }

    protected void stop() {
        loop = false;
    }
    @Override
    public void run() {
        System.out.println("Socket Listener on port: " + serverSocket.getLocalPort());
        while (loop) {
            try {
                sockets.put(serverSocket.accept());
            } catch (InterruptedException e) {
                System.out.println("Error: Socket listener interrupted adding to queue: " +
                        e.getMessage());
            } catch (IOException e) {
                System.out.println("Error: failure in receiving socket: " +
                        e.getMessage());
            }
        }
    }
}
