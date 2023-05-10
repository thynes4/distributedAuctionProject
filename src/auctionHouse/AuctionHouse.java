/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Auction House Main
 */
package auctionHouse;
import general.AuctionData;
import general.Message;
import general.Message.*;
import javafx.animation.AnimationTimer;
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
import java.util.concurrent.LinkedBlockingQueue;

public class AuctionHouse extends Application{
    private Connection bank; // the connection to the bank
    private Map<String, Connection> agents; // list of connected agents
    private BlockingQueue<Pair<Message, ObjectOutputStream>> messages; // messages
    private final Auction[] auctions = new Auction[3]; // list of starting auctions
    private String accountNumber; // account number
    private String name; // the agent's name
    private int port;
    private double aHBalance; // Auction house's bank account balance
    private int auctionNum = 0; //Auction ID number
    private boolean newAuctions; // are there new auctions available?
    private final long FREQ = 2_000_000_000; //two seconds


    private Stage primaryStage;
    private Label balLabel;
    private AnimationTimer accWait;
    private final NumberFormat USD = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private SocketListener sl;
    private SocketParser ps;
    private Thread sockListenThread;
    private Thread parseSockThread;
    private GridPane auctionPane;
    private BlockingQueue<Socket> sockets;

    private final String[] itemColors = {"red", "orange", "yellow", "green",
            "blue", "purple", "black", "white"};
    private final String[] items = {"chair", "couch", "ottoman", "stool",
            "desk", "bed", "dresser", "rug", "table", "recliner", "bench"};

    /**
     * Confirms whether there are funds in an agent's account that are being held for a pending bid
     *
     * @param message message from the bank confirming that the funds are being held
     */
    protected void bidConfirmation(ConfirmHold message) {
        try {
            Auction auction = grabAuction(message.id()); // find the specified auction
            if (message.success()) {
                auction.confirmBid(message); // bid is confirmed & the agent's funds are held
            }
            sendAuctionInfo(); // send the agent the updated auction info
        } catch (IllegalAccessException e) {
            System.out.println("Error: Auction not found" + e.getMessage());
        }
    }

    /**
     * handles a new bid message that has been received
     * @param message message from the agent containing the bid's information
     */
    protected void bidReceived(NewBid message) {
        try {
            // find the auction & check to see if the placed bid is valid.
            Auction auction = grabAuction(message.id());
            auction.checkValidBid(message);
        } catch (IllegalAccessException ignored) {
            try {
                // if the auction cannot be found, try to send a message to the agent to
                // indicate that the bid was unsuccessful
                agents.get(message.accountNumber()).output.writeObject(
                        new ConfirmBid(false, message.item(), name));
            } catch (IOException ignored2) {
            }
        } catch (IOException ignored) {
        }
    }


    /**
     * grabs an auction given its ID number
     * @param id Unique id of an auction.
     * @return Auction object associated with id.
     * @throws IllegalAccessException Error if auction no longer open.
     */
    private Auction grabAuction(int id) throws IllegalAccessException {
        for (int i = 0; i < 3; i++) {
            // if the auction is found, return it
            if (auctions[i].auctionID == id) return auctions[i];
        }
        // if the auction cannot be found, throw an exception and write a message to the console
        throw new IllegalAccessException("That auction doesn't exist");
    }

    /**
     * Updates the auction house's bank account AHBalance
     * @param aHBalance updated AHBalance
     */
    protected void updateAHBankAccount(double aHBalance) {
        this.aHBalance = aHBalance;
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
        Connection agentConnection = agents.remove(accountNumber); // disconnect the agent from the auction house

        // print a message to the console indicating that the agent was disconnected
        if (agentConnection != null) {
            System.out.println("Disconnected the agent named: " + agentConnection.name + " from the Auction House");
            agentConnection.listener.stop();
            agentConnection.thread.interrupt();
        }
    }

    /**
     * Connects a new Agent to the auction house & then sends the agent the current auction info
     * @param message Message with agent's information.
     * @param out sends messages to the agent
     * @param in receives messages from the agent
     */
    protected void addNewAgent(Message.RegisterAgent message, ObjectOutputStream out, ObjectInputStream in) {
        Connection agent = new Connection(); // the agent's connection
        agent.name = message.name(); // the agent's name
        agent.input = in; // the agent's input stream
        agent.output = out; // the agent's output stream
        agent.listener = new MessageListener(messages, in, out); // listens for messages
        agent.thread = new Thread(agent.listener);

        agents.put(message.accountNumber(), agent); // add the agent to our map of connected agents
        System.out.println("Added a new agent named:  \"" + agent.name + "\"" + " to the Auction House");
        agent.thread.start();
        sendAuctionInfo(); // send the auction info to the agent
    }

