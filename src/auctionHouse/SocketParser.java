package auctionHouse;

import general.Message;
import general.Message.*;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SocketParser implements Runnable {
    private boolean loop;
    private final AuctionHouse auctionHouse;
    private final BlockingQueue<Socket> sockets;

    protected SocketParser(AuctionHouse ahObj, BlockingQueue<Socket> sockObj) {
        this.auctionHouse = ahObj;
        this.sockets = sockObj;
        loop = true;
    }

    public void stop () {
        loop = false;
    }
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

