/**
 * 
 */
package main.java.parser;

/**
 * The ElevatorRequest class is responsible for storing all the
 * relevant information regarding passenger's elevator requests
 * 
 * @author Patrick Liu, 101142730
 * @since   2023-01-23
 */
public class ElevatorRequest {
	
	enum Direction {
		  UP,
		  DOWN,
	}
	
	private java.sql.Timestamp timestamp;
	private Integer floorRequest;
	private Direction direction;
	private Integer floorDestination;
	
	/**
	 * Constructor of the ElevatorRequest class
	 * 
	 * @param timestamp is a point in time which the passenger pressed the floor button
	 * @param floorRequest is the floor which the passenger declared his/her traveling intention
	 * @param direction is passenger's declared traveling direction
	 * @param floorDestination is the destination floor which the passenger entered inside the elevator cart
	 */
	public ElevatorRequest(java.sql.Timestamp timestamp, Integer floorRequest, Direction direction, Integer floorDestination) {
		this.timestamp = timestamp;
		this.floorRequest = floorRequest;
		this.direction = direction;
		this.floorDestination = floorDestination;
	}
	
	/**
	 * getTimestamp returns a point in time which the passenger pressed the floor button
	 * @return java.sql.Timestamp
	 */
	public java.sql.Timestamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * getfloorRequest returns the floor which the passenger declared his/her traveling intention
	 * @return Integer
	 */
	public Integer getfloorRequest() {
		return floorRequest;
	}
	
	/**
	 * getDirection returns passenger's declared traveling direction
	 * @return enum Direction
	 */
	public Direction getDirection() {
		return direction;
	}
	
	/**
	 * getfloorDestination returns the destination floor which the passenger entered inside the elevator cart
	 * @return Integer
	 */
	public Integer getfloorDestination() {
		return floorDestination;
	}
	
}
