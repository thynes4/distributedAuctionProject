/**
 * Thomas Hynes, Christopher Jarek, Carmen Manohan
 * Agent Main
 */
package agent;

import general.AuctionData;
import general.Message;
import general.Message.*;
import general.SocketData;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Agent extends Application {
    /**
     * Representation of connection information for bank and auction houses.
     */
    private static class Connection {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Thread listenThread;
        private MessageListener msgListener;
    }

    private Connection bank;
    private Map<String, Connection> auctionHouses;
    private Map<String, List<AuctionData>> currentAuctions;
    private BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private List<Pair<String, Double>> itemsWon;

    private final long FREQ = 2_000_000_000; // 2 seconds
    private final NumberFormat USD = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private String acctNum;
    private String name;
    private double balance;
    private double holds;
    private boolean closingAgent;

    private AnimationTimer acctWait;
    private Stage primaryStage;
    private GridPane auctionPane;
    private Label holdLabel;
    private Label balLabel;
    private Label log;




    /**
     * Updates list of items won in auctions along with the amount won.
     * @param message Message from auction house with information on item won.
     */
    protected void auctionWon(Message.AuctionWon message) {
        itemsWon.add(new Pair<>(message.item(), message.amount()));
    }

    /**
     * Updates the current auctions for a specific auction house with new listings
     * received in a message
     * Removes the previous auction data for the given auction house with new listings
     * and replaces it with the new listings
     * @param message the message received
     */
    protected void newAuctions(Message.NewAuctions message) {
        String name = message.name();
        currentAuctions.remove(name);
        currentAuctions.put(name, message.auctionListings());
    }

    /**
     * takes an AuctionList message and updates the list of auction houses
     * that the agent is currently connected to. It adds any new auction houses
     * in the message and removes any auction houses that are no longer in the message
     */
    protected void updateAuctionHouses(Message.AuctionList message) {
        // Get the updated list of auction houses from the message
        Map<String, SocketData> updatedHouses = message.houses();

        // Add any new auction houses that are not already in the agent's list
        for (String auction : updatedHouses.keySet()) {
            if (!auctionHouses.containsKey(auction)) {
                // Create a new connection to the auction house
                Connection house = new Connection();
                SocketData si = updatedHouses.get(auction);
                try {
                    // Connect to the auction house
                    house.socket = new Socket(si.hostname(), si.port());
                    house.out = new ObjectOutputStream(house.socket.getOutputStream());
                    house.out.flush();
                    house.in = new ObjectInputStream(house.socket.getInputStream());

                    // Start a new message listener for this auction house
                    house.msgListener = new MessageListener(messages, house.in, house.out);
                    house.listenThread = new Thread(house.msgListener);
                    house.listenThread.start();

                    // Add the new auction house to the agent's list
                    auctionHouses.put(auction, house);
                    System.out.println("Added new auction house " + auction);

                    // Register the agent with the auction house
                    house.out.writeObject(new Message.RegisterAgent(acctNum, this.name));
                } catch (UnknownHostException e) {
                    System.out.println("Error finding host: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Error creating connection: " + e.getMessage());
                }
            }
        }

        // Remove any auction houses that are no longer in the updated list
        for (String ahName : auctionHouses.keySet()) {
            if (!updatedHouses.containsKey(ahName)) {
                // Remove the auction house from the agent's list
                Connection house = auctionHouses.get(ahName);
                house.msgListener.stop();
                house.listenThread.interrupt();
                auctionHouses.remove(ahName);
                currentAuctions.remove(ahName);
            }
        }
    }

    /**
     * updates the agent's account balance
     * @param balance the agent's updated account balance
     */
    protected void updateAcct(double balance, double holds) {
        this.balance = balance;
        this.holds = holds;
    }

    /**
     * updates the agent's account number
     * @param accountNumber the updated account number
     */
    protected void setAcctNum(String accountNumber) {
        this.acctNum = accountNumber;
    }

    /**
     * Main used to launch jfx Agent
     * @param args unused
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Agent initialization
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

        acctNum = "none";
        messages = new LinkedBlockingQueue<>();
        auctionHouses = Collections.synchronizedMap(new HashMap<>());
        currentAuctions = Collections.synchronizedMap(new HashMap<>());
        itemsWon = new ArrayList<>();
        this.primaryStage = primaryStage;

        GridPane root = new GridPane();
        TextField nameInput = new TextField();
        TextField bankName = new TextField();
        TextField bankPort = new TextField();
        TextField balance = new TextField();
        Button start = new Button("Start");


        /**
         * start logic goes here
         */
        start.setOnAction(event -> {
            try {
                closingAgent = false;
                bank = new Connection();
                bank = new Connection();
                bank.socket = new Socket(bankName.getText(), Integer.parseInt(bankPort.getText()));
                bank.out = new ObjectOutputStream(bank.socket.getOutputStream());
                bank.out.flush();
                bank.in = new ObjectInputStream(bank.socket.getInputStream());
                bank.msgListener = new MessageListener(messages, bank.in, bank.out);
                bank.listenThread = new Thread(bank.msgListener);
                bank.listenThread.start();
                //TODO: Add multiple message listeners, add threads to global list to stop later
                MessageParser pm = new MessageParser(this, messages);
                Thread t = new Thread(pm);
                t.start();
                name = nameInput.getText();
                bank.out.writeObject(new NewAgent(name, Double.parseDouble(balance.getText())));
                start.setVisible(false);
                run();
            } catch (UnknownHostException e) {
                System.out.println("Error finding host: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error creating connection: " + e.getMessage());
            }
        });


        /**
         * Formatting and such
         */
        int row = 0;
        root.addRow(row, new Label("Name: "), nameInput);
        root.addRow(++row, new Label("Balance: "), balance);
        root.addRow(++row, new Label("Bank Name: "), bankName);
        root.addRow(++row, new Label("Bank Port: "), bankPort);
        root.add(start, 1, ++row);

        root.setPadding(new Insets(10));
        root.setVgap(10);
        root.setHgap(10);

        primaryStage.setTitle("Agent");
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
                    acctWait.stop();
                    updateWindow();
                    primaryStage.sizeToScene();
                }
            }
        };

        acctWait = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - 500_000_000 > 0) {
                    if (!acctNum.equals("none")) {
                        primaryStage.hide();
                        Scene scene = new Scene(buildWindow());
                        primaryStage.setScene(scene);
                        primaryStage.show();
                        timer.start();
                    }
                }
            }
        };

        acctWait.start();
    }

    private void updateWindow() {
        balLabel.setText(USD.format(balance));
        holdLabel.setText(USD.format(holds));
        showAuctions();
        showItemsWon();
        primaryStage.sizeToScene();
    }

    private void showAuctions() {
        int row = 0, col;
        auctionPane.getChildren().clear();

        for (String name : currentAuctions.keySet()) {
            List<AuctionData> auctions = currentAuctions.get(name);
            VBox vbox = new VBox();
            vbox.setMinWidth(100);
            col = 0;

            vbox.getChildren().add(new Label("Auction house:"));
            vbox.getChildren().add(new Label(name));
            auctionPane.add(vbox, 0, row);
            for (AuctionData info : auctions) {
                auctionPane.add(showAuction(name, info), ++col, row);
            }
            row++;
        }
    }

    private GridPane showAuction(String name, AuctionData info) {
        GridPane gp = new GridPane();
        Button bid = new Button("Place bid.");
        int row = 0;

        gp.setPadding(new Insets(10));
        gp.setHgap(5);
        gp.setVgap(5);
        gp.setMinWidth(200);

        gp.add(new Label("ID: " + info.ID()), 0, row);
        gp.add(new Label("Item: " + info.item()), 1, row);
        gp.add(new Label("Price: " + USD.format(info.winningBid())), 0, ++row);
        gp.add(new Label("Current high bidder: " + info.winningAgent()), 0, ++row, 2, 1);

        if (balance - holds >= info.winningBid()) {
            gp.add(bid, 0, ++row, 2, 1);
        }

        bid.setOnAction(event -> {
            if (balance - holds >= info.winningBid() + 1) {
                Connection ah = auctionHouses.get(name);
                try {
                    ah.out.writeObject(new NewBid(info.item(), info.ID(), info.winningBid() + 1, acctNum));
                } catch (IOException e) {
                    System.out.println("Error sending bid to auction house: " + e.getMessage());
                }
            }
        });

        if (closingAgent) bid.setVisible(false);

        return gp;
    }

    private void showItemsWon() {
        log.setText("");
        for (Pair<String, Double> pair : itemsWon) {
            log.setText(pair.getKey() + " (" + USD.format(pair.getValue()) +
                    ")\n" + log.getText());
        }
    }

    private GridPane buildWindow() {
        GridPane gp = new GridPane();
        log = new Label("");
        ScrollPane logPane = new ScrollPane();
        Button close = new Button("Close Bidding Agent");
        balLabel = new Label(USD.format(0));
        holdLabel = new Label(USD.format(0));
        auctionPane = new GridPane();
        int row = 0;

        gp.setPadding(new Insets(10));
        gp.setVgap(5);
        gp.setHgap(5);

        gp.add(new Label("Bidder name: " + name), 1, row, 4, 1);

        gp.add(new Label("Account number: " + acctNum), 1, ++row, 4, 1);

        balLabel.setMinWidth(50);
        holdLabel.setMinWidth(50);

        gp.add(new Label("Balance:"), 1, ++row);
        gp.add(balLabel, 2, row);
        gp.add(new Label("Holds:"), 3, row);
        gp.add(holdLabel, 4, row);

        auctionPane.setPadding(new Insets(10));
        auctionPane.setVgap(5);
        auctionPane.setHgap(5);

        gp.add(auctionPane, 2, ++row, 3, 1);

        log.setMinWidth(200);
        logPane.setMinHeight(500);
        logPane.setMinWidth(225);
        logPane.setContent(log);
        gp.add(new Label("Items won:"), 0, 1);
        gp.add(logPane, 0, 2, 1, 3);

        close.setOnAction(event -> {
            close.setVisible(false);
            closingAgent = true;

            AnimationTimer timer = new AnimationTimer() {
                long lastUpdate = 0;
                @Override
                public void handle(long now) {
                    if (now - 500_000_000 > lastUpdate) {
                        lastUpdate = now;
                        if (checkAuctions()) {
                            CloseAgent msg = new CloseAgent(acctNum);
                            try {
                                bank.out.writeObject(msg);
                                bank.msgListener.stop();
                                bank.listenThread.interrupt();
                            } catch (IOException e) {
                                System.out.println(
                                        "Error writing close message to bank: "
                                                + e.getMessage());
                            }

                            for (String s : auctionHouses.keySet()) {
                                Connection ah = auctionHouses.get(s);
                                try {
                                    ah.out.writeObject(msg);
                                    ah.msgListener.stop();
                                    ah.listenThread.interrupt();
                                } catch (IOException e) {
                                    System.out.println("Error writing close message: " + e.getMessage());
                                }
                            }
                            auctionHouses.clear();
                            currentAuctions.clear();
                            stop();
                        }
                    }
                }

                private boolean checkAuctions() {
                    for (String s : currentAuctions.keySet()) {
                        for (AuctionData info : currentAuctions.get(s)) {
                            if (info.winningAgent().equals(name)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            };
            timer.start();
        });
        gp.add(close, 5, 0);

        return gp;
    }
}