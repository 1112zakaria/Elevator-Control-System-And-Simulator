package main.java.elevator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import main.java.SimulatorConfiguration;
import main.java.UDPClient;
import main.java.dto.AssignedElevatorRequest;
import main.java.dto.ElevatorRequest;
import main.java.dto.ElevatorStatus;
import main.java.elevator.state.ElevatorState;
import main.java.elevator.state.TimeoutEvent;
import main.java.gui.LogConsole;

/**
 * Entity class for Elevator.
 * @author Zakaria Ismail
 */
public class ElevatorContext {
	private int id;
	private List<ElevatorRequest> externalRequests;
	private List<ElevatorRequest> internalRequests;
	private ElevatorState currentState;
	private int currentFloor;
	private Motor motor;
	private Direction direction;
	private Door door;
	private HashMap<Integer, Boolean> elevatorButtonBoard = new HashMap<>();
	private Timer timer;
	
	private ElevatorSubsystem elevatorSubsystem;
	//private JTextArea logConsole;
	private LogConsole logConsole;
	
	/**
	 * Constructor for Elevator Context
	 * @param subsystem ElevatorSubsystem, the elevator subsystem
	 * @param id int, the elevator id
	 */
	public ElevatorContext(ElevatorSubsystem subsystem, int id) {
		elevatorSubsystem = subsystem;
		
		// Initialize context
		this.id = id;
		currentFloor = 1;
		externalRequests = Collections.synchronizedList(new ArrayList<ElevatorRequest>());
		internalRequests = Collections.synchronizedList(new ArrayList<ElevatorRequest>());
		setDoors(Door.OPEN);
		setDirection(Direction.IDLE);
		setMotor(Motor.IDLE);
		
		for (int i = 1; i <= elevatorSubsystem.getConfig().NUM_FLOORS; i ++) {
			elevatorButtonBoard.put(i, false);
		}

		logConsole = new LogConsole(String.format("Elevator#%d", id));
		
	}
	
	/**
	 * Start the threads
	 */
	public void startElevator() {
		// XXX: make this a run() function? something to think about...
		// MUST CALL THIS FUNCTION TO START STATE MACHINE		
		currentState = ElevatorState.start(this);
		printLog(this.toString());
		// notify starting position
		notifyArrivalSensor();
	}
	
	/**
	 * Adding requests from outside of the elevators
	 * @param request
	 */
	public void addExternalRequest(ElevatorRequest request) {
		externalRequests.add(request);
	}
	
	/**
	 * Handle the request state
	 * @param request
	 */
	public void onRequestReceived(ElevatorRequest request) {
		addExternalRequest(request);
		synchronized (currentState) {
			printLog(String.format("Elevator#%d Event: Request Received", id));
			printLog(String.format("Elevator#%d will handle request going %s from floor %d to floor %d at %s" ,
					id , request.getDirection(), request.getSourceFloor(), request.getDestinationFloor(), request.getTimestamp()));
			currentState = currentState.handleRequestReceived(request);
			printLog(this.toString());
			// update view...
			notifyArrivalSensor();
		}
	}
	
	/**
	 * Timeout the event
	 * @param event
	 */
	public void onTimeout(TimeoutEvent event) {
		synchronized (currentState) {
			//System.out.println(String.format("Elevator#%d %s", id, event));
			printLog(String.format("Elevator#%d Event: Timeout", id));
			currentState = currentState.handleTimeout();
			printLog(this.toString());
			// update view...
			notifyArrivalSensor();
		}
	}
	
	/**
	 * Loading passengers method
	 */
	public void loadPassengers() {
		// when passengers are loaded, press button
		// external requests @ current floor are moved to internal requests		
		synchronized (externalRequests) {		
			ElevatorRequest req;
			List<ElevatorRequest> toRemove = new ArrayList<>();
			
			for (int i=0; i<externalRequests.size(); i++) {
				req = externalRequests.get(i);
				if (req.getSourceFloor() == currentFloor) {
					// FIXME: shouldn't it check for same direction?
					toRemove.add(req);
					internalRequests.add(req);
					pressElevatorButton(req.getDestinationFloor());
				}
			}
			externalRequests.removeAll(toRemove);
		}
	}
	
