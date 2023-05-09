package auctionHouse;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;

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



        }
    }
}

