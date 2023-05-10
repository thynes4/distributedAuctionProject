/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Bank Main
 */
package bank;

import general.Message;
import general.Message.*;
import general.SocketData;
import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Bank {
    /**
     * Definition of the Account class with all required variables.
     */
    private static class Account {
        private String name;
        private double balance;
        private Map<String, Double> holds;
        private SocketData socketData;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Thread listenerThread;
    }

    /**
     * Definition of global variables
     */
    private final int port;
    private final int accountDifference = 4;
    private final Map<String, Account> agents;
    private final Map<String, Account> auctionHouses;
    BlockingQueue<Pair<Message,ObjectOutputStream>> messages;
    BlockingQueue<Socket> sockets;
    private Integer current = 0;

    /**
     * main method for Bank, used to initialize the port and bank
     * @param args not used
     */
    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(args[0]);
            Bank bank = new Bank(port);
            bank.start();
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number : " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Cannot start bank : " + e.getMessage());
        }
    }

    /**
     * Definition of the bank class with variables.
     * @param port number ID used to represent the port
     */
    private Bank(int port) {
        this.port = port;
        auctionHouses = new HashMap<>();
        agents = new HashMap<>();
        sockets = new LinkedBlockingQueue<>();
        messages = new LinkedBlockingQueue<>();
    }

    /**
     * Starts the parser and message threads.
     * @throws IOException in case any destination obj is not found
     */
    private void start() throws IOException {
        SocketParser socketParser = new SocketParser(this, sockets);
        SocketListener socketListener = new SocketListener(port, sockets);
        MessageParser messageParser = new MessageParser(this, messages);

        Thread sParserThread = new Thread(socketParser);
        Thread sListenerThread = new Thread(socketListener);
        Thread mParserThread = new Thread(messageParser);

        sParserThread.start();
        mParserThread.start();
        sListenerThread.start();
    }

    /**
     * Gathers and sends the list of active Auction Houses to each available Agent.
     */
    protected void sendAuctionHouseList() {
        synchronized (auctionHouses) {
            Map<String, SocketData> auctionHouseData = new HashMap<>();
            for (String accNum : auctionHouses.keySet()) {
                Account ah = auctionHouses.get(accNum);
                auctionHouseData.put(ah.name, ah.socketData);
            }
            if(!agents.isEmpty()) {
                for (String accNum : agents.keySet()) {
                    Account account = agents.get(accNum);
                    try {
                        account.out.writeObject(new AuctionList(auctionHouseData));
                    } catch (IOException e) {
                        System.out.println("Cannot send auctionHouse data: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * gathers the list of holds for given Agent
     * gathers the Agent's current balance, sends both to Agent
     * @param accNum string used to represent target Agent
     */
    protected void sendAgentBalance(String accNum) {
        Account account = agents.get(accNum);
        try {
            synchronized (agents.get(accNum)) {
                double holds = 0;
                for (String s : account.holds.keySet()) {
                    holds = holds + account.holds.get(s);
                }
                account.out.writeObject(new UpdateMoney(account.balance, holds));
            }
        } catch (IOException e) {
            System.out.println("Error in writing balance to client: " + e.getMessage());
        }
    }

    /**
     * pulls and updates the auction house's balance
     * @param accountNum string used to represent target Agent
     */
    protected void sendAHBalance(String accountNum) {
        Account account = auctionHouses.get(accountNum);
        try {
            synchronized (auctionHouses.get(accountNum)) {
                account.out.writeObject(new UpdateMoney(account.balance, 0));
            }
        } catch (IOException e) {
            System.out.println("Cannot write balance to auction house: " + e.getMessage());
        }
    }

    /**
     * Method to close the Auction House and remove the current hold on the wining Agent
     * @param auctionHouseAccount Auction House balance before close
     * @param agentAccount target Agent
     * @param item hold to be removed from the Agent
     * @param amount to be added to the Auction House's balance after close
     */
    protected void auctionEnded(String auctionHouseAccount, String agentAccount, String item, double amount) {
        removeHold(agentAccount, item);
        auctionHouses.get(auctionHouseAccount).balance += amount;
        agents.get(agentAccount).balance -= amount;
    }

    /**
     * Removes the specified hold from the given Agent
     * @param accountNum string used to ID the specific Agent
     * @param item string used to ID the hold to be removed
     */
    protected void removeHold(String accountNum, String item) {
        Account account = agents.get(accountNum);
        synchronized (agents.get(accountNum)) {
            account.holds.remove(item);
        }
    }

    /**
     * Method to add a hold to an Agent's current holds
     * @param newHold type NewHold to be added
     * @return returns true if Agent can support current holds, false otherwise
     */
    protected boolean addNewHold(NewHold newHold) {
        if (agents.containsKey(newHold.accountNumber())) {
            Account account = agents.get(newHold.accountNumber());
            synchronized (agents.get(newHold.accountNumber())) {
                account.holds.remove(newHold.accAndID());
                double holds = 0;
                for (String s : account.holds.keySet()) {
                    holds = account.holds.get(s) + holds;
                }
                if ((holds + newHold.amount()) > account.balance) {
                    System.out.println("Could not apply hold to account #" + newHold.accountNumber() + ": Insufficient funds.");
                    return false;
                }
                account.holds.put(newHold.accAndID(), newHold.amount());
            }
            System.out.println("Hold for $" + newHold.accAndID() + " to account #" + newHold.accountNumber());
            return true;
        } else {
            System.out.println("Error: Could not apply hold to account #" + newHold.accountNumber() + ": Account not found.");
            return false;
        }
    }

    /**
     * kills the thread currently running the given Auction House
     * @param accNum string to ID given Auction House
     */
    protected void closeAuctionHouseAcc(String accNum) {
        synchronized (auctionHouses) {
            Account account = auctionHouses.remove(accNum);
            if (account != null) {
                account.listenerThread.interrupt();
                System.out.println("Removed auction house \"" + account.name + "\"");
            }
        }
        sendAuctionHouseList();
    }

    /**
     * Creates and adds a new Agent to the list of Auction Houses and the Bank
     * @param msg NewAgent type used to name the account
     * @param input input stream
     * @param output output stream
     * @throws IOException in case the Bank or an Auction House object is not found
     */
    protected void addAgent (NewAgent msg, ObjectInputStream input, ObjectOutputStream output)
            throws IOException {
        String accNum;

        synchronized (current) {
            current += accountDifference;
            accNum = "CL" + String.format("%04d", current);
        }

        Account account = new Account();
        account.name = msg.name();
        account.balance = msg.startBal();
        account.holds = new HashMap<>();
        account.in = input;
        account.out = output;

        synchronized (agents) {agents.put(accNum, account);}

        System.out.println("Added new client \"" + account.name +
                "\", #" + accNum);

        account.listenerThread = new Thread(new MessageListener(messages, account.in, account.out));
        account.listenerThread.start();

        output.writeObject(new AgentMade(accNum));
        sendAgentBalance(accNum);

        synchronized (auctionHouses) {
            if (!auctionHouses.isEmpty()) {
                Map<String, SocketData> ahList = new HashMap<>();
                for (String s : auctionHouses.keySet()) {
                    ahList.put(auctionHouses.get(s).name,
                            auctionHouses.get(s).socketData);
                }
                output.writeObject(new AuctionList(ahList));
            }
        }
    }

    /**
     * Method to initialize a new Auction House and add it to the current active list
     * @param socket socket
     * @param msg NewAuctionHouse type message
     * @param in input stream
     * @param out output stream
     * @throws IOException in case listener thread is not found
     */
    protected void addAH(Socket socket, NewAuctionHouse msg, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        String accNum;

        synchronized (current) {
            current += accountDifference;
            accNum = "AH" + String.format("%04d", current);
        }

        Account account = new Account();
        account.name = msg.name();
        account.balance = 0;
        account.holds = null;
        account.socketData = new SocketData(socket.getInetAddress().getHostName(), msg.port());
        account.in = in;
        account.out = out;

        synchronized (auctionHouses) {auctionHouses.put(accNum, account);}

        System.out.println("Added new auction house \"" + account.name +
                "\", #" + accNum);

        account.listenerThread = new Thread(new MessageListener(messages, account.in, account.out));
        account.listenerThread.start();

        out.writeObject(new AuctionHouseMade(accNum));
        sendAuctionHouseList();
    }

    /**
     * kills the specified Agent
     * @param accNum string to identify desired Agent
     */
    protected void closeAgentAccount(String accNum) {
        synchronized (agents) {
            Account account = agents.remove(accNum);
            System.out.println("Removed agent account for \"" + account.name + "\"");
            account.listenerThread.interrupt();
        }
    }
}