	/**
	 * Load single passenger if at floor & is going in same direction
	 * @param request	ElevatorRequest, passenger request to board
	 * @return boolean, request was boarded onto elevator
	 */
	public boolean loadPassengers(ElevatorRequest request) {
		if (request.getSourceFloor() == currentFloor && request.getDirection() == direction) {
			externalRequests.remove(request); // already sync'd
			internalRequests.add(request);
			return true;
		}
		return false;
	}
	
	/**
	 * Unload passenger
	 */
	public void unloadPassengers() {
		// clear button at current floor
		// internal requests @ current floor are removed...
		// removed internal requests are sent to scheduler as completed requests
		ElevatorRequest req;
		List<ElevatorRequest> toRemove = new ArrayList<>();
		for (int i=0; i<internalRequests.size(); i++) {
			req = internalRequests.get(i);
			if (req.getDestinationFloor() == currentFloor) {
				//internalRequests.remove(req);
				toRemove.add(req);
				printLog("Completed ElevatorRequest... sending back to scheduler: " + req);
				elevatorSubsystem.sendCompletedElevatorRequest(req);
			}
		}
		internalRequests.removeAll(toRemove);
		clearElevatorButton(currentFloor);
	}
	
	/**
	 * Update the lamp to true when the floor button is pressed
	 * @param floor
	 */
	private void pressElevatorButton(int floor) {
		elevatorButtonBoard.put(floor, true);
	}
	
	/**
	 * Update the lamp to false when the floor button is pressed
	 * @param floor
	 */
	private void clearElevatorButton(int floor) {
		elevatorButtonBoard.put(floor, false);
	}
	
	/**
	 * Set timer method
	 * @param task
	 * @param delay
	 */
	public void setTimer(TimerTask task, int delay) {
		if (timer != null) {
			// a timer is already set... call killTimer() first
			return;
		}
		
		if (!elevatorSubsystem.getConfig().TEST_MODE) {
			timer = new Timer();
			timer.schedule(task, delay);
		}
	}
	
	/**
	 * Kill timer method
	 */
	public void killTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	/**
	 * Setter for direction
	 * @param d
	 */
	public void setDirection(Direction d) {
		// TODO: set the direction lamps here
		direction = d;
	}
	
	/**
	 * Setter for door
	 * @param d
	 */
	public void setDoors(Door d) {
		door = d;
	}
	
	/**
	 * Setter for motor
	 * @param m
	 */
	public void setMotor(Motor m) {
		motor = m;
	}
	
	/**
	 * Increment the floor when it gets to the new floor
	 * @return true if the floor is updated else false
	 */
	public boolean incrementCurrentFloor() {
		// TODO: add condition checks and throw exception
		// if invalid floor?... the state machine should not
		// allow transition to MOVING state if floor above
		// doesn't exist...		
		if (currentFloor + 1 <= elevatorSubsystem.getConfig().NUM_FLOORS) {
			currentFloor++;
			return true;
		}
		return false;
	}
	
	/**
	 * Decrement the floor when it gets to the new floor
	 * @return true if the floor is updated else false
	 */
	public boolean decrementCurrentFloor() {
		if (currentFloor - 1 >= 1) {
			currentFloor--;
			return true;
		}
		return false;
	}
	
	/**
	 * Getter for configuration of this class
	 * @return configuration
	 */
	public SimulatorConfiguration getConfig() {
		return elevatorSubsystem.getConfig();
	}
	
	/**
	 * Overriding toString method
	 */
	@Override
	public String toString() {
		String externalReqStr = "";
		String internalReqStr = "";
		synchronized (externalRequests) {
			for (int i = 0; i < externalRequests.size(); i ++) {
				externalReqStr += externalRequests.get(i) + ", ";
			}	
		}
		synchronized (internalRequests) {
			for (int i = 0; i < internalRequests.size(); i ++) {
				internalReqStr += internalRequests.get(i) + ", ";
			}
		}
		
		return String.format(
				"Elevator#%d {CurrentFloor: %d, Current State: %s, Direction: %s, Motor: %s, Door: %s}"
				+ " queued for pickup: {%s}, carrying: {%s}",
				id, currentFloor, currentState, direction, motor, door, externalReqStr, internalReqStr);
	}

