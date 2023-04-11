package main.java.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator.OfDouble;

import main.java.dto.AssignedElevatorRequest;
import main.java.dto.ElevatorRequest;
import main.java.dto.ElevatorStatus;
import main.java.elevator.Direction;
import main.java.elevator.state.ElevatorStateEnum;
import main.java.scheduler.state.SchedulerState;

/**
 * Responsible for routing each elevator to requested floors and coordinating elevators in such a way to minimize
 * waiting times for people moving between floors (avoiding starvation).
 * @author Bobby Ngo, Patrick Liu
 */
public class SchedulerContext {
	
	private SchedulerSubsystem schedulerSubsystem;
	
	// storing all the elevators that are available
	private List<ElevatorStatus> availableElevatorStatus;
	// pending elevators requests
	private List<ElevatorRequest> pendingElevatorRequests;
	// Elevator requests that completed
	private List<ElevatorRequest> completedElevatorRequests;
	
	private SchedulerState currentState;
	
	/**
	 * Constructor for Scheduler Context.
	 * @param schedulerSubsystem SchedulerSubsystem, the elevator subsystem
	 */
	public SchedulerContext(SchedulerSubsystem schedulerSubsystem) {
		this.schedulerSubsystem = schedulerSubsystem;
		
		// make sure that 4 scheduler threads use the same instance of these 3 array list
		availableElevatorStatus = Collections.synchronizedList(new ArrayList<>());
		pendingElevatorRequests = Collections.synchronizedList(new ArrayList<>());
		completedElevatorRequests = Collections.synchronizedList(new ArrayList<>());
		
		for (int i = 1; i <= schedulerSubsystem.getSimulatorConfiguration().NUM_ELEVATORS; i++) {
			availableElevatorStatus.add(new ElevatorStatus(i));
		}
		currentState = SchedulerState.start(this);
		System.out.println(String.format("Current state: %s", currentState));
	}
	
	/**
	 * Finds the closest elevator to the requested floor.
	 * @param elevators ArrayList, the list of elevators
	 * @param sourceFloor int, the source floor number
	 * @return ElevatorStatus, the status of the elevator
	 */
	private ElevatorStatus findTheClosestElevatorToRequestFloor(ArrayList<ElevatorStatus> elevators, int sourceFloor) {
		ElevatorStatus chosenElevatorStatus = null;
		// init the min value to the total floors
		int closestElevator = schedulerSubsystem.getSimulatorConfiguration().NUM_FLOORS;
		
		
		// Find the smallest distance between the floor that the elevator locates and the floor that request elevator
		for (ElevatorStatus status : elevators) {
			int differentBetweenElevatorAndRequest = Math.abs(status.getFloor() - sourceFloor);
			
			if(closestElevator > differentBetweenElevatorAndRequest) {
				differentBetweenElevatorAndRequest = closestElevator = differentBetweenElevatorAndRequest;
				chosenElevatorStatus = status;
			}
		}
		return chosenElevatorStatus;
	}
	
	private ElevatorStatus findTheAvailableIdleElevator(ElevatorRequest request) {
		ArrayList<ElevatorStatus> idleElevatorStatus = new ArrayList<>();
		for (ElevatorStatus status : availableElevatorStatus) {
			if (status.getDirection() == Direction.IDLE) {
				idleElevatorStatus.add(status);
			}
		}
		ElevatorStatus chosenElevatorStatus = null;
		
		// Begining of the program, all the elevators are idle, return a first elevator
		if (idleElevatorStatus.size() == schedulerSubsystem.getSimulatorConfiguration().NUM_ELEVATORS) {
			chosenElevatorStatus = availableElevatorStatus.get(0);
			
		} else {
			chosenElevatorStatus = findTheClosestElevatorToRequestFloor(idleElevatorStatus, request.getSourceFloor());
		}
		return chosenElevatorStatus;
	}
	
