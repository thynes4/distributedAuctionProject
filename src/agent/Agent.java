/**
 * Thomas Hynes, Christopher Jarek, Carmen Manohan
 * Agent Main
 */
package agent;

import general.AuctionData;
import general.Message;
import general.SocketData;
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
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


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

    private Map<String, Connection> auctionHouses;
    private Map<String, List<AuctionData>> currentAuctions;
    private BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private List<Pair<String, Double>> itemsWon;

    private String acctNum;
    private String name;
    private double balance;
    private double holds;


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
        GridPane root = new GridPane();
        TextField name = new TextField();
        TextField bankName = new TextField();
        TextField bankPort = new TextField();
        TextField balance = new TextField();
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

}