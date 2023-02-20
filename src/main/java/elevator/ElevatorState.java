package main.java.elevator;

/**
 * ElevatorState enum class holds the possible states of the Elevator.
 * @author Trong Nguyen
 * @version 2.0, 02/27/23
 */
public enum ElevatorState {

	Idle {
		@Override
		public ElevatorState nextState() {
			return AwaitRequest;
		}
	},
	
	AwaitRequest {
		@Override
		public ElevatorState nextState() {
			return Moving;
		}
	},
	
	Moving {
		@Override
		public ElevatorState nextState() {
			return Stop;
		}
	},
	
	Stop {
		@Override
		public ElevatorState nextState() {
			return DoorsOpen;
		}
	},
	
	DoorsOpen {
		@Override
		public ElevatorState nextState() {
			return DoorsClose;
		}
	},
	
	DoorsClose {
		@Override
		public ElevatorState nextState() {
			return AwaitRequest;
		}
	};
	 
	public abstract ElevatorState nextState(); 
	
}
