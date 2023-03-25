/**
 * 
 */
package main.java;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Zakaria Ismail
 *
 */
public class SimulatorConfiguration {
	// XXX: do i make config object or read directly from config...
	// node topology configuration
	public final int NUM_ELEVATORS;
	public final int NUM_FLOORS;
	
	// times in ms
	public final int DOORS_OPEN_TIME;
	public final int DOORS_CLOSE_TIME;
	public final int LOADING_TIME;
	public final int MOVING_TIME;
	
	// SchedulerOld and SchedulerSubsystem config
	public final String SCHEDULER_HOST;
	public final int SCHEDULER_FLOOR_REQ_PORT;
	public final int SCHEDULER_PENDING_REQ_PORT;
	public final int SCHEDULER_ARRIVAL_REQ_PORT;
	public final int SCHEDULER_COMPLETED_REQ_PORT;
	public final int SCHEDULER_NOTIFY_PORT;
	
	// Elevator config
	public final String ELEVATOR_SUBSYSTEM_HOST;
	public final int ELEVATOR_SUBSYSTEM_REQ_PORT;
	
	// Floor config
	public final String FLOOR_SUBSYSTEM_HOST;
	public final int FLOOR_SUBSYSTEM_REQ_PORT;
	
	public SimulatorConfiguration(String configFilePath) {
		FileInputStream propsInput;
		Properties prop = null;
		
		try {
			propsInput = new FileInputStream(configFilePath);
			prop = new Properties();
			prop.load(propsInput);
		} catch (IOException e) {
			System.out.println("Failed to load config.");
			System.exit(0);
		}
		
		NUM_ELEVATORS = Integer.parseInt(prop.getProperty("NUM_ELEVATORS"));
		NUM_FLOORS = Integer.parseInt(prop.getProperty("NUM_FLOORS"));
		
		DOORS_OPEN_TIME = Integer.parseInt(prop.getProperty("DOORS_OPEN_TIME"));
		DOORS_CLOSE_TIME = Integer.parseInt(prop.getProperty("DOORS_CLOSE_TIME"));
		LOADING_TIME = Integer.parseInt(prop.getProperty("LOADING_TIME"));
		MOVING_TIME = Integer.parseInt(prop.getProperty("MOVING_TIME"));
		
		SCHEDULER_HOST = prop.getProperty("SCHEDULER_HOST");
		SCHEDULER_FLOOR_REQ_PORT = Integer.parseInt(prop.getProperty("SCHEDULER_FLOOR_REQ_PORT"));
		SCHEDULER_PENDING_REQ_PORT = Integer.parseInt(prop.getProperty("SCHEDULER_PENDING_REQ_PORT"));
		SCHEDULER_ARRIVAL_REQ_PORT = Integer.parseInt(prop.getProperty("SCHEDULER_ARRIVAL_REQ_PORT"));
		SCHEDULER_COMPLETED_REQ_PORT = Integer.parseInt(prop.getProperty("SCHEDULER_COMPLETED_REQ_PORT"));
		SCHEDULER_NOTIFY_PORT = Integer.parseInt(prop.getProperty("SCHEDULER_NOTIFY_PORT"));
		
		ELEVATOR_SUBSYSTEM_HOST = prop.getProperty("ELEVATOR_SUBSYSTEM_HOST");
		ELEVATOR_SUBSYSTEM_REQ_PORT = Integer.parseInt(prop.getProperty("ELEVATOR_SUBSYSTEM_REQ_PORT"));
		
		FLOOR_SUBSYSTEM_HOST = prop.getProperty("FLOOR_SUBSYSTEM_HOST");
		FLOOR_SUBSYSTEM_REQ_PORT = Integer.parseInt(prop.getProperty("FLOOR_SUBSYSTEM_REQ_PORT"));
		
	}
	
	
	
}
