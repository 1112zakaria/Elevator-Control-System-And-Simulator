/**
 * 
 */
package main.java.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;

import main.java.dto.ElevatorRequest;
import main.java.exception.*;
import main.java.dto.Direction;

/**
 * The Parser class reads through a standard text file 
 * and exports the information in a specified format
 * 
 * @author Patrick Liu, 101142730
 * @since   2023-01-23
 */
public class Parser {
	private FileReader input;
	private BufferedReader reader;
	private String lineEntry;
	private ArrayList<ElevatorRequest> elevatorRequestList;
	
	/**
	 * Constructor of the Parser class
	 * 
	 * @param fileName is the name of the provided file
	 * @throws FileNotFoundException when the file with the provided name can not be found
	 */
	public Parser(String fileName) throws FileNotFoundException {
		input = new FileReader(fileName);
		reader = new BufferedReader(input);
		lineEntry = null;
		elevatorRequestList = new ArrayList<>();
	}
	
	/**
	 * RequestParser is responsible for parsing the text file and storing the extracted
	 * information in ElevatorRequest objects 
	 * 
	 * @return an ArrayList of ElevatorRequest object
	 * @throws IOException when input/output error is encountered
	 */
	public ArrayList<ElevatorRequest> requestParser() throws IOException {
		
		int lineNumber = 0;
		boolean parsingSuccess = true;
		
		while ( (lineEntry = reader.readLine()) != null){
		    String[] line = lineEntry.split(" ");		    
		    Timestamp timestamp = null;
		    lineNumber ++;
		    
		    try {
		    	SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
		        Date parsedDate = dateFormat.parse(line[0]);
		        timestamp = new Timestamp(parsedDate.getTime());
		    	
		    	if(line.length != 4) {
		    		throw new IncorrectElevatorRequestParameterNumberException(
		    				"Line " + lineNumber + " contains incorrect numbers of elevator request parameter");
		    	}
		    	
		    	elevatorRequestList.add(new ElevatorRequest(timestamp, Integer.valueOf(line[1]), 
			    		Direction.valueOf(line[2]), Integer.valueOf(line[3])));
		    	
		    }catch(ParseException e){
		    	System.out.println(e + " on line " + lineNumber);
		    	parsingSuccess = false;
		    }catch(IncorrectElevatorRequestParameterNumberException e) {
		    	System.out.println(e);
		    	parsingSuccess = false;
		    }catch(NumberFormatException e) {
		    	System.out.println(e + " on line " + lineNumber);
		    	parsingSuccess = false;
		    }catch(IllegalArgumentException e) {
		    	System.out.println(e + " on line " + lineNumber);
		    	parsingSuccess = false;
		    }finally {
		    	if (!parsingSuccess) {
		    		elevatorRequestList.clear();
		    	}
		    }

		}
		return elevatorRequestList;
		
	}

}
