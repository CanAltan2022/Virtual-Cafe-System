// Student Name: Can Altan
// Student ID: 2316850
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket; import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;

public class Barista {
  private static final int PORT = 8888;
  // For keeping track of customer orders ---
  private static final Map<String, Map<String,Integer>> orders = new ConcurrentHashMap<>();
  private static final Map<String, Map<String,Integer>> waitingArea = new ConcurrentHashMap<>();
  private static final Map<String, Integer> brewingArea = new ConcurrentHashMap<>();// max 2 coffees and 2 teas
  private static final Map<String, Map<String,Integer>> tray = new ConcurrentHashMap<>();
  //-------
  // brewing Area limits ---
  private static final int MAX_TEA = 2; 
  private static final int MAX_COFFEE = 2;  
  // -------
  private static int totalClients = 0; 
  private static int connectionNum = 0; // for client ID assignement 
  private static boolean regularExit = false;
  public static void main(String[] args) {
    brewingArea.put("tea", 0);
    brewingArea.put("coffee", 0);
    startServer();
  }
  private static void startServer() {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(PORT);
      System.out.println("Waiting for incoming connections...");
      while(true) {
        Socket socket = serverSocket.accept();
        System.out.println("Accepted");
        new Thread(new ClientHandler(socket)).start();
        totalClients++;
    }
  } catch (IOException e) { e.printStackTrace();}
}

static class ClientHandler implements Runnable {
  private final Socket socket;
  private String customerID;

  public ClientHandler(Socket socket) 
  {
    this.socket = socket;
  }

  @Override
  public void run() {
    try (Scanner scanner = new Scanner(socket.getInputStream());
       PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) 
       {
        String customerName =  scanner.nextLine();
        // create the customer in all areas with the ID and name -> to prevent duplicate names -----
        customerID = "ID:" + connectionNum + "|" + customerName;
        connectionNum++;
        System.out.println("New connection from customer: " + customerID);
        servStateChange();
        writer.println("SUCCESS");
        
        orders.put(customerID, new ConcurrentHashMap<String, Integer>());
        orders.get(customerID).put("tea", 0);
        orders.get(customerID).put("coffee", 0);

        waitingArea.put(customerID, new ConcurrentHashMap<String, Integer>());
        waitingArea.get(customerID).put("tea", 0); 
        waitingArea.get(customerID).put("coffee", 0);

        tray.put(customerID, new ConcurrentHashMap<String, Integer>());
        tray.get(customerID).put("tea", 0);
        tray.get(customerID).put("coffee", 0);
        // ---------------

        while (scanner.hasNextLine()) {
          String command = scanner.nextLine();
          if(command.equalsIgnoreCase("MENU")){
            writer.println("<Quantity> tea & <Quantity> coffee"); 
            writer.println("END");
          }
          else if(command.equalsIgnoreCase("ORDER STATUS")){
            orderStatus(customerID,writer);
            writer.println("END");
          }

          else if(command.startsWith("ORDER") || command.startsWith("order")){ 
            // create another thread for handling Order processing.meanwhile, let the main thread handle other commands 
            Thread processOrders = new Thread(() -> {
              String[] tokens = command.split(" ");
              if(tokens.length < 2) {
                writer.println("Invalid order format. Please, use ORDER/order <quantity> <tea or coffee> <and> ....");
              } else {
              HandleOrder(tokens, customerID, writer); 
              }
              writer.println("END");
              });
            processOrders.start();
            // --------------------
          }
          else if(command.equalsIgnoreCase("COLLECT")) {
            // assignments for simplifying access to-------  
            Map<String, Integer> trayCustomer=tray.get(customerID);
            Map<String, Integer> orderCustomer = orders.get(customerID);
            // ------  
            if (orderCustomer.get("tea") ==0 && orderCustomer.get("coffee") ==0){
              writer.println("No order found for: "+ customerID);
              } else {
              if (isOrderReady(customerID)){
                trayCustomer.put("tea", 0); trayCustomer.put("coffee", 0);
                orderCustomer.put("tea", 0); orderCustomer.put("coffee", 0);
                writer.println(customerID + ", You have successfully collected your order.");
                servStateChange();
                } else {writer.println(customerID + ", Your order is not ready, yet");}
            }
            writer.println("END");
          }
          else if (command.equalsIgnoreCase("EXIT")){
            regularExit = true;
            writer.println("Goodbye... come again..."); 
            clientCleanUp(customerID);
            totalClients--;
            writer.println("END");
          }
          else{
            writer.println("Invalid Command: " + command);
            writer.println("END");
          }
         
        }
      } catch(Exception e){System.out.println("Error handling client: " + e.getMessage());} 
      if(!regularExit){
        clientCleanUp(customerID);
        totalClients --;
        servStateChange();
      } // --> sigINT cleanups
      System.out.println("Customer disconnected: " + customerID);
    }

