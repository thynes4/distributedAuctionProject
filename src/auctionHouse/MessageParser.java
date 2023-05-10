/**
 * Thomas Hynes, Christopher Jarek, Carmen Monohan
 * Auction House Message Parser
 */
package auctionHouse;

import javafx.util.Pair;
import general.Message;
import general.Message.*;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

/**
 * reads messages  and calls the appropriate method in the bank class based
 * on the type of message received.
 */
public class MessageParser implements Runnable {
    private final AuctionHouse auctionHouse;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private boolean loop;

    /**
     * Constructor
     * @param ahObj the auction house object we're parsing messages for
     * @param messages a blockingqueue to read messages from
     */
    protected MessageParser(AuctionHouse ahObj, BlockingQueue<Pair<Message, ObjectOutputStream>> messages) {
        this.auctionHouse = ahObj;
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

                Pair<Message, ObjectOutputStream> data = messages.take();
                Message m = data.getKey();
                if (m instanceof AuctionHouseMade ahMade) {
                    auctionHouse.updateAccountNumber(ahMade.accountNumber());
                } else if (m instanceof CloseAgent closeAgent) {
                    auctionHouse.disconnectAgent(closeAgent.accountNumber());
                }else if (m instanceof ConfirmHold confirmHold) {
                    auctionHouse.bidConfirmation(confirmHold);
                } else if (m instanceof NewBid newBid) {
                    auctionHouse.bidReceived(newBid);
                } else if (m instanceof  UpdateMoney updateMoney) {
                    auctionHouse.updateAHBankAccount(updateMoney.amount());
                }
            } catch (InterruptedException e) {
                System.out.println("Error: Message parser interrupted: " + e.getMessage());
            }
        }
    }
}