	/**
	 * Method for finding all the elevators that are moving and make sure that its current distance is closest the most to the floor requesting the request. 
	 * @param direction
	 * @param newRequestSourceFloor
	 * @return Elevator Status
	 */
	private ElevatorStatus findTheAvailableMovingElevator(Direction direction, int newRequestSourceFloor) {
		// checked the available is empty already so no need to check again
		ElevatorStatus chosenElevatorStatus = null;
		ArrayList<ElevatorStatus> movingUpElevatorStatus = new ArrayList<>();
		ArrayList<ElevatorStatus> movingDownElevatorStatus = new ArrayList<>();
		for (ElevatorStatus status : availableElevatorStatus) {
			if (status.getState() != ElevatorStateEnum.DOORS_STUCK || status.getState() != ElevatorStateEnum.ELEVATOR_STUCK) {
				// 1st priority: Elevator that is moving up and current floor <= source floor
				if (status.getDirection() == direction && direction == Direction.UP
						&& status.getFloor() <= newRequestSourceFloor) {
					movingUpElevatorStatus.add(status);
					//chosenElevatorStatus = status;
				} 
				// 2nd priority: Elevator that is moving down and current floor >= source floor
				else if (status.getDirection() == direction && direction == Direction.DOWN
						&& status.getFloor() >= newRequestSourceFloor) {
					movingDownElevatorStatus.add(status);
					//chosenElevatorStatus = status;
				} 
			}
		}
		// Find the closest distance of the elevator that is moving up and its current floor is closest to the floor that request the elevator
		if (direction == Direction.UP) {
			chosenElevatorStatus = findTheClosestElevatorToRequestFloor(movingUpElevatorStatus, newRequestSourceFloor);
		} else if(direction == Direction.DOWN) {
			// Find the closest distance of the elevator that is moving down and its current floor is closest to the floor that request the elevator
			chosenElevatorStatus = findTheClosestElevatorToRequestFloor(movingDownElevatorStatus, newRequestSourceFloor);
		}
		
		return chosenElevatorStatus;
	}
	
	/**
	 * Method for finding the best elevator following by the priority
	 * @return AssignedElevatorRequest
	 */
	public AssignedElevatorRequest findBestElevatorToAssignRequest() {
		// following C convention or zak will be malding
		AssignedElevatorRequest assignedElevatorRequest = null;
		
		if (availableElevatorStatus.size() == 0) {
			// should add udp saying there is not available elevator?
			System.out.println("There is no available elevator");
		} 
		// these 2 list could both be empty so if 
		if (pendingElevatorRequests.size() == 0) {
			// should add udp saying there is not available elevator?
			System.out.println("There is no elevator requests");
		} else {
			ElevatorStatus chosenElevatorStatus = null;
			
			synchronized (pendingElevatorRequests) {
				ElevatorRequest request = null;
				// Find the moving elevators
				for (int i=0; i<pendingElevatorRequests.size(); i++) {
					request = pendingElevatorRequests.get(i);
					
					chosenElevatorStatus = findTheAvailableMovingElevator(request.getDirection(), request.getSourceFloor()); 
					 
					if (chosenElevatorStatus != null) {
						assignedElevatorRequest = new AssignedElevatorRequest(chosenElevatorStatus.getElevatorId(), request);
						break;
					} 
				}
				
				// Find the idle elevators
				if (chosenElevatorStatus == null) {
					for (int i=0; i<pendingElevatorRequests.size(); i++) {
						request = pendingElevatorRequests.get(i);
						
						chosenElevatorStatus = findTheAvailableIdleElevator(request);
						 
						if (chosenElevatorStatus != null) {
							assignedElevatorRequest = new AssignedElevatorRequest(chosenElevatorStatus.getElevatorId(), request);
							break;
						} 
					}
				}
				
				if (chosenElevatorStatus == null) {
					int numOfElevators = schedulerSubsystem.getSimulatorConfiguration().NUM_ELEVATORS;
					assignedElevatorRequest = new AssignedElevatorRequest((int)(Math.random() * numOfElevators + 1) , request);
				}
				
				if (request != null) {
					pendingElevatorRequests.remove(request);
				}
			}
		}
		
		// can be null cause the 2 if statements
		return assignedElevatorRequest;
	}
	
	
	
