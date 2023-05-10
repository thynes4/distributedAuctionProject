package general;

import java.io.Serializable;

/**
 * Represents the data for an auction. Super simple. Holds auction info like the current winning bid,
 * the item up for bid, the auction's ID number, and the bidder who placed the current winning bid.
 */
public record AuctionData(String item, int ID, double winningBid, String winningAgent) implements Serializable {
    @Override
    public String toString() {
        return "Item: " + item + " ID:" + ID + " - " + winningAgent + " $" + String.format("%.2f", winningBid);
    }
}
