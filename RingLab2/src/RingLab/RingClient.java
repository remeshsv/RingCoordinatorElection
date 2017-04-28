package RingLab;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * Student Name : Remesh Sreemoolam Venkitachalam
 * Student ID: 1001414827
 * 
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text area to see the communication between processes, 
 * and a crash button to crash processes
 * and a revive button to bring back these processes to life.
 * 
 * references - http://www2.cs.uregina.ca/~hamilton/courses/330/notes/distributed/distributed.html
 *              github - ksashikumar, sam benison
 *
 */


public class RingClient {
	
	BufferedReader in;
    PrintWriter out;
    private static int port = 8080;
    private static int k=1;
    public int procNo = 0, crashedPort = 0;  // current process, crashed process
    public static boolean isCrashed=false;
    static processThread thread = new processThread();
    public static int coordinator = 0;   // current coordinator
    
    public static ServerSocket socket;
    JFrame frame = new JFrame("Ring Election");    // The Gui frame for each fprocess
    JTextArea messageArea = new JTextArea(25, 40); // Shows what's going on with the processes.
    JLabel textField = new JLabel("Process", 10); // Enter the process to be modified
    public JButton btnCrash = new JButton("Crash"); // create a button to crash processes in the UI
	public JButton btnRevive = new JButton("Revive"); // create a button to revive processes in the UI
	
	public RingClient() {
		
		//Creating the GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), "North");
        frame.getContentPane().add(textField, "Center");
        frame.getContentPane().add(btnCrash, "West");
        frame.getContentPane().add(btnRevive, "East");
        frame.pack();
        btnRevive.setEnabled(false);
        
        //Functionalities for the crash button
        btnCrash.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				isCrashed = true; // Change the boolean value when crashed
				
				btnCrash.setEnabled(false); // set the Crash button to be disabled
				btnRevive.setEnabled(true); // set the Revive button to be enabled
			
				try {
					thread.suspend();
					socket.close();
					crashedPort = 8080 + getProcNo(); // Assign the crashed port
					messageArea.append("\n Process crashed!");
				} catch (IOException ex) {  // Catch actions if not crashed
					isCrashed = false;
					messageArea.append("\n Unable to crash this processor, Please try again later..!!!");
				}
				
												
				System.out.println("Clicked" + crashedPort);
				
			}
        				
	});
        
              //Functionalities for the 'Revive' button when clicked
     			btnRevive.addActionListener(new ActionListener() {
     				
     				@Override
     				public void actionPerformed(ActionEvent e) {
     					// TODO Auto-generated method stub
     					
     					isCrashed = false;
     					System.out.println("Clicked" + crashedPort);
     					
     					btnCrash.setEnabled(true); // set the Crash button to be enabled
     					btnRevive.setEnabled(false); // set the Reset button to be disabled
     					
                         // Reset the crashed process
     					try {
     						socket = new ServerSocket(crashedPort);
     						thread.restart(crashedPort, procNo, socket); //keep thread up to date
     						thread.resume();
     						messageArea.append("\n Thread Restarted!  \n Begin Election...");
     						crashedPort = 0;
     					} catch (IOException ex) {  // Exception to shut down the process when crashed port is unavailable
     						System.out.println("\n Port not available for Process Restart..!! \n Process Restart failed..!!");
     						System.exit(1);
     					}
     					
     					//isIdle = false; // boolean value change
     					
     					// Initiate the election whenever the process is restarted
     					 String currToken = "ELECTION " + procNo;
     					  startElection(currToken, procNo+1);
     					
     					
     					System.out.println("Clicked");
     										
     				}
     			});
	}
	
	public void startElection(String token, int process) {
    	if(process>6) {
        	process = process - 6;
        }
        try {
        	// Create a socket based on the process number
            Socket socket = new Socket("127.0.0.1", 8080+process);
            // Create the PrintWriter for the socket
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            // send the token into the printwriter for that particular socket
            out.println(token);
            out.flush();  // flush the printwriter output
            out.close();  // close the printwriter
            socket.close();  // close the socket
        } catch(Exception ex) {
        	startElection(token, process+1); // inform to next next if the next is unavailable
        }
    }
	
	public int getProcNo(){
		return this.procNo;
	}
	
	private void runProcess(RingClient client) throws IOException {
		System.out.println("Success");
				
		for (int i=1;;i++)
			{
			  port=port+k;
			try{
				socket = new ServerSocket(port);
			    procNo = port-8080;
		        textField.setText("Process " + procNo);
		        messageArea.append("Connected to the port : " + port);
		        //k++;
		        socket.close();
		        break;
			}catch (IOException e){
				System.out.println(port + " is in use...");
			}
			}
			try{
				socket = new ServerSocket(port);
				System.out.println("Server for" + port);
			    thread.start(); // Start the thread
	            thread.init(port, procNo, client, socket); //Provide thread with initializations
	            if(port==8086)
	            	{String currToken = "ELECTION " + procNo;
			         startElection(currToken, procNo+1);
	            	}
			}catch (IOException e) {
				System.out.println("Not able to connect to port " + port);
				System.exit(1);
			}
	        
			
		
        
//        in = new BufferedReader(new InputStreamReader(
//            socket.getInputStream()));
//        out = new PrintWriter(socket.getOutputStream(), true);
		
	}
	
	public static void main(String[] args) throws Exception {
		
		//for(int i=0;i<6;i++){
		RingClient client = new RingClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
        client.runProcess(client);
		//}
       
	}
}