	/**
	 * assignNextBestElevatorRequest
	 * @throws IOException
	 */
	public void assignNextBestElevatorRequest() throws IOException {
		AssignedElevatorRequest request = this.findBestElevatorToAssignRequest();
		if (request != null) {
			schedulerSubsystem.sendPendingRequest(request);
		}
	}
	
	/**
	 * Adding method for availableElevatorStatus list
	 * @param elevatorStatus
	 */
	public void addAvailableElevatorStatus(ElevatorStatus elevatorStatus) {
		synchronized(availableElevatorStatus) {
			availableElevatorStatus.add(elevatorStatus);
		}
	}
	
	/**
	 * Method for modifying the availableElevatorStatus list
	 * @param index
	 * @param elevatorStatus
	 */
	public void modifyAvailableElevatorStatus(int index, ElevatorStatus elevatorStatus) {
		synchronized(availableElevatorStatus) {
			availableElevatorStatus.set(index, elevatorStatus);
		}
	}
	
	/**
	 * Adding method for pendingElevatorRequests list
	 * @param elevatorRequest
	 */
	public void addPendingElevatorRequests(ElevatorRequest elevatorRequest) {
		synchronized(pendingElevatorRequests) {
			pendingElevatorRequests.add(elevatorRequest);
			onRequestReceived();
		}
	}
	
	/**
	 * Adding method for completedElevatorRequests list
	 * @param elevatorRequest
	 */
	public void addCompletedElevatorRequests(ElevatorRequest elevatorRequest) {
		synchronized(completedElevatorRequests) {
			completedElevatorRequests.add(elevatorRequest);
			onRequestReceived();
		}
	}
	
	/**
	 * Method for onRequestReceived
	 */
	public void onRequestReceived() {
		synchronized (currentState) {
			System.out.println("Event: Request Received");
			currentState = currentState.handleRequestReceived();
			System.out.println(String.format("Current state: %s, # Completed: %d", 
					currentState, this.completedElevatorRequests.size()));
		}
	}
	
	/**
	 * onRequestSent
	 */
	public void onRequestSent() {
		synchronized (currentState) {
			System.out.println("Event: Request Sent");
			currentState = currentState.handleRequestSent();
			System.out.println(String.format("Current state: %s, # Completed: %d", 
					currentState, this.completedElevatorRequests.size()));
		}
	}
	
	/**
	 * Checking if scheduler should be idle
	 * @return boolean
	 */
	public boolean isSchedulerIdle() {
		return pendingElevatorRequests.size() == 0 && completedElevatorRequests.size() == 0;
	}
	
	/**
	 * processCompletedElevatorRequest
	 * @throws IOException
	 */
	public void processCompletedElevatorRequest() throws IOException {
		// do something with the completed elevator request here...?
		ElevatorRequest nextCompletedRequest;
		if (completedElevatorRequests.size() > 0) {
			nextCompletedRequest = completedElevatorRequests.get(completedElevatorRequests.size() - 1);
			//this.completedElevatorRequests.remove(nextCompletedRequest);
			schedulerSubsystem.sendCompletedElevatorRequest(nextCompletedRequest);
		}
	}
	
	/**
	 * Getter for availableElevatorStatus
	 * @return all available elevators status
	 */
	public List<ElevatorStatus> getAvailableElevatorStatus() {
		return availableElevatorStatus;
	}
	
	/**
	 * Getter for pendingElevatorRequests
	 * @return pendingElevatorRequests list
	 */
	public List<ElevatorRequest> getPendingElevatorRequests() {
		return pendingElevatorRequests;
	}
	
	/**
	 * Getter for completedElevatorRequests
	 * @return elevator request
	 */
	public List<ElevatorRequest> getCompletedElevatorRequests() {
		return completedElevatorRequests;
	}
	
	public SchedulerState getCurrentState() {
		return currentState;
	}

	
	
	
}
