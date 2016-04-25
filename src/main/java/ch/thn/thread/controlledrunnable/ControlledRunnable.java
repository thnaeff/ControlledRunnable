/**
 *    Copyright 2016 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.thn.thread.controlledrunnable;

import java.util.ArrayList;

/**
 * This abstract class contains the functionality for pausing, resetting and stopping
 * a thread. Instead of implementing {@link Runnable}, extending with
 * {@link ControlledRunnable} gives access to {@link #pause(boolean)}/{@link #pause(boolean, boolean)},
 * {@link #reset()} and {@link #stop()}/{@link #stop(boolean)}. {@link ControlledRunnable}
 * then offers several methods which can be used in the program flow to act on the different
 * states. This class also provides the possibility to register {@link ControlledRunnableListener}s
 * which are notified of state changes. The listeners are notified when a state change has been requested 
 * and once the state has been reached.<br />
 * <br />
 * Is not possible for {@link ControlledRunnable} to know during your
 * own program flow implementation when it is a good time to stop, reset or pause
 * the execution. Therefore, the pause/reset/stop methods only set an internal state which
 * needs to be checked in your runnable class in order to get the desired behavior.
 * This means that methods like {@link #runStart()}, {@link #runEnd()},
 * {@link #runPause(boolean, boolean)}, {@link #isResetRequested()}, {@link #isPauseRequested()}
 * or {@link #isStopRequested()} have to be used in your runnable to control the
 * behavior.<br>
 * <br>
 * A run method in your runnable could look like this:<br>
 * <pre>
 * public void run() {
 *	{@link #runStart()};
 *	
 *	//Main loop. Keeping the thread running
 *	while (!{@link #isStopRequested()}) {
 *		//The reset point of the program could be here
 *		{@link #runReset()}
 *		//Pause (if requested). Do not exit when reset is called.
 *		//This method implements the pausing and blocks as long as the pause state is set.
 *		{@link #runPause(boolean)}
 *
 *		if ({@link #isStopRequested()}) {
 *			//Exit the while-loop to end the thread
 *			break;
 *		}
 *
 *		if ({@link #isResetRequested()}) {
 *			//Reset could just mean "start again at the beginning". It is up to the
 *			//implementation on how to use it.
 *			continue;
 *		}
 *
 *		//Do your stuff...
 *		//Use any of the offered methods to control the program flow
 *		//depending on the controlled runnable state.
 *		//Methods to use in addition to the ones mentioned already in this example:
 *		//{@link #controlledWait()}
 *		//{@link #controlledWait(long)}
 *		//{@link #isPauseRequested()}
 *		//Any of the {@link #pause()} methods to request a pause
 *		//
 *		//Hint: Instead of using wait() on a object directly, use one of the controlledWait
 *		//methods if you want the implementation to react to any pause/reset/stop etc. calls. The
 *		//controlledWait methods use the wait() internally but exit the wait if
 *		//reset() or stop() is called.
 *
 *	}
 *
 *	{@link #runEnd()}
 * }
 * 
 * </pre>
 * <br />
 * The idea is that any object that wants to interact with the {@link ControlledRunnable}
 * can request it to stop, pause or reset. The runnable implementation then defines
 * how those states are reached. The implementation might first have to finish a certain task
 * before it can reach the requested state, or it might or might not wake up from a
 * waiting state to reach the requested state. It is completely up to the implementation.
 * <br />
 * <br />
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public abstract class ControlledRunnable implements Runnable {

	private volatile boolean running = false;
	private volatile boolean paused = false;
	private volatile boolean stopRequested = false;
	private volatile boolean stopped = false;
	private volatile boolean resetRequested = false;

	private volatile long timeout = 0;

	private volatile Boolean pauseRequested = false;

	private boolean pausingImplemented = true;
	private boolean resetImplemented = true;

	private final ArrayList<ControlledRunnableListener> listeners;

	private final Object PAUSESYNC = new Object();
	private final Object WAITFORSYNC = new Object();

	/**
	 * Creates a new {@link ControlledRunnable} class. The "-Implemented" parameters 
	 * give information about the possibilities depending on how the runnable is implemented.
	 * For example, if <code>pausingImplemented</code> is set to <code>true</code>, calling 
	 * {@link #pause(boolean)} on the runnable will actually make the process wait. If 
	 * <code>pausingImplemented</code> is given as <code>false</code>, calling {@link #pause(boolean)} 
	 * would cause an {@link UnsupportedOperationException}.
	 * 
	 * 
	 * @param pausingImplemented The pausing flag has to be provided to indicate if the
	 * implementation supports pausing or not.
	 * @param resetImplemented The reset flag has to be provided to indicate if the
	 * implementation supports resetting or not.
	 */
	protected ControlledRunnable(boolean pausingImplemented, boolean resetImplemented) {
		this.pausingImplemented = pausingImplemented;
		this.resetImplemented = resetImplemented;

		paused = false;
		stopRequested = false;
		resetRequested = false;
		pauseRequested = false;

		listeners = new ArrayList<ControlledRunnableListener>();

	}

	/**
	 * Adds a listener to this class. The listener will be notified when the state
	 * of this runnable changes (see {@link ControlledRunnableEvent}).
	 * 
	 * @param l
	 */
	public void addControlledRunnableListener(ControlledRunnableListener l) {
		listeners.add(l);
	}

	/**
	 * Removes an existing listener from this class
	 * 
	 * @param l
	 */
	public void removeControlledRunnableListener(ControlledRunnableListener l) {
		listeners.remove(l);
	}

	/**
	 * Fires the listener with the state of the event
	 * 
	 * @param e
	 */
	private void fireControlledRunnableListener(ControlledRunnableEvent e) {
		for (ControlledRunnableListener l : listeners) {
			l.runnableStateChanged(e);
		}
	}

	// ========================================================================


	/**
	 * Returns a flag which indicates if this runnable is currently running or not
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return running;
	}

	// ========================================================================

	/**
	 * Pauses the runnable for the given number of milliseconds. This method is non blocking and 
	 * does not wait for the runnable to reach the paused state.
	 * 
	 * @param timeout The number of milliseconds to wait
	 */
	public void pause(long timeout) {
		pause(timeout, false);
	}

	/**
	 * Pauses the runnable for the given number of milliseconds. The <code>wait</code> parameter 
	 * can be used to choose if the method should block until the runnable reached the pause state.
	 * 
	 * @param timeout The number of milliseconds to wait
	 * @param wait Wait for the runnable to reach the pause state (<code>true</code>). Do not wait, request 
	 * the pause and continue (<code>false</code>).
	 */
	public void pause(long timeout, boolean wait) {
		this.timeout = timeout;
		pause(true, wait);
	}

	/**
	 * Requests the runnable to pause (pause=<code>true</code>) or to exit from
	 * its paused state (pause=<code>false</code>).
	 * 
	 * @param pause Indicator if runnable should be paused (<code>true</code>) or un-paused (<code>false</code>).
	 */
	public void pause(boolean pause) {
		if (!pausingImplemented) {
			throw new ControlledRunnableError("Pausing this runnable is not implemented.");
		}

		this.timeout = 0;

		pauseInternal(pause, false);
	}

	/**
	 * Requests the runnable to pause (pause=<code>true</code>) or to exit from
	 * its paused state (pause=<code>false</code>). With flag to wait for runnable
	 * to reach the paused state.
	 * 
	 * @param pause Indicator if runnable should be paused (<code>true</code>) or un-paused (<code>false</code>).
	 * @param wait Wait for the runnable to reach the pause state (<code>true</code>). Do not wait, request 
	 * the pause and continue (<code>false</code>).
	 */
	public void pause(boolean pause, boolean wait) {
		if (!pausingImplemented) {
			throw new ControlledRunnableError("Pausing this runnable is not implemented.");
		}

		this.timeout = 0;

		pauseInternal(pause, wait);
	}

	/**
	 * Requests the runnable to pause (pause=<code>true</code>) or to exit from
	 * its paused state (pause=<code>false</code>).
	 * 
	 * @param pause Indicator if runnable should be paused (<code>true</code>) or un-paused (<code>false</code>).
	 * @param wait Wait for the runnable to reach the pause state (<code>true</code>). Do not wait, request 
	 * the pause and continue (<code>false</code>).
	 */
	private void pauseInternal(boolean pause, boolean wait) {
		if (stopped) {
			throw new ControlledRunnableError("The pause state of this runnable can not be changed, because it has already been stopped.");
		}
		
		//Request pause
		pauseRequested = pause;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.PAUSE, 1));

		if (!pause && paused) {
			//No pause any more (if currently paused)
			synchronized (PAUSESYNC) {
				PAUSESYNC.notify();
			}
		}

		//Wait for the runnable to reach the paused state
		if (wait) {
			//Only synchronize if really necessary
			if (pause && !paused || !pause && paused) {
				
				synchronized (WAITFORSYNC) {
					try {
						//Wait for entering or exiting pause state
						while (pause && !paused || !pause && paused) {
							WAITFORSYNC.wait();
						}
					} catch (InterruptedException e) {}
				}
				
			}

		}

	}

	/**
	 * A flag which is set when the runnable is requested to pause
	 * (with {@link #pause(boolean)} or {@link #pause(boolean, boolean)})
	 * 
	 * @return
	 */
	protected boolean isPauseRequested() {
		return pauseRequested;
	}

	/**
	 * @see #runPause(boolean, boolean)
	 */
	protected void runPause(boolean exitAtReset) {
		runPause(exitAtReset, false);
	}

	/**
	 * This method implements the pausing mechanism and blocks as long as the runnable
	 * is in the pausing state. When this method exits it could be because pausing
	 * is done or stopping or reset has been requested. Therefore, after calling
	 * this method there should be a check in the program flow if stopping or reset
	 * (if implemented) has been requested in order to figure out the right cause of the 
	 * end of the pause.
	 * 
	 * @param exitAtReset If <code>true</code>, this pause is ended when {@link #reset()}
	 * is called. If <code>false</code>, this method keeps pausing even though
	 * the runnable is in the reset state (it ignores {@link #reset()} and
	 * the reset state is cleared automatically).
	 * @param controlledWait Waits even though a pause is not requested. This is
	 * to simulate a wait(), but to be able to react to reset and stop calls.
	 */
	private void runPause(boolean exitAtReset, boolean controlledWait) {

		if (!stopRequested && (pauseRequested || controlledWait)) {
			//Only do the synchronization if a pause is actually requested

			synchronized (PAUSESYNC) {
				//Check the pause request again, since it could have changed
				//just before the synchronized block. Also leave immediately if
				//stop is requested
				if (!controlledWait && (!pauseRequested || stopRequested)) {
					return;
				}

				paused = true;
				fireControlledRunnableListener(
						new ControlledRunnableEvent(this,
								controlledWait ? ControlledRunnableEvent.StateType.WAIT : ControlledRunnableEvent.StateType.PAUSE, 2));

				notifyWaitForSync();

				while (!stopRequested && (pauseRequested || controlledWait)) {
					try {
						PAUSESYNC.wait(timeout);

						//Possible timeout expired
						if (timeout != 0) {
							//Even though the pausing ended, do the request for ending the pause
							//because it also notifies listeners etc.
							pauseInternal(false, false);
						}

						if (controlledWait) {
							break;
						}
					} catch (InterruptedException e) {}

					//The synchronized(pauseRequested) block makes sure that
					//once the pause(false) has been called, pausing is exited
					//properly and does not repeat if pause(true) is called
					//again right after

					if (!resetRequested && pauseRequested) {
						//Might have been interrupted from the outside -> only
						//if pause(false) has been called pauseRequested would
						//be false
						continue;
					}

					if (resetRequested) {
						if (!exitAtReset && pauseRequested) {
							//Do not exit this pause if reset is called
							continue;
						} else {
							//Exit this pause because reset has been called
							pauseRequested = false;
							break;
						}
					}

				}

				pauseDone(controlledWait);

			}

		}
	}

	/**
	 * A helper method to signal the end of the pause
	 * 
	 * @param controlledWait
	 */
	private void pauseDone(boolean controlledWait) {
		paused = false;
		fireControlledRunnableListener(
				new ControlledRunnableEvent(this,
						controlledWait ? ControlledRunnableEvent.StateType.WAIT : ControlledRunnableEvent.StateType.PAUSE, 3));

		notifyWaitForSync();
	}

	/**
	 * Returns <code>true</code> if the runnable is currently paused
	 * 
	 * @return
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * A flag which is set when the runnable has been requested to pause (with
	 * {@link #pause(boolean) or {@link #pause(boolean, boolean)}) but it is still
	 * running.
	 * 
	 * 
	 * @return
	 */
	public boolean willPause() {
		return pauseRequested && !paused;
	}
	
	/**
	 * A flag which is set when the runnable has been requested to un-pause (leave the pause 
	 * state with {@link #pause(boolean) or {@link #pause(boolean, boolean)}) but it is still 
	 * in the pause state
	 * 
	 * 
	 * @return
	 */
	public boolean willUnPause() {
		return ! pauseRequested && paused;
	}

	// ========================================================================

	/**
	 * Blocks the execution for the given time or until the pause state of this
	 * {@link ControlledRunnable} is changed (with reset/stop/pause). This is an
	 * alternative for the {@link Runnable#wait(long)} method. It behaves exactly
	 * like it, but this method here gets interrupted if reset or stop is called
	 * and it puts the {@link ControlledRunnable} in the pause state. Using this
	 * method is preferred because it does not lock the operation of the
	 * {@link ControlledRunnable}.
	 * 
	 * @param timeout
	 */
	protected void controlledWait(long timeout) {
		this.timeout = timeout;
		runPause(true, true);
	}

	/**
	 * Blocks the execution until the pause state of this {@link ControlledRunnable}
	 * is changed (with reset/stop/pause). This is an alternative for the
	 * {@link Runnable#wait()} method. It behaves exactly like it, but this
	 * method here gets interrupted if reset or stop is called and it puts the
	 * {@link ControlledRunnable} in the pause state.
	 * 
	 */
	protected void controlledWait() {
		controlledWait(0);
	}

	// ========================================================================


	/**
	 * Requests the runnable to stop.<br />
	 * The result depends on the implementation, but usually this ends in a thread
	 * to end.
	 * 
	 */
	public void stop() {
		stopRequested = true;
		pauseRequested = false;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RUN, 4));

		// Notify the pausing. runPause will exit if stop requested
		synchronized (PAUSESYNC) {
			PAUSESYNC.notify();
		}

	}

	/**
	 * Requests the runnable to stop. With flag to wait for the runnable to stop.<br />
	 * The result depends on the implementation, but usually this ends in a thread
	 * to end.
	 * 
	 * @param wait If set to <code>true</code>, this method blocks until the runnable has
	 * reached its end point.
	 */
	public void stop(boolean wait) {
		stopRequested = true;
		pauseRequested = false;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RUN, 5));

		// Notify the pausing. runPause will exit if stop requested
		synchronized (PAUSESYNC) {
			PAUSESYNC.notify();
		}

		if (!stopped) {
			//Only synchronize if really necessary

			synchronized (WAITFORSYNC) {
				try {
					//Wait for runnable to stop
					while (!stopped) {
						WAITFORSYNC.wait();
					}
				} catch (InterruptedException e) {}
			}
		}

	}

	/**
	 * A flag which is set when the runnable is requested to stop
	 * (with {@link #stop()} or {@link #stop(boolean)})
	 * 
	 * @return
	 */
	protected boolean isStopRequested() {
		return stopRequested;
	}

	/**
	 * A flag which is set when the runnable has been requested to stop (with
	 * {@link #stop()} or {@link #stop(boolean)}) but it is still running.
	 * 
	 * @return
	 */
	public boolean willStop() {
		return stopRequested && running;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean isStopped() {
		return stopped;
	}

	// ========================================================================

	/**
	 * This method should be called at the very beginning of the implemented run()
	 * method. It initializes the running state and notifies listeners about the
	 * new state.
	 * 
	 */
	protected void runStart() {
		//Set current thread name for easier identification
		Thread.currentThread().setName(getClass().getSimpleName());

		stopped = false;
		running = true;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RUN, 6));
	}

	/**
	 * This method should be called at the very end of the run method, just before
	 * the method exits. This method sets necessary flags to mark the runnable as
	 * stopped and calls listeners.
	 * 
	 */
	protected void runEnd() {
		running = false;
		stopped = true;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RUN, 7));

		notifyWaitForSync();

		//Reset the stopRequested flag after notifying the listeners so listeners
		//can check if the stop has been requested or if the thread just ended
		stopRequested = false;
	}

	// ========================================================================

	/**
	 * Notifies the syncing object
	 * 
	 */
	private void notifyWaitForSync() {
		synchronized (WAITFORSYNC) {
			WAITFORSYNC.notify();
		}
	}
	
	// ========================================================================

	/**
	 * Requests the runnable to reset.<br />
	 * It depends on the implementation if the reset is available and how it behaves.
	 * A reset usually sets the running process back to a defined start state.
	 * 
	 */
	public void reset() {
		if (!resetImplemented) {
			throw new ControlledRunnableError("Resetting this runnable is not implemented.");
		}

		resetRequested = true;

		fireControlledRunnableListener(
				new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RESET, 8));

		if (paused) {
			//Wake up if pausing. The runPause method will decide if it exits from
			//pausing or not
			synchronized (PAUSESYNC) {
				PAUSESYNC.notify();
			}
		}
	}

	/**
	 * Has to be called wherever the reset point is in the program flow.
	 * 
	 */
	protected void runReset() {
		if (resetRequested) {
			resetRequested = false;

			fireControlledRunnableListener(
					new ControlledRunnableEvent(this, ControlledRunnableEvent.StateType.RESET, 9));
		}

	}

	/**
	 * Returns a flag which indicates if a reset has been requested
	 * 
	 * @return
	 */
	protected boolean isResetRequested() {
		return resetRequested;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public boolean willReset() {
		return resetRequested;
	}


}