class processThread extends Thread {
	
	public int processNo;
	public int portNo;
	public RingClient ring;
	public ServerSocket serSoc;
	public Socket socket;	
	public BufferedReader in;
    public PrintWriter out;
    public static int coordinator = 0;
	
public void init(int portNumber, int processNumber ,RingClient ring, ServerSocket servSoc){
		
		System.out.println(portNumber);
		this.portNo = portNumber; // assign the port number
		this.processNo = processNumber; // assign the process number 
		this.ring = ring; // assign the frame
		this.serSoc = servSoc;	 // assign the socket
		ring.messageArea.append("\n This process has started!");
		
	}

public void restart(int portNumber, int processNumber , ServerSocket servSoc){
	
	System.out.println(portNumber);
	this.portNo = portNumber; // assign the port number
	this.processNo = processNumber; // assign the process number 
	this.serSoc = servSoc;	 // assign the socket
	
}

public void run(){
		
	while(true){
		try {
			socket = serSoc.accept();
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//		        out = new PrintWriter(socket.getOutputStream(), true);
			
			String token = in.readLine(); //Read input into token
			
			// Functionalities of ELECTION process
			if (token.startsWith("ELECTION")){
				ring.messageArea.append("\nElection Process - Token : " + token.substring(8));
				
				int pos = 9;
				System.out.println("char " + token.charAt(pos));
				System.out.println("process " + processNo);
				int process = Character.getNumericValue(token.charAt(pos));
				if(process==processNo){
					int[] processes = new int[10];
				    processes[0] = processNo;
				    int cnt = 1;
				    //pos++;
				    while(!(token.substring(pos).isEmpty()) && cnt<6) {
				    	System.out.println(token.substring(pos));
				    	process = Character.getNumericValue(token.charAt(pos));
				    	processes[cnt] = process;
				        cnt++;
				        pos++;
				    }
				  System.out.println(processes[0]);
				    coordinator=electCoordinator(processes, processNo);
				    ringProceed(processNo+1, coordinator); // then
				    				    
				}else {
					// if not the initiated process, just add the process number to the token and pass the token to next processor
					Thread.sleep(2000);
					ring.messageArea.append("\nToken passed to next process");
				  	passToken(token+processNo, processNo+1);
				}
				
	 }else if (token.startsWith("COORDINATOR")){           // Functionalities of finding co-ordinator
		 Thread.sleep(2000);
		 ring.messageArea.append("\nCo-ordinator : " + token.substring(11));
		 int coord = Character.getNumericValue(token.charAt(11));
		 System.out.println("co-ord:" + coordinator);
//		 if (processNo == coord){ // if the elector is the coordinator
//     		System.out.println("im here");
//     		ringProceed(processNo+1); // then start the alive message from the next available processor
//     	} else { // or else start the alive message from the current processor
//     		System.out.println("im there");
//     		String tokenAlive = "ALIVE " + coordinator + processNo;
//     		verifyAlive(ring.coordinator, processNo+1, processNo+1, tokenAlive);
//     	}
		 
	 }else if(token.startsWith("NEXT_ALIVE")){    // Choosing the next live process to pass token
		 int coord = Character.getNumericValue(token.charAt(10));
		 int next = Character.getNumericValue(token.charAt(11));
		 System.out.println("Alive in : " + next);
		 System.out.println("coordinator here is " + coord);
		 Thread.sleep(2000);
		 if(next == processNo){
			 String tokenNext = "ALIVE" + coord + processNo;
     		verifyAlive(coord, processNo+1, processNo, tokenNext);			 
		 }
		 
	 }else if(token.startsWith("ALIVE")){ // Passing the message of coordinator being active
	  
		 try{
			 int tcoord = Character.getNumericValue(token.charAt(5));
		 	 int tproc = Character.getNumericValue(token.charAt(6));
		 System.out.println(processNo);
		 
		 if(processNo == tproc && processNo != tcoord){
			 System.out.println(token.substring(7));
			 if(token.substring(7).equals("OK")){
				 
				 ring.messageArea.append("\n Alive Coordinator");				 
				 if (processNo+1 == coordinator){
					 ringProceed(processNo+2, tcoord);
             	} else {
             		ringProceed(processNo+1, tcoord);
             	}
				 
			 }else{
				 ring.messageArea.append("\nCo-ordinator crashed! Re-election!");
				 String currToken = "ELECTION " + processNo;
				 ring.startElection(currToken, processNo+1);
			 }
			 
		 }else if(processNo == tproc && processNo == tcoord){
			 ringProceed(processNo+1, tcoord);			 
		 }else if(processNo == tcoord){
			 ring.messageArea.append("\n Alive - coordinator");
			 token = token + "OK";
			 System.out.println(token);
			 Thread.sleep(1000);
			 passToken(token, processNo+1);
		 }else{
			 ring.messageArea.append("\n Alive Coordinator");
			 passToken(token, processNo+1);
		 }
		 
		 
	 }catch(Exception e){
		 ringProceed(processNo+1, coordinator);
	 }
	 }
		
	}catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		ringProceed(processNo+1, coordinator);
		} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}finally {
        try {
        	in.close();
        } catch (IOException ex) {
        	System.out.println("Exception : " + ex);
        }
    }
}
}


