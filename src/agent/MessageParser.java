package agent;

import javafx.util.Pair;
import general.Message;
import general.Message.*;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

public class MessageParser implements Runnable {
    private final Agent agent;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private boolean loop;

    protected MessageParser(Agent agentObj, BlockingQueue<Pair<Message, ObjectOutputStream>> messages) {
        this.agent = agentObj;
        this.messages = messages;
        loop = true;
    }

    protected void stop() {
        loop = false;
    }

    @Override
    public void run() {
        while (loop) {
            try {

                Pair<Message, ObjectOutputStream> data = messages.take();
                ObjectOutputStream out = data.getValue();
                Message m = data.getKey();

                //ADD FUNCTIONALITY TO HANDLE DIFFERENT MESSAGES

            } catch (InterruptedException e) {
                System.out.println("Error: Message parser interrupted: " + e.getMessage());
            }
        }
    }
}