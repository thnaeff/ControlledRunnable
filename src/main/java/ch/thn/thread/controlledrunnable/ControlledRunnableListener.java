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

import ch.thn.thread.controlledrunnable.ControlledRunnableEvent.StateType;


/**
 * Listener which gets notified of a state change in a {@link ControlledRunnable}
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public interface ControlledRunnableListener {

	/**
	 * This listener method is called when the state of the {@link ControlledRunnable} changes. 
	 * The state is indicated by the {@link StateType} value, which can be obtained with 
	 * {@link ControlledRunnableEvent#getStateType()}.<br />
	 * <br />
	 * The actual state has to be checked on the {@link ControlledRunnable} which caused the event. Here 
	 * is an example which checks all the states:<br />
	 * <pre>
	 * 
	 * 	ControlledRunnable runnable = (ControlledRunnable) e.getSource();
	 *	String state = null;
	 *	
	 *	switch (e.getStateType()) {
	 *	case PAUSE:
	 *		if (runnable.isPaused()) {
	 *			state = "paused";
	 *		} else if (runnable.willPause()) {
	 *			state = "will pause";
	 *		} else {
	 *			state = "un-paused";
	 *		}
	 *		break;
	 *	case RUN:
	 *		if (runnable.willStop()) {
	 *			//"Will stop" has to be tested before the running state. It is still running when the signal 
	 *			//to stop comes in.
	 *			state = "will stop";
	 *		} else if (runnable.isRunning()) {
	 *			state = "running";
	 *		} else if (runnable.isStopped()) {
	 *			state = "stopped";
	 *		} else {
	 *			state = "not running";
	 *		}
	 *		break;
	 *	case RESET:
	 *		if (runnable.willReset()) {
	 *			state = "will reset";
	 *		} else {
	 *			state = "reset";
	 *		}
	 *		break;
	 *	case WAIT:
	 *		state = "wait";
	 *		break;
	 *	default:
	 *		break;
	 *	}
	 * 
	 * </pre>
	 * 
	 * @param e The event object which contains all the event information
	 */
	public void runnableStateChanged(ControlledRunnableEvent e);

}
