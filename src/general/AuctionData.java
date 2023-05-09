package general;

import java.io.Serializable;

public record AuctionData(String item, int iD, double curBid, String topBidder) implements Serializable {
    @Override
    public String toString() {
        return "Item: " + item + " ID:" + iD + " - " + topBidder + " $" + String.format("%.2f", curBid);
    }
}
