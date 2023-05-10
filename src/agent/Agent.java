/**
 * Thomas Hynes, Christopher Jarek, Carmen Manohan
 * Agent Main
 */
package agent;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Agent extends Application {
    private Connection bank;
    private String ID = "N/A";
    private String name = "N/A";
    private double bal;

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

    private class Connection {
        private Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private Thread thread;
        //private MsgListener msgListener;
    }
}