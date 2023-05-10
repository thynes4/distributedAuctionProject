/**
 * Thomas Hynes, Christopher Jarek, Carmen Manohan
 * Auction House Main
 */
package auctionHouse;

import agent.Agent;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AuctionHouse extends Application{
    private Connection bank;


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
        //private MsgListener msgListener;
    }
}
