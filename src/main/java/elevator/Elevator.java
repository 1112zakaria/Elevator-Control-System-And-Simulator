package main.java.elevator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.java.dto.ElevatorRequest;
import main.java.scheduler.Scheduler;

/**
 * Elevator class serves elevator requests from the Scheduler and stores in internal queue.
 * @author Trong Nguyen
 * @since 1.0, 02/04/23
 * @version 2.0, 02/27/23
 */
public class Elevator implements Runnable {
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	public static final int ELEVATOR_PORT = 69;
	private DatagramSocket dataSocket, ackSocket;
	
	private int id = 0;
	private Scheduler scheduler;
	private ElevatorState elevatorState;
	
	/**
	 * Main method for the Floor class.
	 * @param args, default parameters
	 */
	public static void main(String[] args) {
		new Thread(new Elevator()).start();
	}
	
	
	/**
	 * Constructor for Elevator class.
	 * @param id int, elevator id
	 * @param scheduler	Scheduler, scheduler object referenced by Elevator
	 */
	public Elevator() {
		this.id = id++;
		elevatorState = ElevatorState.Idle;
		logger.setLevel(Level.INFO);
		// Start of the program, the elevator should be in floor 1
		//scheduler.registerElevatorLocation(id, 1);
	}

	/**
	 * Elevator class run() implementation.
	 * Serves requests from the Scheduler until all requests have been served.
	 * java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		ElevatorRequest request = null;
		DatagramPacket reply = null;
		try {
			dataSocket = new DatagramSocket();
			ackSocket = new DatagramSocket();
			Thread.sleep(1000);
			while (true) {
				//if (scheduler.getRequestsQueue().size() >= 0) {
					//String elevatorStateStr = elevatorState.displayCurrentState(getElevatorId(), request);
					switch (elevatorState) {
						// Only State Moving and Stop only use request argument
						case Idle: {
							//System.out.println(elevatorStateStr);
							elevatorState = elevatorState.nextState();
							break;
						}
						case AwaitRequest: {
							//System.out.println(elevatorStateStr + " ------------------------------------------ \n");
							send();
							reply = receiveData();
							elevatorState = elevatorState.nextState();
							break;
						}
						case Moving: {
							//System.out.println(elevatorStateStr);
							// Note: Elevator needs to move to the floor that the users request the elevator
							// to pick up the users then move the the floor they want
							
							// Move from the current floor to the floor that request the elevator
							//if (scheduler.displayElevatorLocation(id) != request.getSourceFloor()) {
								//logger.info(String.format("Elevator %d is moving to floor %d to pick up the users", id , request.getSourceFloor()));
								//scheduler.movingTo(id, scheduler.displayElevatorLocation(id), request.getSourceFloor());
								logger.info(String.format("Elevator %d picked up the users, start doing the request", id));
							//}
							// Move from the picked up floor to the floor users want 
							//scheduler.movingTo(id, scheduler.displayElevatorLocation(id), request.getDestinationFloor());
							elevatorState = elevatorState.nextState();
							break;
						}
						case Stop: {
							//System.out.println(elevatorStateStr + "\n");							
							sendAck(reply);
							receive();
							System.out.println("--------------------------------------");
							elevatorState = elevatorState.nextState();
							break;
						}
						case DoorsOpen: {
							//System.out.println(elevatorState.displayCurrentState(getElevatorId(), request));
							elevatorState = elevatorState.nextState();
							break;
						}
						case DoorsClose: {
							//System.out.println(elevatorState.displayCurrentState(getElevatorId(), request));
							elevatorState = elevatorState.nextState();
							break;
						}
						default:
							break; 		
					}
				}
			//}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dataSocket.close();
			ackSocket.close();
			System.err.println(this.getClass().getName() + ": Program terminated.");
		}
	}
	
	
	/**
	 * Send request for data to Scheduler.
	 */
	private void send() {
		byte[] data = (this.getClass().getName() + ": Waiting for request...").getBytes();
		try {
			DatagramPacket sendPacket = new DatagramPacket(
					data, 
					data.length, 
					InetAddress.getLocalHost(), 
					ELEVATOR_PORT);
			dataSocket.send(sendPacket);
			printPacketContent(sendPacket, "Scheduler <- send()");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receive data from Scheduler.
	 * @return DatagramPacket, elevator requested from Scheduler
	 */
	public DatagramPacket receiveData() {
		byte[] data = new byte[100];
		DatagramPacket replyPacket = new DatagramPacket(data, data.length);
		try {
			System.out.println(this.getClass().getName() + ": Waiting...\n");
			dataSocket.receive(replyPacket);
			printPacketContent(replyPacket, "Scheduler -> reply(:request)");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return replyPacket;
	}	
	
	/**
	 * Send acknowledge to Scheduler.
	 * @param receiveHostPacket DatagramPacket, data packet received from request
	 */
	public void sendAck(DatagramPacket replyPacket) {
		//decodeData(replyPacket.getData());
		// Add verification of data method here
		//byte[] data = replyPacket.getData();
		DatagramPacket ackPacket = new DatagramPacket(
				replyPacket.getData(), 
				replyPacket.getLength(), 
				replyPacket.getAddress(), 
				replyPacket.getPort());
		try {
			ackSocket.send(ackPacket);
			printPacketContent(ackPacket, "Scheduler <- send(:ack)");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receive reply to acknowledge from Scheduler.
	 */
	public void receive() {
		byte[] data = new byte[100];
		DatagramPacket ackPacket = new DatagramPacket(data, data.length);
		try {
			System.out.println(this.getClass().getName() + ": Waiting...\n");
			ackSocket.receive(ackPacket);
			printPacketContent(ackPacket, "Scheduler -> reply()");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Decodes byte[] data into an ElevatorRequest object.
	 * @param message byte[], the encoded elevatorRequest
	 * @return elevatorRequest, ElevatorRequest obj
	 * @throws IOException
	 */
	private String decodeData(byte[] message) {
		//ElevatorRequest elevatorRequest = null;
		ByteArrayInputStream is = new ByteArrayInputStream(message);
		String reqStr = is.toString();
		System.out.println(reqStr);
		// Return string for now, but later convert to ElevatorRequest
		return reqStr;
	}
	
	/**
	 * Prints out the information it has put in the packet 
	 * both as a String and as bytes. 
	 * @param packet, DatagramPacket
	 * @param direction, String (i.e. received or sending)
	 */
	private void printPacketContent(DatagramPacket packet, String direction) {
		System.out.println(this.getClass().getName() + ": " + direction);
	    System.out.println("Address: " + packet.getAddress());
	    System.out.println("Port: " + packet.getPort());
	    int len = packet.getLength();
	    System.out.println("Length: " + packet.getLength());
	    System.out.print("Containing: ");
	    String packetStr = new String(packet.getData(), 0, len);
	    System.out.println(packetStr + "\n");
	    try {
	        Thread.sleep(1000);
	    } catch (InterruptedException e ) {
	        e.printStackTrace();
	        System.exit(1);
	    }
	}
	
	/**
	 * Get the elevator id.
	 * @return int, elevator id
	 */
	public int getElevatorId() {
		return this.id;
	}
	
	/**
	 * Get current elevator state
	 * @return elevatorState, current elevator state
	 */
	public ElevatorState getElevatorState() {
		return elevatorState;
	}

}
