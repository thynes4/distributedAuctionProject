/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Bank Message Parser
 */
package bank;

import general.Message;
import javafx.util.Pair;
import general.Message.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;


/**
 * reads messages and calls the appropriate method in the bank class based
 * on the type of message received.
 */
public class MessageParser implements Runnable {
    private final Bank bank;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private boolean loop;

    /**
     * Constructor
     * @param bankObj the bank object we're parsing messages for
     * @param messages a blockingqueue to read messages from
     */
    protected MessageParser(Bank bankObj, BlockingQueue<Pair<Message, ObjectOutputStream>> messages) {
        this.bank = bankObj;
        this.messages = messages;
        loop = true;
    }

    /**
     * sets the loop flag to false. to stop the thread
     */
    protected void stop() {
        loop = false;
    }

    /**
     * Run method overrides the runnable interface and is executed when the thread is started.
     * Reads messages from the messages and processes them accordingly
     */
    @Override
    public void run() {
        while (loop) {
            try {
                Pair<Message, ObjectOutputStream> data = messages.take();
                ObjectOutputStream out = data.getValue();
                Message m = data.getKey();

                if (m instanceof AuctionOver auctionOver) {
                    bank.auctionEnded(auctionOver.ahNum(), auctionOver.agentNum(), auctionOver.item(), auctionOver.amount());
                    bank.sendAgentBalance(auctionOver.agentNum());
                    bank.sendAHBalance(auctionOver.ahNum());
                } else if (m instanceof AuctionHouseClosed closeAH) {
                    bank.closeAuctionHouseAcc(closeAH.accountNumber());
                } else if (m instanceof CloseAgent closeAgent) {
                    bank.closeAgentAccount(closeAgent.accountNumber());
                } else if (m instanceof EndHold endHold) {
                    bank.removeHold(endHold.accNum(), endHold.item());
                    bank.sendAgentBalance(endHold.accNum());
                } else if (m instanceof NewHold newHold) {
                    boolean holdPlaced = bank.addNewHold(newHold);
                    bank.sendAgentBalance(newHold.accountNumber());
                    out.writeObject(new ConfirmHold(holdPlaced, newHold.accAndID(), newHold.accountNumber(), newHold.idNum()));
                } else {
                    System.out.println("Error: Invalid message on existing stream.");
                }
            } catch (InterruptedException e) {
                System.out.println("Message parser interrupted : " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error sending hold confirmation : " + e.getMessage());
            }
        }
    }

}
