/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Auction House Main
 */
package auctionHouse;
import general.AuctionData;
import general.Message;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class AuctionHouse extends Application{
    private Connection bank; // the connection to the bank
    private Map<String, Connection> agents; // list of connected agents
    private BlockingQueue<Pair<Message, ObjectOutputStream>> messages; // messages
    private final Auction[] auctions = new Auction[3]; // list of starting auctions
    private String accountNumber; // account number
    private String name; // the agent's name
    private int port; // bank port number
    private double balance; // Auction house's bank account balance
    private int auctionNum = 0; //Auction ID number
    private final long FREQ = 2_000_000_000; //Set to 2,000,000,000 (2 seconds)
    private boolean newAuctions; // are there new auctions available?

    private final NumberFormat USD = NumberFormat.getCurrencyInstance(
            new Locale("en", "US"));

    private SocketListener sl; // socket listener
    private SocketParser ps; // socket parser
    private Thread sockListenThread;
    private Thread parseSockThread;
    private BlockingQueue<Socket> sockets;


    private final String[] itemColors = {"red", "orange", "yellow", "green",
            "blue", "purple", "black", "white"};
    private final String[] items = {"chair", "couch", "ottoman", "stool",
            "desk", "bed", "dresser", "rug", "table", "recliner", "bench"};

    /**
     * Confirms a hold on an auction bid.
     *
     * @param message Confirmation of valid hold from bank.
     */
    protected void bidConfirmation(Message.ConfirmHold message) {
        try {
            Auction auction = createNewAuction(message.id());
            if (message.success()) {
                auction.confirmBid(message);
            }
            sendAuctionInfo();
        } catch (IllegalAccessException e) {
            System.out.println("Error: Auction not found, hold confirmed:" +
                    e.getMessage());
        }
    }

    /**
     * Initiates process of verifying a bid.
     * @param msg Bid message from client.
     */
    protected void bidReceived(Message.NewBid msg) {
        try {
            Auction auction = createNewAuction(msg.id());
            auction.newBid(msg);
        } catch (IllegalAccessException e) {
            System.out.println("Error: " + e.getMessage());
            try {
                agents.get(msg.accNum()).output.writeObject(
                        new Message.ConfirmBid(false, msg.item(), name));
            } catch (IOException err) {
                System.out.println("Error writing failed bid: " +
                        err.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Error sending message to bank/client: " +
                    e.getMessage());
        }
    }


    /**
     * Returns the auction based on its id.
     * @param id Unique id of an auction.
     * @return Auction object associated with id.
     * @throws IllegalAccessException Error if auction no longer open.
     */
    private Auction createNewAuction(int id) throws IllegalAccessException {
        for (int i = 0; i < 3; i++) {
            if (auctions[i].id == id) return auctions[i];
        }
        throw new IllegalAccessException("Auction not found in current auctions.");
    }

    /**
     * Updates the auction house's bank account AHBalance
     * @param AHBalance updated AHBalance
     */
    protected void updateAHBankAccount(double AHBalance) {
        this.balance = AHBalance;
    }

    /**
     * Updates an accountNumber
     * @param accountNumber account number
     */
    protected void updateAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**
     * Disconnects an agent from the Auction House & prints a confirmation that the agent was disconnected
     * @param accountNumber the agent's account number
     */
    protected void disconnectAgent(String accountNumber) {
        Connection connection = agents.remove(accountNumber);
        if (connection != null) {
            System.out.println("Disconnected the agent named: " + connection.name + " from the Auction House");
            connection.listener.stop();
            connection.thread.interrupt();
        }
    }

    /**
     * Connects a new Agent to the auction house & then sends the agent the current auction info
     * @param message Message with agent's information.
     * @param out sends messages to the agent
     * @param in receives messages from the agent
     */
    protected void addNewAgent(Message.RegisterAgent message, ObjectOutputStream out,
                               ObjectInputStream in) {
        Connection agent = new Connection();
        agent.name = message.name();
        agent.input = in;
        agent.output = out;
        agent.listener = new MessageListener(messages, in, out);
        agent.thread = new Thread(agent.listener);

        agents.put(message.accNum(), agent);
        System.out.println("Added a new agent named:  \"" + agent.name + "\"" + " to the Auction House");

        agent.thread.start();
        sendAuctionInfo();
    }

    /**
     * Sends information on current auctions to each agent that's connected to the auction house
     */
    private void sendAuctionInfo() {
        synchronized (auctions) {
            if (!agents.isEmpty()) {
                ArrayList<AuctionData> list = new ArrayList<>();

                for (Auction auction : auctions) {
                    if (auction != null) {
                        list.add(new AuctionData(auction.item, auction.id,
                                auction.currentBid, auction.leadingBidder));
                    }
                }

                for (String agent : agents.keySet()) {
                    ObjectOutputStream out = agents.get(agent).output;

                    try {
                        out.writeObject(new Message.NewAuctions(name, list));
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    /**
     * Checks if any auctions have expired and need to be replaced.
     * Replace any expired auctions with new ones, or null if no available auctions exist.
     * Checks to see if any auctions have been expired. If the auctions list has been updated, send the updated
     * auctions information over to each connected Agent
     */
    private void checkAndUpdateAuctions() {
        boolean auctionsUpdated = false;
        for (int i = 0; i < 3; i++) {
            if ((auctions[i] != null) && (auctions[i].expired)) {
                auctionsUpdated = true;
                if (!auctions[i].leadingBidder.equals("no bidder")) {
                    auctions[i].auctionWon();
                }
                if (newAuctions) {
                    auctions[i] = createNewAuction();
                } else {
                    auctions[i] = null;
                }
            }
        }
        if (auctionsUpdated) sendAuctionInfo();
    }

    /**
     * Creates a new auction with a unique auction ID number given an item that is to be up for bid.
     * Each item is randomly chosen from the list of possible items and is randomly assigned a color
     * @return the new Auction item that is to be added to our list of all auctions
     */
    private Auction createNewAuction() {
        String item = itemColors[(int)(Math.random() * itemColors.length)] + " " +
                items[(int)(Math.random() * items.length)];
        return new Auction(item, ++auctionNum);
    }

    /**
     * Generates 3 random auctions for the starting options.
     */
    private void startingAuctions() {
        for (int i = 0; i < 3; i++) {auctions[i] = createNewAuction();}
    }

    /**
     * Main used to launch jfx Agent
     * @param args unused
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Auction House initialization
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     */
    @Override
    public void start(Stage primaryStage){
        /**
         * Initializing jfx objects
         */
        GridPane root = new GridPane();
        TextField name = new TextField();
        TextField localPort = new TextField();
        TextField bankName = new TextField();
        TextField bankPort = new TextField();
        Button start = new Button("Start");


        /**
         * start logic goes here
         */
        start.setOnAction(event -> {});


        /**
         * Formatting and such
         */
        int row = 0;
        root.addRow(row, new Label("Name: "), name);
        root.addRow(++row, new Label("Local Port: "), localPort);
        root.addRow(++row, new Label("Bank Name: "), bankName);
        root.addRow(++row, new Label("Bank Port: "), bankPort);
        root.add(start, 1, ++row);

        root.setPadding(new Insets(10));
        root.setVgap(10);
        root.setHgap(10);

        primaryStage.setTitle("Auction House");
        primaryStage.setScene(new Scene(root));
        root.getStylesheets().add("style.css");
        primaryStage.show();
    }

    private class Connection{
        private Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private String name;
        private Thread thread;
        private MessageListener listener;
    }

    private class Auction {
        private final String item;
        private final int id;
        private double currentBid;
        private String leadingBidder;
        private String bidderAcct;
        private final List<Message.NewBid> pendingBids;

        private Timer timer;
        private TimerTask task;
        private boolean expired;
        private final long DELAY = 30_000L; //30s delay for timer

        /**
         * Generates string used in hold messages to bank.
         * @param id Auction id.
         * @return Combination of acctNum and id.
         */
        private String holdStr(int id) {
            return accountNumber + ":" + id;
        }

        /**
         * Sends relevant messages when this auction is won by a bidder.
         */
        private void auctionWon() {
            try {
                Connection c = agents.get(bidderAcct);
                bank.output.writeObject(new Message.AuctionOver(
                        accountNumber, bidderAcct, holdStr(id), currentBid));
                c.output.writeObject(new Message.AuctionWon(item, currentBid));
            } catch (IOException e) {
                System.out.println("Error writing auction over messages: " +
                        e.getMessage());
            }
        }

        /**
         * Updates high bidder on auction following verification of funds.
         * @param msg Message from bank indicating hold is valid.
         */
        private void confirmBid(Message.ConfirmHold msg) {
            expired = false;
            resetTimer();
            try {
                Message.NewBid bid = findBid(msg);
                pendingBids.remove(bid);
                Connection c = agents.get(bid.accNum());

                if (!leadingBidder.equals("no bidder") &&
                        (!bidderAcct.equals(msg.accNum()))) {
                    bank.output.writeObject(new Message.EndHold(
                            bidderAcct, currentBid, holdStr(id)));
                }

                if (bid.bid() > currentBid) {
                    currentBid = bid.bid();
                    leadingBidder = c.name;
                    bidderAcct = bid.accNum();

                    c.output.writeObject(new Message.ConfirmBid(true, item, name));
                    System.out.println("New bid on " + item +
                            " accepted from " + leadingBidder);
                }
                else {
                    c.output.writeObject(new Message.ConfirmBid(false, item, name));
                    bank.output.writeObject(new Message.EndHold(
                            bid.accNum(), bid.bid(), holdStr(bid.id())));
                    System.out.println("New bid on " + item +
                            " rejected from " + c.name);
                }
            } catch (IllegalAccessException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error writing bid confirmation/end hold: "+
                        e.getMessage());
            }
        }

        /**
         * Recalls a previous pending bid.
         * @param msg Confirmation of valid hold message from bank.
         * @return Initial bid message from client.
         * @throws IllegalAccessException Error if old bid does not exist.
         */
        private Message.NewBid findBid(Message.ConfirmHold msg) throws IllegalAccessException {
            for (Message.NewBid newBid : pendingBids) {
                if ((msg.id() == newBid.id()) &&
                        (msg.accNum().equals(newBid.accNum()))) {
                    return newBid;
                }
            }
            throw new IllegalAccessException(
                    "Bid not found after confirmation.");
        }

        /**
         * Initiates check with bank to see if a bid is valid.
         * @param msg Bid message from client.
         * @throws IOException Error if unable to request hold with bank.
         */
        private void newBid(Message.NewBid msg) throws IOException {
            resetTimer();
            pendingBids.add(msg);
            System.out.println("New bid on " + item +
                    " received from " + agents.get(msg.accNum()).name);
            if (msg.bid() > currentBid) {
                bank.output.writeObject(new Message.NewHold(
                        msg.accNum(), msg.bid(), holdStr(id), id));
            } else {
                agents.get(msg.accNum()).output.writeObject(
                        new Message.ConfirmBid(false, item, name));
                System.out.println("New bid on " + item +
                        " rejected from " + agents.get(msg.accNum()).name);
            }
        }

        /**
         * Resets the 30s timer on an auction
         */
        private void resetTimer() {
            timer.cancel();
            timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {expired = true;}
            };
            timer.schedule(task, DELAY);
        }

        /**
         * Constructor for an auction item.
         * @param item Name of item to be sold.
         * @param auctionID Unique auctionID of auction at this auction house.
         */
        private Auction(String item, int auctionID) {
            this.item = item;
            this.id = auctionID;
            currentBid = 20; // All auctions start at $20
            leadingBidder = "no bidder";
            expired = false;
            pendingBids = new ArrayList<>();

            timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {expired = true;}
            };

            timer.schedule(task, DELAY);
        }
    }
}
