package agent;

import general.Message;
import javafx.util.Pair;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/**
 * listens for incoming messages
 */
public class MessageListener implements Runnable {
    private boolean loop;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    /**
     * Creates a new MessageListener with the specified queue of messages
     * @param messages the queue of messages to be listened to
     * @param in input stream
     * @param out output stream
     */
    protected MessageListener(BlockingQueue<Pair<Message, ObjectOutputStream>> messages, ObjectInputStream in, ObjectOutputStream out) {
        this.messages = messages;
        this.in = in;
        this.out = out;
        loop = true;
    }

    /**
     * sets the loop flag false to stop the thread
     */
    protected void stop() {
        loop = false;
    }

    /**
     * Reads incoming messages and puts them into a blockingqueue
     * messages are paired with an output stream that can be used to send responses back to sender
     * runs until the loop flag is set to false by calling stop()
     */
    public void run() {
        while (loop) {
            try {
                Message m = (Message)in.readObject();
                messages.put(new Pair<>(m, out));
            } catch (InterruptedException e) {
                System.out.println("Message listener interrupted: " +
                        e.getMessage());
                loop = false;
            } catch (ClassNotFoundException e) {
                System.out.println("Error: Invalid incoming message class: " +
                        e.getMessage());
            } catch (IOException e) {
                System.out.println("Error: IO error on message listener: " +
                        e.getMessage());
                loop = false;
            }
        }
    }
}
