package main.java.elevator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import main.java.SimulatorConfiguration;
import main.java.dto.ElevatorRequest;
import main.java.elevator.state.ElevatorState;
import main.java.elevator.state.TimeoutEvent;
import main.java.gui.LogConsole;

/**
 * Entity class for Elevator.
 * 
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
	private LogConsole logConsole;

	/**
	 * Constructor for Elevator Context
	 * 
	 * @param subsystem ElevatorSubsystem, the elevator subsystem
	 * @param id        int, the elevator id
	 */
	public ElevatorContext(ElevatorSubsystem subsystem, int id) {
		this.elevatorSubsystem = subsystem;
		this.id = id;
		currentFloor = 1;
		externalRequests = Collections.synchronizedList(new ArrayList<ElevatorRequest>());
		internalRequests = Collections.synchronizedList(new ArrayList<ElevatorRequest>());
		setDoors(Door.OPEN);
		setDirection(Direction.IDLE);
		setMotor(Motor.IDLE);
		for (int i = 1; i <= elevatorSubsystem.getConfig().NUM_FLOORS; i++) {
			elevatorButtonBoard.put(i, false);
		}
		logConsole = new LogConsole(String.format("Elevator#%d", id));
	}

	/**
	 * Start the elevator threads.
	 */
	public void startElevator() {
		currentState = ElevatorState.start(this);
		printLog(this.toString());
		notifyArrivalSensor();
	}

	/**
	 * Adding requests from outside of the elevators.
	 * 
	 * @param request ElevatorRequest, the elevator request object
	 */
	public void addExternalRequest(ElevatorRequest request) {
		synchronized (externalRequests) {
			externalRequests.add(request);
		}
		onRequestReceived(request);
	}

	/**
	 * Handle the request state upon receive.
	 * 
	 * @param request ElevatorRequest, the elevator request object
	 */
	public void onRequestReceived(ElevatorRequest request) {
		synchronized (currentState) {
			printLog(String.format("REQUEST_RECEIVED -- Elevator#%d", id));
			printLog(String.format("Elevator#%d will handle request going %s from floor %d to floor %d at %s", id,
					request.getDirection(), request.getSourceFloor(), request.getDestinationFloor(),
					request.getTimestamp()));
			currentState = currentState.handleRequestReceived();
			printLog(this.toString());
			notifyArrivalSensor();
		}
	}

	/**
	 * Handle a timeout the event.
	 * 
	 * @param event TimeoutEvent, the timeout event
	 */
	public void onTimeout(TimeoutEvent event) {
		synchronized (currentState) {
			//System.out.println(String.format("TIMEOUT_EVENT -- Elevator#%d", id));
			currentState = currentState.handleTimeout();
			printLog(this.toString());
			notifyArrivalSensor();
		}
	}

	/**
	 * Loading passengers method when passengers are loaded, press button external
	 * requests at current floor are moved to internal requests
	 */
	public void loadPassengers() {
		synchronized (externalRequests) {
			ElevatorRequest req;
			List<ElevatorRequest> toRemove = new ArrayList<>();
			for (int i = 0; i < externalRequests.size(); i++) {
				req = externalRequests.get(i);
				if (req.getSourceFloor() == currentFloor) {
					toRemove.add(req);
					internalRequests.add(req);
					pressElevatorButton(req.getDestinationFloor());
				}
			}
			externalRequests.removeAll(toRemove);
		}
	}

	/**
	 * Unload passenger method when clear button at current floor then internal
	 * requests at current floor are removed. Removes internal requests are sent to
	 * scheduler as completed requests.
	 */
	public void unloadPassengers() {
		ElevatorRequest req;
		List<ElevatorRequest> toRemove = new ArrayList<>();
		for (int i = 0; i < internalRequests.size(); i++) {
			req = internalRequests.get(i);
			if (req.getDestinationFloor() == currentFloor) {
				toRemove.add(req);
				printLog(String.format("REQUEST_COMPLETED -- %s", req));
				elevatorSubsystem.sendCompletedElevatorRequest(req);
			}
		}
		internalRequests.removeAll(toRemove);
		clearElevatorButton(currentFloor);
	}

	/**
	 * Update the lamp to true when the floor button is pressed.
	 * 
	 * @param floor int, the elevator floor number
	 */
	private void pressElevatorButton(int floor) {
		elevatorButtonBoard.put(floor, true);
	}

	/**
	 * Update the lamp to false when the floor button is pressed.
	 * 
	 * @param floor int, the elevator floor number
	 */
	private void clearElevatorButton(int floor) {
		elevatorButtonBoard.put(floor, false);
	}

	/**
	 * Set timer method.
	 * 
	 * @param task  Timertask, the timer task
	 * @param delay int, the time delay
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
	 * Kill timer method.
	 */
	public void killTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Setter for direction.
	 * 
	 * @param d Direction, the direction
	 */
	public void setDirection(Direction d) {
		direction = d;
	}

	/**
	 * Setter for door.
	 * 
	 * @param d Door, the door
	 */
	public void setDoors(Door d) {
		door = d;
	}

	/**
	 * Setter for motor.
	 * 
	 * @param m Motor, the motor
	 */
	public void setMotor(Motor m) {
		motor = m;
	}

	/**
	 * Increment the floor when it gets to the new floor.
	 * 
	 * @return boolean, true if the floor is updated else false.
	 */
	public boolean incrementCurrentFloor() {
		if (currentFloor + 1 <= elevatorSubsystem.getConfig().NUM_FLOORS) {
			currentFloor++;
			return true;
		}
		return false;
	}

	/**
	 * Decrement the floor when it gets to the new floor.
	 * 
	 * @return boolean, true if the floor is updated else false
	 */
	public boolean decrementCurrentFloor() {
		if (currentFloor - 1 >= 1) {
			currentFloor--;
			return true;
		}
		return false;
	}

	/**
	 * Getter for configuration of this class.
	 * 
	 * @return SimulatorConfiguration, the simulator configuration
	 */
	public SimulatorConfiguration getConfig() {
		return elevatorSubsystem.getConfig();
	}

	/**
	 * Overriding toString method.
	 * 
	 * @return String
	 */
	@Override
	public String toString() {
		String externalReqStr = "";
		String internalReqStr = "";
		synchronized (externalRequests) {
			for (int i = 0; i < externalRequests.size(); i++) {
				externalReqStr += externalRequests.get(i) + ", ";
			}
		}
		synchronized (internalRequests) {
			for (int i = 0; i < internalRequests.size(); i++) {
				internalReqStr += internalRequests.get(i) + ", ";
			}
		}
		return String.format(
				"Elevator#%d {CurrentFloor: %d, Current State: %s, Direction: %s, Motor: %s, Door: %s}"
						+ " queued for pickup: {%s}, carrying: {%s}",
				id, currentFloor, currentState, direction, motor, door, externalReqStr, internalReqStr);
	}

	/**
	 * Getter for elevator id.
	 * 
	 * @return id int, the elevator id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Getter for direction.
	 * 
	 * @return direction Direction, the elevator direction
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Get total number of requests.
	 * 
	 * @return int, the number of requests
	 */
	public int getNumRequests() {
		return internalRequests.size() + externalRequests.size();
	}

	/**
	 * Getter for external requests.
	 * 
	 * @return List, the list of external requests
	 */
	public List<ElevatorRequest> getExternalRequests() {
		return externalRequests;
	}

	/**
	 * Getter for internal requests.
	 * 
	 * @return List, the list of internal requests
	 */
	public List<ElevatorRequest> getInternalRequests() {
		return internalRequests;
	}

	/**
	 * Get the Elevator Lamp Light status.
	 * 
	 * @param floor Integer, the button number
	 * @return boolean, true when lamp is on, false when lamp is off
	 */
	private boolean getElevatorLampStatus(Integer floor) {
		return elevatorButtonBoard.get(floor);
	}

	/**
	 * Method that getting all the selected floors in the elevator.
	 * 
	 * @return ArrayList, list containing the all the selected floors
	 */
	private ArrayList<Integer> getAllSelectedFloors() {
		ArrayList<Integer> allSelectedFloors = new ArrayList<>();
		for (Integer floorNumber : elevatorButtonBoard.keySet()) {
			if (elevatorButtonBoard.get(floorNumber)) {
				allSelectedFloors.add(floorNumber);
			}
		}
		return allSelectedFloors;
	}

	/**
	 * Getter for current floor.
	 * 
	 * @return currentFloor int, the current floor
	 */
	public int getCurrentFloor() {
		return currentFloor;
	}

	/**
	 * Calculates next direction of the elevator.
	 * 
	 * @return Direction, the elevator direction
	 */
	public Direction calculateNextDirection() {
		Direction directionIfIdle;
		boolean continueSweepingUp, continueSweepingDown;

		continueSweepingUp = shouldContinueSweepingUp();
		continueSweepingDown = shouldContinueSweepingDown();
		directionIfIdle = determineNextDirection();

		switch (direction) {
		case UP:
			if (continueSweepingUp)
				return Direction.UP;
			if (continueSweepingDown)
				return Direction.DOWN;
		case DOWN:
			if (continueSweepingDown)
				return Direction.DOWN;
			if (continueSweepingUp)
				return Direction.UP;
		case IDLE:
			return directionIfIdle;
		}
		return Direction.IDLE;
	}

	/**
	 * Handle the continuation of the elevator sweeping up.
	 * 
	 * @return boolean, true if the elevator continues sweeping
	 */
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

	/**
	 * Handle the continuation of the elevator sweeping up.
	 * 
	 * @return boolean, true if the elevator continues sweeping
	 */
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

	/**
	 * Determines the next direction of the elevator.
	 * 
	 * @return Direction, the direction of the elevator
	 */
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
		return Direction.IDLE;
	}

	/**
	 * Determine whether the elevator should stop.
	 * 
	 * @return boolean, true if the elevator should stop
	 */
	public boolean shouldElevatorStop() {
		// check that there exists internal request @ current floor
		// or external request @ current floor and in current direction

		// new detected case: if the elevator car is empty and the elevator
		// is moving, then disregard having to check that there is a direction match?

		// case: if going some direction & there's a request at currentFloor & there's
		// no more requests
		// if you were to go in the same direction, then stop for the request @ the
		// current floor?
		// -> can you stop sweeping? violate the check of having to accept a request
		// going in same direction
		// -> essentially, the goal is to sweep until you hit the bottom-most or
		// top-most request source?
		if (getElevatorLampStatus(currentFloor)) {
			return true;
		}

		synchronized (externalRequests) {
			boolean interceptElevator = false;
			boolean existsAboveReq = existsExternalRequestsAbove();
			boolean existsBelowReq = existsExternalRequestsBelow();
			for (ElevatorRequest pendingReq : externalRequests) {
				if (pendingReq.getSourceFloor() == currentFloor && pendingReq.getDirection() == direction) {
					return true;
				}
				if (internalRequests.size() == 0 && pendingReq.getSourceFloor() == currentFloor) {
					// no one is in the car and there is a request at this floor
					if (!existsAboveReq && direction == Direction.UP
							|| !existsBelowReq && direction == Direction.DOWN) {
						// if there are no jobs to do in the direction that you are going, then you might
						// as well pick them up
						interceptElevator = true;
					}
				}
			}
			if (interceptElevator) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if there exists requests from above floors.
	 * 
	 * @return boolean, true if there are floor request from above
	 */
	private boolean existsExternalRequestsAbove() {
		synchronized (externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() > currentFloor) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determine if there exists requests from below floors.
	 * 
	 * @return boolean, true if there are floor request from below
	 */
	private boolean existsExternalRequestsBelow() {
		synchronized (externalRequests) {
			for (ElevatorRequest req : externalRequests) {
				if (req.getSourceFloor() < currentFloor) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Generates an error on the floor.
	 * 
	 * @return ElevatorError, the elevator error object
	 */
	public ElevatorError isAtErrorFloor() {
		ElevatorError error = null, reqError;
		for (int i = 0; i < internalRequests.size(); i++) {
			reqError = internalRequests.get(i).getElevatorError();
			if (reqError != null) {
				switch (reqError) {
				case ELEVATOR_STUCK:
					return ElevatorError.ELEVATOR_STUCK;
				case DOORS_STUCK:
					error = ElevatorError.DOORS_STUCK;
				}
			}
		}
		return error;
	}

	/**
	 * Notify the arrival sensors.
	 */
	private void notifyArrivalSensor() {
		elevatorSubsystem.notifyContextUpdate(this);

	}

	/**
	 * Return the external elevator request, if there is an elevator fault.
	 */
	public void returnExternalRequests() {
		synchronized (externalRequests) {
			printLog("ELEVATOR_FAULT: returning externalRequests to scheduler");
			elevatorSubsystem.returnElevatorRequests(externalRequests);
			externalRequests.removeAll(externalRequests);
		}
	}

	/**
	 * Get the current elevator current state.
	 * 
	 * @return ElevatorState, the state of the elevator
	 */
	public ElevatorState getCurrentState() {
		return currentState;
	}

	/**
	 * Get the door.
	 * 
	 * @return Door, the elevator door
	 */
	public Door getDoors() {
		return door;
	}

	/**
	 * Get the motor.
	 * 
	 * @return Motor, the elevator motor
	 */
	public Motor getMotor() {
		return motor;
	}

	/**
	 * Prints the console log to a text area.
	 * 
	 * @param message String, the string to be displayed
	 */
	private void printLog(String message) {
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		String output = String.format("[%s] : %s\n", currentTime, message);
		//System.out.println(output);
		logConsole.appendLog(output);
	}

}
