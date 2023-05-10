/**
 * Thomas Hynes, Christopher Jarek, Carmen Manohan
 * Interface to be used to format our messages between the bank, agent,
 * and auction house
 */

package general;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface Message {
    record AgentMade(String accountNumber) implements Message, Serializable {}
    record NewAgent(String name, double initialBalance) implements Message, Serializable {}
    record RegisterAgent(String accountNumber, String name) implements Message, Serializable {}
    record NewHold(String accountNumber, double amount, String accAndID, int idNum) implements Message, Serializable {}
    record ConfirmHold(boolean success, String item, String accountNumber, int id) implements Message, Serializable {}
    record EndHold(String accountNumber, double amount, String item) implements Message, Serializable {}
    record CloseAgent(String accountNumber) implements Message, Serializable {}
    record AuctionList(Map<String, SocketData> houses) implements Message, Serializable {}
    record AuctionHouseMade(String accountNumber) implements Message, Serializable {}
    record NewAuctionHouse(String name, int port) implements Message, Serializable {}
    record NewAuctions(String name, List<AuctionData> auctionListings) implements Message, Serializable {}
    record AuctionOver(String  ahNum, String agentNumber, String item, double amount) implements Message, Serializable {}
    record AuctionWon(String item, double amount) implements Message, Serializable {}
    record AuctionHouseClosed(String accountNumber) implements Message, Serializable {}
    record ConfirmBid(boolean success, String item, String name) implements Message, Serializable {}
    record updateMoney(double amount, double hold) implements Message, Serializable {}
    record NewBid(String item, int id, double bid, String accountNumber)
            implements Message, Serializable {}

}