	/**
	 * Getter for elevator id
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Getter for direction
	 * @return direction
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Get total number of requests
	 * @return number of requests 
	 */
	public int getNumRequests() {
		// i should probably synchronize this...
		return internalRequests.size() + externalRequests.size();
	}
	
	/**
	 * Getter for external requests
	 * @return external requests
	 */
	public List<ElevatorRequest> getExternalRequests() {
		return externalRequests;
	}
	
	/**
	 * Getter for internal requests
	 * @return internal requests
	 */
	public List<ElevatorRequest> getInternalRequests() {
		return internalRequests;
	}
	
	/**
	 * Get the Elevator Lamp Light status
	 * @param floor the button number 
	 * @return boolean, true when lamp is on, false when lamp is off
	 */
	private boolean getElevatorLampStatus(Integer floor) {
		return elevatorButtonBoard.get(floor);
	}
	
	/**
	 * Method that getting all the selected floors in the elevator
	 * @return a hashmap containing the all the selected floors
	 */
	private ArrayList<Integer> getAllSelectedFloors(){
		ArrayList<Integer> allSelectedFloors = new ArrayList<>();
		for (Integer floorNumber : elevatorButtonBoard.keySet()) {
			if (elevatorButtonBoard.get(floorNumber)) {
				allSelectedFloors.add(floorNumber);
			}
		}
		// could return a null hashmap
		return allSelectedFloors;
	}

	/**
	 * Getter for current floor
	 * @return current floor
	 */
	public int getCurrentFloor() {
		return currentFloor;
	}
	
	public Direction calculateNextDirection() {
		// approach:
		// if going up, you want to keep going up
		//	stop going up when there are no more requests above you going UP
		// if going down, you want to keep going down
		//	stop going down when there are no more requests below you going DOWN
		// if idle, ... TBD
		Direction directionIfIdle;
		boolean continueSweepingUp, continueSweepingDown;
		
		continueSweepingUp = shouldContinueSweepingUp();
		continueSweepingDown = shouldContinueSweepingDown();
		directionIfIdle = determineNextDirection();
		
		switch (direction) {
		case UP:
			if (continueSweepingUp) return Direction.UP;
			if (continueSweepingDown) return Direction.DOWN;
			break;
		case DOWN:
			if (continueSweepingDown) return Direction.DOWN;
			if (continueSweepingUp) return Direction.UP;
			break;
		case IDLE:
			// TODO: pick based on # of jobs? copying UP for now
			// to keep as simple as possible... "when picking up"
			// new passenger(s) from an idle an state, "ignore"
			// jobs that are not going in the direction of the passenger
			// that you are picking up
			break;
		}
		return Direction.IDLE;
	}
	