    private static void clientCleanUp(String customerID) {
      synchronized (brewingArea){
        // remove client data ...
        orders.remove(customerID);
        waitingArea.remove(customerID);
        tray.remove(customerID);
        // then notify all the threads that are waiting for the brewing area
        brewingArea.notifyAll();
      }
      System.out.println(customerID + " Has left the cafe");
    }
    private static void HandleOrder(String[] tokens, String customerID, PrintWriter writer){
      // Helper method for parsing the order and assinging items, etc to correct data structs
      String orderSummary = customerID + " has ordered ";
      int teaCount= 0;
      int coffeeCount=0;
      boolean isValidOrder =  true;
        for(int i = 1; i<tokens.length; i++){
            if(tokens[i].equalsIgnoreCase("tea") || tokens[i].equalsIgnoreCase("teas")){
              // if token[i -1] is not a number --> mark order as invalid---
              if (!tokens[i - 1].matches("\\d+")) {
                writer.println("Please enter the quantity of the item before specifying 'tea' or 'teas'.");
                isValidOrder =false;
                break;
                //-----
              } else {
                teaCount = Integer.parseInt(tokens[i-1]);
                orders.get(customerID).put("tea", orders.get(customerID).get("tea") + teaCount);
              
              if(teaCount>1){tokens[i]="teas";}
                orderSummary+=teaCount + " " + (String)(tokens[i]);
              }
            }
            else if(tokens[i].equalsIgnoreCase("coffee") || tokens[i].equalsIgnoreCase("coffees")){ 
              if (!tokens[i - 1].matches("\\d+")) {
                writer.println("Please enter the quantity of the item before specifying 'coffee' or 'coffees'.");
                isValidOrder =false;
                break;
              } else{
              coffeeCount = Integer.parseInt(tokens[i-1]);
              orders.get(customerID).put("coffee", orders.get(customerID).get("coffee") + coffeeCount);
              
              if(coffeeCount>1){tokens[i]="coffees";}
                orderSummary+=coffeeCount+ " " + (String)(tokens[i]);
              }
            }
            // for orders with separate items
            else if(tokens[i].equalsIgnoreCase("and")){
              orderSummary+= " and "; 
            } 

            else if(tokens[i].matches("\\d+")) {
              // checks if customer specified an item after quantity
              if(tokens.length > i + 1 ){
              continue;
              } else {
                  writer.println("please, specify an item"); 
                  isValidOrder =false;
                  break;  
                  }
            }
            else {
              writer.println("Invalid order format. Please, use ORDER/order <quantity> <tea or coffee> <and> ....");
              isValidOrder =false; 
              break;
            }
          }

        if (isValidOrder){
          // do the necessary assingments
          waitingArea.get(customerID).put("tea", waitingArea.get(customerID).get("tea") + teaCount); 
          waitingArea.get(customerID).put("coffee", waitingArea.get(customerID).get("coffee") + coffeeCount); 
          System.out.println(orderSummary);
          servStateChange();
          ProcessBrewing(customerID, teaCount, coffeeCount);
        }
      }
    private static void ProcessBrewing(String customerID, int teaCount, int coffeeCount){
      // method for simulating and handling brewing of items ...
      for (int i = 0; i < teaCount; i++) {
        // create separate threads for each tea in the order. 
        new Thread(() -> {
            synchronized (brewingArea) { 
                if (!waitingArea.containsKey(customerID)) {
                    brewingArea.notifyAll();
                    return; // Exit thread if customer left
                }
            }
            // Brewing process
            try {
                synchronized (brewingArea) {
                    // if brewing area is at max capacity make the thread wait for space
                    while (brewingArea.get("tea") >= MAX_TEA) {
                        brewingArea.wait();
                    }
                    if (!waitingArea.containsKey(customerID)) return; // Customer left
                    
                    waitingArea.get(customerID).put("tea", waitingArea.get(customerID).get("tea") - 1);
                    brewingArea.put("tea", brewingArea.get("tea") + 1);
                    servStateChange();
                }

                // Simulate brewing time
                Thread.sleep(30000);
                synchronized (brewingArea) {
                    // if customer left. remove the teas from the brewing area to make space.
                    if (!tray.containsKey(customerID)) {
                        brewingArea.put("tea", brewingArea.get("tea") - 1);
                        brewingArea.notifyAll();
                        return;
                    }
                    // ------
                    tray.get(customerID).put("tea", tray.get(customerID).get("tea") + 1);
                    brewingArea.put("tea", brewingArea.get("tea") - 1);
                    brewingArea.notifyAll();
                    servStateChange();
                }

                if (isOrderReady(customerID)) {
                    System.out.println(customerID + "'s order is ready for collection");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Handle thread interruption
            }
        }).start();

    }
      
    for(int i = 0; i<coffeeCount; i++){
       // create separate threads for each coffee in the order.
      new Thread(() -> {
      synchronized (brewingArea) {
        if(!waitingArea.containsKey(customerID)){
          brewingArea.notifyAll();
          return;
        }
      }
        try {
          synchronized(brewingArea){
            while(brewingArea.get("coffee")>= MAX_COFFEE){
              brewingArea.wait();
            }
            if(!waitingArea.containsKey(customerID)) return;

          waitingArea.get(customerID).put("coffee", waitingArea.get(customerID).get("coffee") -1);
          brewingArea.put("coffee", brewingArea.get("coffee")+1);
          servStateChange();
        }
        
          Thread.sleep(45000);
          synchronized (brewingArea){
            if(!tray.containsKey(customerID)) {
              brewingArea.put("coffee", brewingArea.get("coffee") - 1);
              brewingArea.notifyAll();
              return;
            }
          tray.get(customerID).put("coffee", tray.get(customerID).get("coffee") + 1);
          brewingArea.put("coffee", brewingArea.get("coffee")-1);
          brewingArea.notifyAll();
          servStateChange();
        }
          if(isOrderReady(customerID)){ 
            System.out.println(customerID+"'s order is ready for collection");
          }
      
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      }).start();
  }
      
    }

    private static String orderStatusHelper(String customerID, String status, String area, Map<String, Map<String, Integer>> map){
      // for tray and waiting areas...
      Map<String, Integer> customer = map.get(customerID);
      if(customer !=null){
        StringBuilder sb = new StringBuilder(status);
        if(customer.get("tea") != null) {
          sb.append("> ").append(customer.get("tea")).append("tea(s)");
        }
        if(customer.get("coffee") != null) {
          if(sb.length() > 0) {
            sb.append("and ");
          }
          sb.append(customer.get("coffee")).append(" coffee(s)");
        }
        if(sb.length() > 0) {
          return sb.append(" in the ").append(area).toString();
        }
      }
      return ""; 
    }
    private static void orderStatus(String customerID, PrintWriter writer){
      String waitingAreaStat = "";
      String brewingAreaStat = "";
      String trayStat = "";
      // String builder for preventing unexpected errors caused by threads and efficiency
      StringBuilder sb = new StringBuilder(brewingAreaStat);
      Map<String, Integer> order = orders.get(customerID);

      if (order.get("tea") == 0 && order.get("coffee") == 0){
        writer.println("No order found for "+ customerID);
      } else {
        writer.println("Order Status For " + customerID + ":");
        waitingAreaStat = orderStatusHelper(customerID, waitingAreaStat, "waiting area", waitingArea);
        trayStat = orderStatusHelper(customerID, trayStat, "tray area", tray);

        writer.println(waitingAreaStat);
        writer.println(trayStat);
        // calculate how many teas and coffees are being brewed
        sb.append("> ").append(order.get("tea") - (waitingArea.get(customerID).get("tea") + tray.get(customerID).get("tea"))).append("tea(s)");
        sb.append(" and ").append(order.get("coffee") - (waitingArea.get(customerID).get("coffee") + tray.get(customerID).get("coffee"))).append("coffee(s) in the brewing area") ;
        writer.println(sb.toString());
        }
    }
    
    private static boolean isOrderReady(String customerID){
      Map<String, Integer> trayCustomer=tray.get(customerID);
      Map<String, Integer> orderCustomer = orders.get(customerID);
      
        if(trayCustomer.get("tea") == orderCustomer.get("tea") && 
          trayCustomer.get("coffee") == orderCustomer.get("coffee")){
            return true;
        }
      return false;
    }
    private static void servStateChange() {
      // method for keeping track and reporting changes that take place in the server
      int activeCustomers = 0; int coffeeWaiting = 0;
      int teaWaiting =0; int trayCoffee = 0; int trayTea =0;
      System.out.println("----- Current Server State -----");
      if (totalClients != 0) {
      System.out.println("Number of customers in the cafe: " + totalClients);
      } else {
      System.out.println("There are no customers in the cafe");
      }
      // go through the customers and check if they have ordered anything ....
      for(Map.Entry<String, Map<String, Integer> > customer: orders.entrySet()){
        Map<String, Integer> items = customer.getValue();
        if (items.get("tea") != 0 || items.get("coffee")!=0){
          activeCustomers ++;
        };
      }
    System.out.println("Number of customers waiting for orders: " + activeCustomers);
    
    System.out.println("Items in the Waiting Area: ");
    // go through the customers in the waiting area and calculate the total items in the waiting area
    for(Map.Entry<String, Map<String, Integer>> waitingCustomer : waitingArea.entrySet()){
      teaWaiting+=waitingCustomer.getValue().get("tea");
      coffeeWaiting+=waitingCustomer.getValue().get("coffee");
    }
    System.out.println("> " + teaWaiting + " tea(s)\n" + "> " +coffeeWaiting + " coffee(s)");
    // go through the brewing hashmap to count total teas and coffees... 
    System.out.println("Items in the Brewing Area: ");
    for(Map.Entry<String, Integer> item: brewingArea.entrySet()){ 
      System.out.println("> " + item.getValue() 
      + " "+item.getKey() + "(s)");
    }
    // .... 
    System.out.println("Items in the Tray Area: ");
    for(Map.Entry<String, Map<String, Integer>> trayItems: tray.entrySet()){
      trayTea+= trayItems.getValue().get("tea");
      trayCoffee+= trayItems.getValue().get("coffee");
    }
    System.out.println("> " + trayTea + " tea(s)\n" + "> " +trayCoffee + " coffee(s)");
    System.out.println("--------------------------------");
    }
  }
}