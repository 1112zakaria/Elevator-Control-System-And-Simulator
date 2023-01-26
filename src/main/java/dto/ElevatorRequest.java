/**
 * 
 */
package main.java.dto;

import java.sql.Timestamp;

/**
 * The ElevatorRequest class is responsible for storing all the
 * relevant information regarding passenger's elevator requests
 * 
 * @author Patrick Liu, 101142730
 * @since   2023-01-23
 */
public class ElevatorRequest {
	
	private Timestamp timestamp;
	private Integer sourceFloor;
	private Direction direction;
	private Integer destinationFloor;
	
	/**
	 * Constructor of the ElevatorRequest class
	 * 
	 * @param timestamp is a point in time which the passenger pressed the floor button
	 * @param sourceFloor is the floor which the passenger declared his/her traveling intention
	 * @param direction is passenger's declared traveling direction
	 * @param destinationFloor is the destination floor which the passenger entered inside the elevator cart
	 */
	public ElevatorRequest(Timestamp timestamp, Integer sourceFloor, Direction direction, Integer destinationFloor) {
		this.timestamp = timestamp;
		this.sourceFloor = sourceFloor;
		this.direction = direction;
		this.destinationFloor = destinationFloor;
	}
	
	/**
	 * getTimestamp returns a point in time which the passenger pressed the floor button
	 * @return java.sql.Timestamp
	 */
	public Timestamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * getSourceFloor returns the floor which the passenger declared his/her traveling intention
	 * @return Integer
	 */
	public Integer getSourceFloor() {
		return sourceFloor;
	}
	
	/**
	 * getDirection returns passenger's declared traveling direction
	 * @return enum Direction
	 */
	public Direction getDirection() {
		return direction;
	}
	
	/**
	 * getDestinationFloor returns the destination floor which the passenger entered inside the elevator cart
	 * @return Integer
	 */
	public Integer getDestinationFloor() {
		return destinationFloor;
	}
	
	/**
	 * toString combines the object attributes into a readable format
	 * @return String
	 */
	@Override
	public String toString(){ 
		  return timestamp.toString().split(" ")[1] + " " + + sourceFloor + " " +
				  direction + " " + destinationFloor;
	} 
	
}