	private boolean shouldContinueSweepingUp() {
		// check internal (using button board) and external
		ArrayList<Integer> selectedFloors = getAllSelectedFloors();
		
		for (int selectedFloor : selectedFloors) {
			if (selectedFloor > currentFloor) {
				return true;
			}
		}
		
		synchronized (externalRequests) {
			for (ElevatorRequest pendingReq : externalRequests) {
				if (pendingReq.getSourceFloor() > currentFloor && pendingReq.getDirection() == Direction.UP) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean shouldContinueSweepingDown() {
		ArrayList<Integer> selectedFloors = getAllSelectedFloors();
		
		for (int selectedFloor : selectedFloors) {
			if (selectedFloor < currentFloor) {
				return true;
			}
		}
		
		synchronized (externalRequests) {
			for (ElevatorRequest pendingReq : externalRequests) {
				if (pendingReq.getSourceFloor() < currentFloor && pendingReq.getDirection() == Direction.DOWN) {
					return true;
				}
			}
		}
		return false;
	}
	
	private Direction determineNextDirection() {
		ElevatorRequest nextRequest;
		
		synchronized (externalRequests) {
			if (externalRequests.size() > 0) {
				nextRequest = externalRequests.get(0);
			} else {
				return Direction.IDLE;
			}
		}
		if (currentFloor > nextRequest.getSourceFloor()) {
			return Direction.DOWN;
		}
		if (currentFloor < nextRequest.getSourceFloor()) {
			return Direction.UP;
		}
		return Direction.IDLE;	// FIXME: double-check. 
	}
	
	public Direction calculateNextHomingDirection() {
		// approach: if elevator is considering homing state, it would
		// have no more requests to sweep in the current direction and no more
		// to sweep in the opposite direction at its current position
		// the elevator must therefore identify whether it could "home" toward
		// the same direction that it was travelling so that it could start serving
		// requests in the opposite direction... TBD
		boolean existsReqAbove = this.existsHomingExternalRequestsAbove();
		boolean existsReqBelow = this.existsHomingExternalRequestsBelow();
		switch(direction) {
		case UP: 
			if (existsReqAbove) return Direction.DOWN;
			if (existsReqBelow) return Direction.UP;
			break;
		case DOWN: 
			if (existsReqBelow) return Direction.UP;
			if (existsReqAbove) return Direction.DOWN;
			break;
		default:
			// FIXME: this could cause problems....
		}
		return Direction.IDLE;
	}
	
	public boolean shouldElevatorStop() {
		// check that there exists internal request @ current floor
		// or external request @ current floor and in current direction
		
		// new detected case: if the elevator car is empty and the elevator
		// is moving, then disregard having to check that there is a direction match? (idk if valid...)
		
		// case: if going some direction & there's a request at currentFloor & there's no more requests
		// if you were to go in the same direction, then stop for the request @ the current floor?
		//	-> can you stop sweeping? violate the check of having to accept a request going in same direction
		//	-> essentially, the goal is to sweep until you hit the bottom-most or top-most request source?
		// FIXME: consider stopping when you are at either lowest floor going down or highest floor going up -> TEST THIS
		if (getElevatorLampStatus(currentFloor)) {
			return true;
		}
		
		synchronized (externalRequests) {
			boolean interceptElevator = false;
			boolean existsAboveReq = existsSweepingExternalRequestsAbove();
			boolean existsBelowReq = existsSweepingExternalRequestsBelow();
			for (ElevatorRequest pendingReq : externalRequests) {
				if (pendingReq.getSourceFloor() == currentFloor && (pendingReq.getDirection() == direction || direction == Direction.IDLE)) {
					// there exists a pending req that is "on the way" - continue sweeping
					// FIXME: is it correct to add a Direction.IDLE condition?
					return true;
				}
				if (internalRequests.size() == 0 && pendingReq.getSourceFloor() == currentFloor) {
					// no one is in the car and there is a request at this floor
					if (!existsAboveReq && direction == Direction.UP || !existsBelowReq && direction == Direction.DOWN) {
						// if there are no jobs todo in the direction that you are going, then you might as well pick them up
						interceptElevator = true;
					}
				}
				
			}
			
			if (interceptElevator) {
				return true;
			}
//			// case: elevator is trying to beeline reach a mf floor
//			if (internalRequests.size() == 0 && externalRequests.size() == 1 && externalRequests.get(0).getSourceFloor() == currentFloor) {
//				// there is no one in the elevator car and you have come across a floor w/ a pending request & there are no other requests
//				return true;
//			}
		}
		return false;
	}
	
	public boolean shouldElevatorStop(ElevatorRequest request) {
		if (request.getSourceFloor() == currentFloor && (request.getDirection() == direction 
				|| direction == Direction.IDLE)) {
			return true;
		}
		return false;
	}
	
	private boolean existsSweepingExternalRequestsAbove() {
		synchronized (externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() > currentFloor && req.getDirection() == Direction.UP) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean existsSweepingExternalRequestsBelow() {
		synchronized (externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() < currentFloor && req.getDirection() == Direction.DOWN) {
					return true;
				}
			}
		}
		return false;
	}
	
	public ElevatorError isAtErrorFloor() {
		ElevatorError error = null, reqError;
		for (int i=0; i<internalRequests.size(); i++) {
			reqError = internalRequests.get(i).getElevatorError();
			if (reqError != null) {
				// XXX: i have to do this because can't use switch/case w/ null
				switch (reqError) {
					case ELEVATOR_STUCK: return ElevatorError.ELEVATOR_STUCK;
					case DOORS_STUCK: error = ElevatorError.DOORS_STUCK;
				}
			}
		}
		return error;
	}
	
	public boolean shouldElevatorSweep() {
		// check that there exists request to serve up?
		// use .shouldElevatorStop() logic
		//return !shouldElevatorStop();
		boolean existsReqAbove = this.existsSweepingExternalRequestsAbove();
		boolean existsReqBelow = this.existsSweepingExternalRequestsBelow();
		
		if (existsReqAbove && direction == Direction.UP || existsReqBelow && direction == Direction.UP) {
			return true;
		}
		return false;
	}
	
	public boolean shouldElevatorHome() {
		// check that there exists a request to travel to the other extreme
		// of the shaft
		// assumes that elevator is empty, only looks at external requests
		boolean existsReqAbove = existsHomingExternalRequestsAbove();
		boolean existsReqBelow = existsHomingExternalRequestsBelow();
		
		if (existsReqAbove && direction == Direction.DOWN || existsReqBelow && direction == Direction.UP) {
			return true;
		}
		return false;
	}
	
	private boolean existsHomingExternalRequestsAbove() {
		synchronized(externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() > currentFloor && req.getDirection() == Direction.DOWN) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean existsHomingExternalRequestsBelow() {
		synchronized(externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() > currentFloor && req.getDirection() == Direction.UP) {
					return true;
				}
			}
		}
		return false;
	}
	
	// XXX: this method name is misleading...
	private void notifyArrivalSensor() {
		elevatorSubsystem.notifyContextUpdate(this);
		
	}
	
	public void returnExternalRequests() {
		synchronized (externalRequests) {
			printLog("Elevator crashed: returning externalRequests to scheduler");
			elevatorSubsystem.returnElevatorRequests(externalRequests);
			externalRequests.removeAll(externalRequests);
		}
	}
	
	public ElevatorState getCurrentState() {
		return currentState;
	}
	
	public Door getDoors() {
		return door;
	} 
	
	public Motor getMotor() {
		return motor;
	}
	
	private void printLog(String message) {
		System.out.println(message);
		logConsole.appendLog(" " + message + "\n");
	}
	
	public static void main(String[] args) throws ParseException, UnknownHostException, IOException, InterruptedException {
		// small visual test
		System.out.println("--small little elevator context test");
		SimulatorConfiguration sc = new SimulatorConfiguration("./src/main/resources/config.properties");
		//s.startElevatorSubsystem(); TODO: update this lil test
		UDPClient testServer = new UDPClient();
		UDPClient arrivalNotifRcv = new UDPClient(sc.SCHEDULER_ARRIVAL_REQ_PORT);
		Thread notifRcvThread;
		AssignedElevatorRequest testRequest = null;

		Thread.sleep(1000);
		testRequest = new AssignedElevatorRequest(
					1, "07:01:15.000", 3, Direction.UP, 5
				);
		testServer.sendMessage(testRequest.encode(), sc.ELEVATOR_SUBSYSTEM_HOST, sc.ELEVATOR_SUBSYSTEM_REQ_PORT);
		
		notifRcvThread = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramPacket packet;
				ElevatorStatus status;
				while (true) {
					packet = arrivalNotifRcv.receiveMessage();
					try {
						status = ElevatorStatus.decode(UDPClient.readPacketData(packet));
						System.out.println(status);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					
				}
				
			}
		});
		notifRcvThread.start();
	}
}
