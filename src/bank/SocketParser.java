package bank;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;

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



        }
    }
}
