package auctionHouse;

import javafx.util.Pair;
import general.Message;
import general.Message.*;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

public class MessageParser implements Runnable {
    private final AuctionHouse auctionHouse;
    private final BlockingQueue<Pair<Message, ObjectOutputStream>> messages;
    private boolean loop;

    protected MessageParser(AuctionHouse ahObj, BlockingQueue<Pair<Message, ObjectOutputStream>> messages) {
        this.auctionHouse = ahObj;
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
                Message m = data.getKey();
                if (m instanceof AuctionHouseMade ahMade) {
                    auctionHouse.updateAccountNumber(ahMade.accNum());
                } else if (m instanceof CloseAgent closeAgent) {
                    auctionHouse.disconnectAgent(closeAgent.accNum());
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