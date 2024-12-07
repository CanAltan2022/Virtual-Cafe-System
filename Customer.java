// Student Name: Can Altan
// Student ID: 2316850
import java.io.*; import java.net.Socket; import java.util.Scanner;

public class Customer implements AutoCloseable{
    private final Scanner reader; private final PrintWriter writer;
    private final int PORT = 8888; private final Socket socket;
    private static boolean regularExit = false; // -> for sigINT exit check

    public Customer(String customerName) throws Exception {
        socket = new Socket("localhost", PORT);
        reader = new Scanner(socket.getInputStream());
        writer = new PrintWriter(socket.getOutputStream(), true);

        writer.println(customerName);
        String response = reader.nextLine();
        if (!response.equalsIgnoreCase("SUCCESS")){
            throw new Exception("Server rejected the connection" + response);
        }
        }
  
        public String sendCommands(String command) {
            writer.println(command); // Send command to the server
            //writer.flush(); // Ensure the command is sent immediately
            StringBuilder response = new StringBuilder();
        
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (line.equals("END")) { // Check for the server's end-of-response mark
                    break;
                }
                response.append(line).append("\n");
            }
        
            return response.toString().trim(); // Return the entire response
        }

    @Override
    public void close(){
        reader.close();
        writer.close();
        }
    
    public static void main(String[] args) {
        // For SIGINT interruption cases. 
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (!regularExit){
            System.out.println("SIGINT was received. Exiting cafe ...");
           
        // perform the necessary deletions here
        }
        }));
        System.out.println("Please, Enter Your Name: ");
        try {
            Scanner in = new Scanner(System.in);
            String customerName = in.nextLine();
            try(Customer customer = new Customer(customerName)) {   
                System.out.println("Welcome To The Virtual Cafe!");
                while (true) {
                    System.out.println("Enter A Command: \nMENU, ORDER <quantity> <item>,\nORDER STATUS, COLLECT, EXIT");
                    String command = in.nextLine();
                    String response = customer.sendCommands(command);
                    System.out.println(response);
                    
                    if (command.equalsIgnoreCase("EXIT")){
                        regularExit =true;
                        break;
                        }
                    }
                 }
            
         
        } catch (Exception e) {System.out.println(e.getMessage());}
    }
}
         
    
    