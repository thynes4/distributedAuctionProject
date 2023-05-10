/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Agent Message Parser
 */
package agent;

import javafx.util.Pair;
import general.Message;
import general.Message.*;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

/**
 * reads messages and calls the appropriate method in the bank class based
 * on the type of message received.
 */
public class MessageParser implements Runnable {
    private final Agent agent;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private boolean loop;

    /**
     * Constructor
     * @param agentObj the agent object we're parsing messages for
     * @param messages a blockingqueue to read messages from
     */
    protected MessageParser(Agent agentObj, BlockingQueue<Pair<Message, ObjectOutputStream>> messages) {
        this.agent = agentObj;
        this.messages = messages;
        loop = true;
    }

    /**
     * sets the loop flag to false. stop the thread
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
                Pair<Message, ObjectOutputStream> pair = messages.take();
                Message msg = pair.getKey();
                if (msg instanceof AgentMade agentMade) {
                    agent.setAcctNum(agentMade.accountNumber());
                } else if (msg instanceof AuctionList ahList) {
                    agent.updateAuctionHouses(ahList);
                } else if (msg instanceof AuctionWon auctionWon) {
                    agent.auctionWon(auctionWon);
                } else if (msg instanceof ConfirmBid confirmBid) {
                    String status = confirmBid.success() ?
                            "accepted" : "rejected";
                    System.out.println("Bid " + status + " for " + confirmBid.item() + " at " + confirmBid.name() + ".");
                } else if (msg instanceof NewAuctions newAuctions) {
                    agent.newAuctions(newAuctions);
                } else if (msg instanceof UpdateMoney updateFunds) {
                    agent.updateAcct(updateFunds.amount(), updateFunds.hold());
                }
            } catch (InterruptedException e) {
                System.out.println("Error: Message parser interrupted: " +
                        e.getMessage());
            }
        }
    }
}