//Function to elect the coordinator
private int electCoordinator(int[] processes, int elector) {
    int newCoord = processes[0];
  //Check the first process number and compare with other live processes and elect Coordinator with highest process number
    for(int i = 1; i<processes.length; i++) {
        if(processes[i]>newCoord) {					
        	newCoord = processes[i];
        }
    }  
    
    // assign the variables with the new coordinator value
    ring.coordinator = newCoord;
    coordinator = newCoord;
    ring.messageArea.append("\nNew Co-ordinator : " + ring.coordinator);
    if(ring.coordinator != 0) {
    	// Announce the coordinator to other processes
    
    	informCoordinator(newCoord, processNo+1, elector);
    }
    return newCoord;
}

// Function to pass the coordinator information
private void informCoordinator(int coord, int procNo, int elector){
System.out.println("found" + elector);
if(procNo>6) {
	procNo = procNo - 6;
}

String token = "COORDINATOR" + coord;
try {
    socket = new Socket("127.0.0.1", 8080+procNo);
    out = new PrintWriter(socket.getOutputStream());
    out.println(token);
    out.flush();
    out.close();
    socket.close();
    if(elector!=procNo){
    	informCoordinator(coord, procNo+1, elector); 
    } 
    
}catch(Exception ex) {
	informCoordinator(coord, procNo+1, elector); // inform to next next if the next one was unavailable
} 
}
    
//Pass tokens to next live processes
private void passToken(String token, int procNo){
        if(procNo>6) {
        	procNo = procNo - 6;
        }
        try {
            socket = new Socket("127.0.0.1", 8080+procNo);
            out = new PrintWriter(socket.getOutputStream());
            out.println(token);
            out.flush();
            out.close();
            socket.close();
        } catch(Exception ex) {
        	passToken(token, procNo+1); // send it to next next if the next one was unavailable
        }
    }
    
//Method to find the process that is to be passed the token
private void verifyAlive(int coordID, int procID, int electedID, String token) {
	System.out.println("so im here now" + coordID);
		long time = 3000;
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(procID>6) {
        	procID = procID - 6;
        }
        try {
            socket = new Socket("127.0.0.1", 8080+procID);
            out = new PrintWriter(socket.getOutputStream());
            out.println(token);
            out.flush();
            out.close();
            socket.close();
            //ring.currentAlive = electedID;
        } catch(Exception ex) {
        	verifyAlive(coordID, procID+1, electedID, token); // pass to next next if next was unavailable
        }
	//}
}

// Method to initiate the NEXT_ALIVE message from the current processor
public void ringProceed(int nextProc, int coord) {
	System.out.println("inside proceed" + nextProc);
	System.out.println("inside proceed coord" + coord);
	if(nextProc>6) {
		nextProc = nextProc - 6;
    }
	String token = "NEXT_ALIVE" + coord + nextProc;
	System.out.println("inside proceed" + nextProc);
    try {
        Socket socket = new Socket("127.0.0.1", 8080+nextProc);
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println(token);
        out.flush();
        out.close();
        socket.close();
    } catch(Exception ex) {
    	// forward to next next if the next one was unavailable
    	if(nextProc+1 == coordinator) {
    		ringProceed(nextProc+2, coord);
    	} else {
    		ringProceed(nextProc+1, coord);
    	}
    }
}



}
	

