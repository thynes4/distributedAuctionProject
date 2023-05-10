package auctionHouse;

import general.Message;
import general.Message.*;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * reads messages from the sockets and calls the appropriate method in the bank class based
 * on the type of message received.
 */
public class SocketParser implements Runnable {
    private boolean loop;
    private final AuctionHouse auctionHouse;
    private final BlockingQueue<Socket> sockets;

    /**
     * Constructor
     * @param ahObj the auction house object we're parsing sockets for
     * @param sockObj a blockingqueue of sockets to read messages from
     */
    protected SocketParser(AuctionHouse ahObj, BlockingQueue<Socket> sockObj) {
        this.auctionHouse = ahObj;
        this.sockets = sockObj;
        loop = true;
    }

    /**
     * sets the loop flag to false to stop the thread
     */
    public void stop () {
        loop = false;
    }

    /**
     * Run method overrides the runnable interface and is executed when the thread is started.
     * Reads messages from the sockets and processes them accordingly
     */
    @Override
    public void run() {
        while (loop) {
            try {
                Socket socket = sockets.take();
                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) in.readObject();
                if (msg instanceof RegisterAgent newAgent) {
                    auctionHouse.addNewAgent(newAgent, out, in);
                } else {
                    System.out.println("Invalid message from new socket.");
                }
            } catch (InterruptedException e) {
                System.out.println("Reader interrupted waiting for socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error reading from/writing to IOStream: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("Unrecognized class object received: " + e.getMessage());
            }
        }
    }
}

