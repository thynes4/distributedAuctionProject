# **DISTRIBUTED AUCTION**
By: Thomas Hynes, Christopher Jarek, and Carmen Monohan 

## _Project Overview:_

---
This program simulates a system of multiple auction houses selling items, 
multiple agents buying items, and a bank to keep track of everyone's funds. 

The bank exists on one machine at a static known address, while the agents 
and auction houses can be dynamically created on other machines. 

## _Packages Overview:_

---
### BANK PACKAGE
___
##### Bank.java:
The bank is static and at a known address. This program must be started before
either the agents or the auction house. The bank is a server, whereas the agents
and auction houses are its clients.

Both agents and auction houses have bank accounts. When an agent bids or is outbid
on an auction, the bank will block or unblock the appropriate amount of funds, at the 
request of the auction house. When an agent wins an auction, the bank will transfer these 
blocked funds from the agent to the auction house account, at the request of the agent.

Auction houses provide the bank with their host and port information. The bank provides the 
agents with the list of the auction houses and their addresses so the agents will be able 
to connect directly to the auction houses.

##### MessageListener.java:
This class listens for incoming messages from a client and adds them to a blockingqueue 
along with an output stream that can be used to send responses back to the client. This class
is designed to run as a separate thread. 
##### MessageParser.java:
This class parses messages received from a client, such as an agent or an auction house, and
processes those messages accordingly. 
##### SocketListener.java:
This class listens for incoming network connections on a specific port, accept those connections,
and add them to a provided blockingqueue data structure for further processing by another thread.
##### SocketParser.java:
This class reads and parses messages from a blockingqueue of socket objects and processes 
those messages accordingly based on their content.

---
### AUCTION HOUSE PACKAGE
___
##### AuctionHouse.java:
Each auction house is dynamically created. Upon creation, it registers with the bank, opening an 
account with a zero balance. It also provides the bank with its host and port address, so that the 
bank can inform the agents of the existence of this auction house. An auction house is a client of
the bank, but it is also a server with agents as its clients.

An auction house hosts a list of items being auctioned and tracks the current bidding status of each 
item. Initially, the auction house will offer at least 3 items for sale. As the items are sold, new 
items will be listed to replace them. 

Upon request, it shares the list of items being auctioned and the bidding status with agents, including
for each item house id, item id, description, minimum bid, and current bid.

The user may terminate the program when no bidding activity is in progress. The program does not allow
exit when there are still bids to be resolved. At termination, the program de-registers with the bank.
An auction house terminating does not break the behavior of any other program in the system. 

    AUCTION RULES:
     - The auction house receives a bid and acknowledges it with a reject or accept response. 
     - When a bid is accepted, the bank is requested to block those funds. 
     - A bid is not accepted if there are not enough available funds in the bank.
     - When a bid is overtaken, an outbid notification is sent to the agent and the funds are unblocked.
     - A bid is successful if not overtaken in 30 seconds.
     - When winning a bid, the agent receives a winner notification and the auction house waits 
       for the unblocked funds to be transferred into its account. 
     - If there has been no bid placed on an item, the item remains listed for sale. 

##### MessageListener.java:
This class listens for incoming messages from a client and adds them to a blockingqueue
along with an output stream that can be used to send responses back to the client. This class
is designed to run as a separate thread.
##### MessageParser.java:
This class parses messages received from a client, such as the bank or an agent, and
processes those messages accordingly.
##### SocketListener.java:
This class listens for incoming network connections on a specific port, accept those connections,
and add them to a provided blockingqueue data structure for further processing by another thread.
##### SocketParser.java:
This class reads and parses messages from a blockingqueue of socket objects and processes
those messages accordingly based on their content.

---
### AGENT PACKAGE
___
##### Agent.java:
Each agent is dynamically created. Upon creation, it opens a bank account by providing a name and an 
initial balance, and receives a unique account number. The agent is a client of both the bank and the 
auction houses. 

The agent gets its list of active auction houses from the bank. It connects to an auction house using 
the host and port information sent from the bank. The agent receives a list of items being auctioned 
from the auction house. 

When an agent places a bid on an item, it receives back one or more status messages as the auction proceeds:

    - ACCEPTANCE: The bid is the current high bid
    - REJECTION: The bid was invalid, too low, insufficient funds in the bank, etc.
    - OUTBID: Some other agent has placed a higher bid
    - WINNER: The auction is over and this agent has won

The agent notifies the bank to transfer the blocked funds to the auction house after it has won a bid. 

The program may terminate when no bidding activity is in progress. The program does not allow exit when
there are still bids to be resolved. At termination, it de-registers with the bank. An agent terminating
does not break the behavior of any other programs in the system. 

##### MessageListener.java:
This class listens for incoming messages from a client and adds them to a blockingqueue
along with an output stream that can be used to send responses back to the client. This class
is designed to run as a separate thread.
##### MessageParser.java:
This class parses messages received from a client, such as the bank or an agent, and
processes those messages accordingly.

---
### GENERAL PACKAGE
___
##### AuctionData.java:
This class represents the data for an auction. Holds auction info like the current winning bid,
the item up for bid, the auction's ID number, and the bidder who placed the current winning bid.
##### Message.java:
This class represents a Message interface that defines messages that can be exchanged within the 
distributed auction system. Each message is defined as a record with a specific set of fields.
##### SocketData.java:
Record class that holds data for a socket. Holds the host name and port number.


## _User Interface:_ 
The UI includes sections of the Auction House and Agent classes. Each class has two UI elements, the login and menus.
The login page for each of these prompts the user for any required information to continue, and the menu is used
to display important information.
The Agent menu also includes buttons so that the user can place a bid on a desired item. These buttons are only active
if the user has enough funds to support a bid.

## _How To Use:_


