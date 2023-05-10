package bank;

import general.Message;
import general.Message.*;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SocketParser implements Runnable {
    private boolean loop;
    private final Bank bank;
    private final BlockingQueue<Socket> sockets;

    protected SocketParser(Bank bankObj, BlockingQueue<Socket> sockObj) {
        this.bank = bankObj;
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
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Message m = (Message) in.readObject();
                if (m instanceof NewAgent newAgent) {
                    bank.addAgent(newAgent, in, out);
                } else if (m instanceof NewAuctionHouse newAuctionHouse) {
                    bank.addAH(socket, newAuctionHouse, in, out);
                } else {
                    System.out.println("Invalid message from new socket");
                }
            } catch (InterruptedException e) {
                System.out.println("Reader interrupted waiting for socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error reading / writing to IOStream: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("Unrecognized class object received: " + e.getMessage());
            }
        }
    }
}