    /**
     * Sends information on current auctions to each agent that's connected to the auction house
     */
    private void sendAuctionInfo() {
        synchronized (auctions) { // make sure auctions can only be access by one thread at a time

            if (!agents.isEmpty()) {
                ArrayList<AuctionData> list = new ArrayList<>();

                // collect the data for each auction & store it in our ArrayList called list
                for (Auction auction : auctions) {
                    if (auction != null) {
                        list.add(new AuctionData(auction.item, auction.auctionID,
                                auction.winningBid, auction.winningAgent));
                    }
                }

                // then, for each agent that's connected to the auction house,
                // send over that list of auction data
                for (String agent : agents.keySet()) {
                    ObjectOutputStream out = agents.get(agent).output;

                    try {
                        out.writeObject(new NewAuctions(name, list));
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
                if (!auctions[i].winningAgent.equals("no bidder")) {
                    auctions[i].auctionWon();
                }
                if (newAuctions) {
                    auctions[i] = grabAuction();
                } else {
                    auctions[i] = null;
                }
            }
        }
        if (auctionsUpdated) sendAuctionInfo();
    }

    private GridPane showAuction(Auction auction) {
        GridPane gp = new GridPane();
        int row = 0;

        gp.setPadding(new Insets(10));
        gp.setHgap(5);
        gp.setVgap(5);

        gp.add(new Label("ID: " + auction.auctionID), 0, row);
        gp.add(new Label("Item: " + auction.item), 1, row);
        gp.add(new Label("Price: " + USD.format(auction.winningBid)),
                0, ++row);
        gp.add(new Label("Current high bidder: " + auction.winningAgent),
                0, ++row, 2, 1);

        return gp;
    }

    /**
     * Creates a new auction with a unique auction ID number given an item that is to be up for bid.
     * Each item is randomly chosen from the list of possible items and is randomly assigned a color
     * @return the new Auction item that is to be added to our list of all auctions
     */
    private Auction grabAuction() {
        String item = itemColors[(int)(Math.random() * itemColors.length)] + " " +
                items[(int)(Math.random() * items.length)];
        return new Auction(item, ++auctionNum);
    }

    /**
     * Generates 3 random auctions for the starting options.
     */
    private void startingAuctions() {
        for (int i = 0; i < 3; i++) {auctions[i] = grabAuction();}
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
    public void start(Stage primaryStage) {
        accountNumber = "none";
        messages = new LinkedBlockingQueue<>();
        agents = Collections.synchronizedMap(new HashMap<>());
        sockets = new LinkedBlockingQueue<>();
        this.primaryStage = primaryStage;


         //Initializing jfx objects

        GridPane root = new GridPane();
        TextField nameInput = new TextField();
        TextField localPort = new TextField();
        TextField bankName = new TextField();
        TextField bankPort = new TextField();
        Button start = new Button("Start");

         //start logic goes here

        start.setOnAction(event -> {
            try {
                newAuctions = true;
                auctionPane = new GridPane();
                startingAuctions();
                port = Integer.parseInt(localPort.getText());
                bank = new Connection();
                bank.socket = new Socket(bankName.getText(), Integer.parseInt(bankPort.getText()));
                bank.output = new ObjectOutputStream(bank.socket.getOutputStream());
                bank.output.flush();
                bank.input = new ObjectInputStream(bank.socket.getInputStream());
                bank.listener = new MessageListener(messages, bank.input, bank.output);
                bank.thread = new Thread(bank.listener);
                bank.thread.start();

                sl = new SocketListener(port, sockets);
                ps = new SocketParser(this, sockets);
                sockListenThread = new Thread(sl);
                parseSockThread = new Thread(ps);
                sockListenThread.start();
                parseSockThread.start();

                MessageParser pm = new MessageParser(this, messages);
                Thread t = new Thread(pm);
                t.start();

                name = nameInput.getText();

                bank.output.writeObject(new NewAuctionHouse(name, port));
                start.setVisible(false);
                run();
            } catch (IOException e) {
                System.out.println("Error creating connection: " + e.getMessage());
            }
        });

         //Formatting and such

        int row = 0;
        root.addRow(row, new Label("Name: "), nameInput);
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

    private void run() {
        AnimationTimer timer = new AnimationTimer() {
            long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - FREQ > lastUpdate) {
                    lastUpdate = now;
                    accWait.stop();
                    updateWindow();
                    primaryStage.sizeToScene();
                }
            }
        };

        accWait = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - 500_000_000 > 0) {
                    if (!accountNumber.equals("none")) {
                        primaryStage.hide();
                        Scene scene = new Scene(buildWindow());
                        primaryStage.setScene(scene);
                        scene.getStylesheets().add("style.css");
                        primaryStage.show();
                        timer.start();
                    }
                }
            }
        };

