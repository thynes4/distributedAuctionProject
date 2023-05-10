/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Bank Main
 */
package bank;

import general.Message;
import general.SocketData;
import javafx.util.Pair;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Bank {

    private class Account {
        private String name;
        private double balance;
        private Map<String, Double> holds; //Only used by client
        private SocketData socketData; //Only used by auction house
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Thread listener;
    }

    private final int port;
    private final Map<String, Account> clients;
    private final Map<String, Account> auctionHouses;
    BlockingQueue<Socket> sockets;
    BlockingQueue<Pair<Message, ObjectOutputStream>> messages;

    /**
     * Constructor for Bank.
     * @param port Port for bank to listen on.
     */
    private Bank (int port) {
        this.port = port;
        clients = new HashMap<>();
        auctionHouses = new HashMap<>();
        sockets = new LinkedBlockingQueue<>();
        messages = new LinkedBlockingQueue<>();
    }


}
