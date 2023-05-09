package bank;

import general.Message;
import javafx.util.Pair;

import java.io.*;
import java.util.concurrent.BlockingQueue;

public class MessageListener implements Runnable {
    private boolean loop;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    protected void stop() {
        loop = false;
    }
    @Override
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

    protected MessageListener(BlockingQueue<Pair<Message, ObjectOutputStream>> messages, ObjectInputStream in, ObjectOutputStream out) {
        this.messages = messages;
        this.in = in;
        this.out = out;
        loop = true;
    }
}

