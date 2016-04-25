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

import java.util.EventObject;

/**
 * Indicates an event with one of the {@link StateType}s
 * 
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class ControlledRunnableEvent extends EventObject {
	private static final long serialVersionUID = -7869931124296779698L;

	/**
	 * The type of the event
	 *
	 */
	public static enum StateType {
		/**
		 * The run state has changed. Either running started, stop requested or
		 * stopped.
		 * 
		 */
		RUN,

		/**
		 * The pause state has changed. Either pausing requested, paused or not paused
		 * any more.
		 * 
		 */
		PAUSE,

		/**
		 * The reset state has changed. Either reset requested or reset done.
		 * 
		 */
		RESET,

		/**
		 * The waiting state has changed.
		 * 
		 */
		WAIT;
	}
	
	/**
	 * Detailed state type
	 *
	 */
	public static enum StateTypeDetail {
		PAUSED, WILL_PAUSE, UN_PAUSED, WILL_UN_PAUSE, 
		WILL_STOP, RUNNING, STOPPED, NOT_RUNNING, 
		WILL_RESET, RESET, 
		WAIT;
	}

	private final StateType stateType;
	private final int locationIdentifier;

	/**
	 * 
	 * 
	 * @param source
	 * @param stateType
	 * @param locationIdentifier
	 */
	public ControlledRunnableEvent(Object source, StateType stateType, int locationIdentifier) {
		super(source);
		this.stateType = stateType;
		this.locationIdentifier = locationIdentifier;
	}

	/**
	 * Returns the state type which has changed
	 * 
	 * @return
	 */
	public StateType getStateType() {
		return stateType;
	}
	
	/**
	 * Internal use only!
	 * 
	 * @return
	 */
	public int getLocationIdentifier() {
		return locationIdentifier;
	}
	
	/**
	 * Returns the detailed state of the source runnable at the moment of this call
	 * 
	 * @return
	 */
	public StateTypeDetail getStateTypeDetail() {
		ControlledRunnable runnable = (ControlledRunnable) getSource();
		
		switch (stateType) {
			case PAUSE:
				if (runnable.willPause()) {
					return StateTypeDetail.WILL_PAUSE;
				} else if (runnable.willUnPause()) {
					return StateTypeDetail.WILL_UN_PAUSE;
				} else if (runnable.isPaused()) {
					return StateTypeDetail.PAUSED;
				} else {
					return StateTypeDetail.UN_PAUSED;
				}
			case RUN:
				if (runnable.willStop()) {
					//"Will stop" has to be tested before the running state. It is still running when the signal 
					//to stop comes in.
					return StateTypeDetail.WILL_STOP;
				} else if (runnable.isRunning()) {
					return StateTypeDetail.RUNNING;
				} else if (runnable.isStopped()) {
					return StateTypeDetail.STOPPED;
				} else {
					return StateTypeDetail.NOT_RUNNING;
				}
			case RESET:
				if (runnable.willReset()) {
					return StateTypeDetail.WILL_RESET;
				} else {
					return StateTypeDetail.RESET;
				}
			case WAIT:
				return StateTypeDetail.WAIT;
			default:
				throw new ControlledRunnableError("Unknown state type " + stateType);
		}
		
	}

}
