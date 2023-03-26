/**
 * 
 */
package main.java.scheduler.state;

import main.java.scheduler.SchedulerContext;

/**
 * @author Zakaria Ismail
 *
 */
public class IdleState extends SchedulerState {

	/**
	 * Constructor
	 * @param ctx
	 */
	public IdleState(SchedulerContext ctx) {
		super(ctx);
		// entry/constructor():
		// do nothing. wait for some "signal" to wake it up
		// being in this state means that there are no pendingRequests
		// and completedRequests in the queues
	}

	/**
	 * handleRequestReceived
	 */
	@Override
	public SchedulerState handleRequestReceived() {
		SchedulerContext ctx = this.getContext();
		if (ctx.isSchedulerIdle()) {
			return this;
		}
		return new InServiceState(ctx);

	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		return "IdleState";
	}

	/**
	 * handleRequestSent 
	 */
	@Override
	public SchedulerState handleRequestSent() {
		return this;
	}

}