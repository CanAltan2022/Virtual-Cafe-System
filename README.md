# Virtual-Cafe-System# Virtual Cafe 

> **Virtual Cafe** is a program which utilises a server-client architecture.It essentially simulates/mimics a real cafe which serves tea and coffee.   

> ### To run the program: 
> * Have JDK installed on your machine/virtual machine. 
> * Have multiple terminal windows open
> * Download and place the java files, Barista.java and Customer.java in an apropriate directory
> * go into the directory where you placed the files using the terminal

> * When you are in the directory where the .java files are located:
> * #### Compile the files by running following commands in the terminal:
> * javac -cp "." Barista.java
> * javac -cp "." Customer.java

> * #### To start the program run the following commands in separate terminals: 
> * java -cp "." Barista.java  -->Starts the server(only run once per session)
> * java -cp "." Customer.java --> 
    for connecting to cafe server as a customer(to create multiple client run in different terminals)

> ### Overview of Cafe Commands
> * ORDER STATUS: When used the customer's current order status is shown --> If the customer hasn't ordered yet --> a proper response is given. 
> * EXIT: Used for exiting the cafe -> customer is terminated when this command is sent. 
> * COLLECT: Used for collecting orders -> if there is no order, the user is greeted with an appropriate response message. 
> * MENU: Shows what items can be ordered and what format must be used
> * ORDER: For ordering tea or coffee or both with the format, ORDER quantity item -> if you want to order multiple items simply use an "and" and then add the quantity item.

### Known Issues: 
> * Sometimes when a customer exits before their order is prepared(in the brewing area) the program moves on to the next customers order a little later than expected(this is minor delay- usually takes about 1-3 seconds)