        accWait.start();
    }

    private GridPane buildWindow() {
        GridPane gp = new GridPane();
        Button close = new Button("Close Auction House");
        balLabel = new Label(USD.format(0));
        int row = 0;

        gp.setPadding(new Insets(10));
        gp.setVgap(5);
        gp.setHgap(5);

        gp.add(new Label("Auction house name: " + name), 0, row, 4, 1);

        gp.add(new Label("Account number: " + accountNumber), 0, ++row, 4, 1);

        balLabel.setMinWidth(50);

        gp.add(new Label("Balance:"), 0, ++row);
        gp.add(balLabel, 1, row);

        gp.add(auctionPane, 1, ++row);

        close.setOnAction(event -> {
            close.setVisible(false);
            newAuctions = false;

            AnimationTimer timer = new AnimationTimer() {
                long lastUpdate = 0;
                @Override
                public void handle(long now) {
                    if (now - 500_000_000 > lastUpdate) {
                        lastUpdate = now;
                        if (checkAuctions()) {
                            try {
                                bank.output.writeObject(new AuctionHouseClosed(accountNumber));
                            } catch (IOException e) {
                                System.out.println(
                                        "Error sending close to bank: " +
                                                e.getMessage());
                            }
                            ps.stop();
                            sl.stop();
                            parseSockThread.interrupt();
                            sockListenThread.interrupt();
                            bank.listener.stop();
                            bank.thread.interrupt();
                            for (String s : agents.keySet()) {
                                Connection c = agents.get(s);
                                c.listener.stop();
                                c.thread.interrupt();
                            }
                            stop();
                        }
                    }
                }

                private boolean checkAuctions() {
                    for (int i = 0; i < 3; i++) {
                        if (auctions[i] != null) return false;
                    }
                    return true;
                }
            };
            timer.start();
        });
        gp.add(close, 4, 0);

        return gp;
    }

    private void updateWindow() {
        auctionPane.getChildren().clear();
        balLabel.setText(USD.format(aHBalance));
        checkAndUpdateAuctions();
        synchronized (auctions) {
            for (int i = 0; i < 3; i++) {
                if (auctions[i] != null) {
                    auctionPane.add(showAuction(auctions[i]), i, 0);
                }
            }
        }
        primaryStage.sizeToScene();
    }

    private class Connection {
        private Socket socket; // bank socket
        private ObjectInputStream input; // input stream
        private ObjectOutputStream output; // output stream
        private String name; // the agent's name
        private Thread thread; // runs the listener
        private MessageListener listener; // listens for messages
    }

    /**
     * Private inner class Auction handles Agents bidding on objects & creates new auctions for items.
     */
    private class Auction {
        private final String item; // the item up for bid
        private final int auctionID; // the auction's unique ID number
        private double winningBid; // the current winning bid on an item
        private String winningAgent; // the agent currently winning the auction
        private String agentAccount; // the agent's account number
        private final List<NewBid> pending; // the list of currently pending bids

        private Timer timer;
        private TimerTask task;
        private boolean expired;
        private final long EXPIRATION_TIMER = 30_000L; //30s delay for timer

        /**
         * Generates string used in hold messages to bank.
         * @param id the auction's ID number
         * @return Combination of acctNum and id to let the bank know to hold funds from the account
         */
        private String holdStr(int id) {
            return accountNumber + ":" + id;
        }

        /**
         * sends message to the bank and to the agent when an auction has been won
         */
        private void auctionWon() {
            try {
                Connection agentConnection = agents.get(agentAccount);
                bank.output.writeObject(new AuctionOver(accountNumber, agentAccount, holdStr(auctionID), winningBid));
                agentConnection.output.writeObject(new AuctionWon(item, winningBid));
            } catch (IOException ignored) {
            }
        }

        /**
         * Verifies whether the agent who placed a bid has enough funds to pay for the bid they placed.
         * Finalizes the confirmation of the bid once the funds have been verified within the agent's bank account.
         * @param message Message from bank indicating that the agent's money hold is valid b/c the bid is confirmed
         */
        private void confirmBid(ConfirmHold message) {
            expired = false; // the bid hasn't expired yet
            reset(); // reset the bid timer

            try {
                NewBid bid = grabBid(message);
                pending.remove(bid); // remove the bid from our list of pending bids
                Connection agentConnection = agents.get(bid.accountNumber()); // grab the agent's bank account number

                // make sure the agent has the funds available in their bank account
                if (!winningAgent.equals("no bidder") &&
                        (!agentAccount.equals(message.accountNumber()))) {
                    bank.output.writeObject(new EndHold(agentAccount, winningBid, holdStr(auctionID)));
                }

                // if the bid amount is higher than the current winning bid
                // replace the current winning bid info with that of the new bid
                // because the new bid is now the current winning bid
                if (bid.bid() > winningBid) {
                    winningBid = bid.bid();
                    winningAgent = agentConnection.name;
                    agentAccount = bid.accountNumber();

                    // send a message to the agent confirming that their bid was
                    // accepted and confirmed, also print a message to the
                    // console about the accepted bid
                    agentConnection.output.writeObject(new ConfirmBid(true, item, name));
                    System.out.println("New bid on item: " + item + "was just accepted from agent: " + winningAgent);
                }
                else {
                    // send a message to the bank that the bid was rejected, also print a message to
                    // the console about the rejected bid
                    agentConnection.output.writeObject(new ConfirmBid(false, item, name));
                    bank.output.writeObject(new EndHold(bid.accountNumber(), bid.bid(), holdStr(bid.id())));
                    System.out.println("New bid on item: " + item + "was just rejected from agent: " + agentConnection.name);
                }
            } catch (IllegalAccessException | IOException ignored) {
            }
        }

        /**
         * Grab a pending bid from the list of pending bids using the bid info provided in the message
         * @param message Confirmation of valid hold message from bank.
         * @return Initial bid message from client.
         * @throws IllegalAccessException Error if old bid does not exist.
         */
        private NewBid grabBid(ConfirmHold message) throws IllegalAccessException {
            for (NewBid bid : pending) {
                // if the bid is found in the list of pending bids, return it
                if ((message.id() == bid.id()) &&
                        (message.accountNumber().equals(bid.accountNumber()))) {
                    return bid;
                }
            }
            // if it's not found, throw an exception and print an error statement
            throw new IllegalAccessException(
                    "That bid was not found.");
        }

        /**
         * Checks to see if a bid placed is valid. If it is valid, confirm the bid and print a
         * message to console indicating that the bid was confirmed. If the bid isn't valid, reject
         * the bid and print a message to the console indicating that the bid was rejected.
         * @param message Bid message from client.
         * @throws IOException Error if unable to request hold with bank.
         */
        private void checkValidBid(NewBid message) throws IOException {
            reset(); // reset the bidding timer
            pending.add(message); // add the bid's info to our list of pending bids
            System.out.println("New bid on the item: " + item +
                    " was just received from agent: " + agents.get(message.accountNumber()).name);

            // a bid is valid if the bid is greater than the current winning bid
            if (message.bid() > winningBid) {
                // send a message to the bank instructing it to hold the bid amount from the bidding agent's account
                bank.output.writeObject(new NewHold(
                        message.accountNumber(), message.bid(), holdStr(auctionID), auctionID));
            } else {
                // a bid is invalid if the bid isn't greater than the current winning bid
                // reject the bid
                agents.get(message.accountNumber()).output.writeObject(
                        new ConfirmBid(false, item, name));
                System.out.println("New bid on the item: " + item +
                        " was just rejected from agent: " + agents.get(message.accountNumber()).name);
            }
        }

        /**
         * Resets the 30s timer on an auction
         */
        private void reset() {
            timer.cancel();
            timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {
                    expired = true;
                }
            };
            timer.schedule(task, EXPIRATION_TIMER);
        }

        /**
         * Constructor for an auction item. Creates a new auction for an item given the auction's unique ID number
         * and the item
         * @param item the item up for bid
         * @param auctionID the auction's unique ID number
         */
        private Auction(String item, int auctionID) {
            this.item = item; // the item
            this.auctionID = auctionID; // the auction's ID
            winningBid = 20; // all auctions should start at $20

            winningAgent = "no bidder"; // the auction was just created, so there's no winning bidder yet

            expired = false; // the auction was just created, so it hasn't expired yet
            pending = new ArrayList<>(); // a list of pending bids

            // Schedule a timer of 30 seconds
            // the task TimerTask will mark an auction as expired if no bids have been placed for
            // a specified duration of time
            timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {expired = true;}
            };

            timer.schedule(task, EXPIRATION_TIMER);
        }
    }
}
