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
    record AgentMade(String accNum) implements Message, Serializable {}
    record NewAgent(String name, double startBal) implements Message, Serializable {}
    record RegisterAgent(String accNum, String name) implements Message, Serializable {}
    record NewBid(String item, int id, double bid, String accNum) implements Message, Serializable {}
    record NewHold(String accNum, double amount, String accAndID, int idNum) implements Message, Serializable {}
    record ConfirmHold(boolean success, String item, String accNum, int id) implements Message, Serializable {}
    record EndHold(String accNum, double amount, String item) implements Message, Serializable {}
    record CloseAgent(String accNum) implements Message, Serializable {}
    record AuctionList(Map<String, SocketData> houses) implements Message, Serializable {}
    record AuctionHouseMade(String accNum) implements Message, Serializable {}
    record NewAuctionHouse(String name, int port) implements Message, Serializable {}
    record NewAuctions(String name, List<AuctionData> auctionListings) implements Message, Serializable {}
    record AuctionOver(String  ahNum, String agentNum, String item, double amount) implements Message, Serializable {}
    record AuctionWon(String item, double amount) implements Message, Serializable {}
    record AuctionHouseClosed(String accNum) implements Message, Serializable {}
    record ConfirmBid(boolean success, String item, String name) implements Message, Serializable {}
    record UpdateMoney (double amount, double hold) implements Message, Serializable {}

}
