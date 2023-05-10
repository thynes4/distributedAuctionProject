/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Bank Socket Listener
 */
package bank;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * listens to incoming connections and adds them to a blockingqueue
 */
public class SocketListener implements Runnable {
    private boolean loop;
    private final ServerSocket serverSocket;
    private final BlockingQueue<Socket> sockets;

    /**
     * Constructor
     * @param port the port number to listen on
     * @param sockets the blockingqueue to add the sockets to
     * @throws IOException if an I/O error occurs when opening the socket
     */
    protected SocketListener(int port, BlockingQueue<Socket> sockets) throws IOException {
        loop = true;
        this.serverSocket = new ServerSocket(port);
        this.sockets = sockets;
    }
    /**
     * sets the loop flag to false. stop the thread
     */
    protected void stop() {
        loop = false;
    }

    /**
     * runs the socket listener thread, listening for incoming connections and adding
     * them to the blockingqueue
     */
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
