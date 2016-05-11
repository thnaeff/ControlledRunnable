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
package ch.thn.thread.buffer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A buffer which stores elements and their index. The {@link OrderedBlockingBuffer} 
 * takes care that the elements are retrieved in their index order. <br />
 * <br />
 * <br />
 * About the buffer:<br />
 * As long as the buffer level is below the maxBufferSize, all new elements are added to the 
 * buffer. As soon as this threshold is reached, two things might happen: <br />
 * - The buffer is full, but the next elements which should be retrieved is not in the buffer (yet). 
 * In this case, new elements will still be added to the buffer until the next needed 
 * elements is encountered. This will cause the buffer to grow over the specified maxBufferSize.<br />
 * - The buffer is full and the next elements is already in the buffer. In this case, the 
 * {@link #put(int, Object)} method blocks as long as elements are taken out. As soon as
 * the buffer level falls under the maxBufferSize, adding of new elements will be possible again.<br />
 * <br />
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class OrderedBlockingBuffer<K> {
		
	private Map<Long, K> bufferMap = null;
		
	/**
	 * The buffer size.
	 */
	private int maxBufferSize = 0;
	
	private long currentIndex = 0;
	
	private volatile boolean waitingForEmptyBuffer = false;
	private volatile boolean waitingForSpaceInBuffer = false;	
	private volatile boolean end = false;

	
	/**
	 * Creates a new {@link OrderedBlockingBuffer} with a user defined start index. 
	 * The first element it can retrieve with {@link #take()} is an element with 
	 * that user defined start index, which means that such an element has to exist.
	 * 
	 * @param maxBufferSize
	 * @param startIndex The index of the first element to look for.
	 */
	public OrderedBlockingBuffer(int maxBufferSize, int startIndex) {
		this.maxBufferSize = maxBufferSize;
		
		bufferMap = new HashMap<Long, K>(maxBufferSize);
		
	}
	
	/**
	 * Creates a new {@link OrderedBlockingBuffer} with a start index of <code>0</code>. 
	 * The first element it can retrieve with {@link #take()} is an element with 
	 * index <code>0</code>, which means that such an element has to exist.
	 * 
	 * @param maxBufferSize
	 */
	public OrderedBlockingBuffer(int maxBufferSize) {
		this(maxBufferSize, 0);
	}
	
	/**
	 * Returns <code>true</code> if the element is currently in the buffer
	 * 
	 * @param element
	 * @return
	 */
	public boolean containsElement(K element) {
		return bufferMap.containsValue(element);
	}
	
	/**
	 * Returns <code>true</code> if an element with the given index is currently in the buffer
	 * 
	 * @param elementIndex
	 * @return
	 */
	public boolean containsIndex(long elementIndex) {
		return bufferMap.containsKey(elementIndex);
	}
	
	/**
	 * Adds an element to the map. Blocks if the buffer is full and data is being written out. 
	 * Continues adding element if the buffer is full but the next expected element is not yet in 
	 * the buffer.
	 * 
	 * @param elementIndex
	 * @param element
	 */
	public void put(long elementIndex, K element) {
		
		int currentBufferSize = 0;
		synchronized (bufferMap) {
			currentBufferSize = bufferMap.size();
		}
		
		if (currentBufferSize >= maxBufferSize) {
			//The buffer is full

			boolean bufferContainsNext = false;
			synchronized (bufferMap) {
				//If the next one will just be added or the next one is already in the ma
				bufferContainsNext = elementIndex == currentIndex || bufferMap.containsKey(currentIndex);
			}
			
			if (bufferContainsNext) {

				//Buffer is full and the next element is already in the buffer. 
				//Wait until retrieving has caught up and there is space available.
				waitForSpaceInBuffer(elementIndex);
			}// else {
				//The next element has not appeared in the buffer yet. Keep 
				//adding elements until the next element is found.
			//}

		}
		
		synchronized (bufferMap) {
			bufferMap.put(elementIndex, element);
			//Only notify if the next item is in the map
			if (elementIndex == currentIndex) {
				bufferMap.notifyAll();
			}
		}
		
	}
	
	/**
	 * Blocks until there is space available in the buffer. Does not block, however, 
	 * if the elementIndex is the element which is expected next or if the next needed element 
	 * is not in the buffer yet.
	 * 
	 * @param elementIndex
	 */
	private void waitForSpaceInBuffer(long elementIndex) {
		
		synchronized (bufferMap) {
			while (bufferMap.size() >= maxBufferSize) {
				//The buffer is full
				if (elementIndex == currentIndex || ! bufferMap.containsKey(currentIndex)) {
					//Do not wait if the new element is the next one which will be retrieved.
					break;
				} else {
					//Otherwise, wait until there is space in the buffer again
					
					waitingForSpaceInBuffer = true;
					
					try {
						bufferMap.wait();
					} catch (InterruptedException e) { break; }
					
					waitingForSpaceInBuffer = false;
				}
				
				if (end) {
					break;
				}
				
			}
		}
	
	}
	
	/**
	 * Returns the index of the element which is retrieved by the next {@link #take()} call.
	 * 
	 * @return
	 */
	public long nextElementIndex() {
		return currentIndex;
	}
	
	/**
	 * Checks if the next item in the buffer is available. The next item is defined by the 
	 * element index, therefore even though there are elements in the buffer, the next item 
	 * might or might not be available.
	 * 
	 * @return
	 */
	public boolean isNextAvailable() {
		synchronized (bufferMap) {
			return bufferMap.containsKey(currentIndex);
		}
	}
	
	/**
	 * Returns the current buffer level (the number of items in the buffer)
	 *  
	 * @return
	 */
	public int level() {
		synchronized (bufferMap) {
			return bufferMap.size();
		}
	}
	
	/**
	 * Resets/clears the buffer. Sets the current index to the given number. The current index 
	 * defines the element index of the item which is next in the buffer.
	 * 
	 * @param currentIndex
	 */
	public void reset(long currentIndex) {
		bufferMap.clear();
		this.currentIndex = currentIndex;
	}
	
	/**
	 * Resets/clears the buffer. The next item the buffer expects has an index of 0.
	 * 
	 */
	public void reset() {
		reset(0);
	}
	
	/**
	 * Retrieves and removes the next element in order. Waits if the next element is 
	 * not available.
	 * 
	 * @return
	 */
	public K take() {
		return take(0, null);
	}
	
	/**
	 * Retrieves and removes the next element in order. Waits if the next element is 
	 * not available.
	 * 
	 * @param timeout how long to wait before giving up, in units of unit
	 * @param unit a TimeUnit determining how to interpret the timeout parameter
	 * @return The next value as soon as the value is available, or NULL if the waiting 
	 * got interrupted.
	 */
	public K take(long timeout, TimeUnit unit) {
		K element = null;
		
		long waitTime = (unit == null ? 0 : unit.toMillis(timeout));
		long startWaitTime = 0;
		if (waitTime > 0) {
			startWaitTime = System.currentTimeMillis();
		}
		
		synchronized (bufferMap) {
			//Wait as long as next line is not in the buffer
			while (! bufferMap.containsKey(currentIndex)) {
				
				try {
					bufferMap.wait(waitTime);
				} catch (InterruptedException e) {
					return null;
				}
				
				if (waitTime > 0) {
					long waitedTime = System.currentTimeMillis() - startWaitTime;
					//Re-calculate how long is left to wait
					waitTime -= waitedTime;
					
					//Wait time is up. Element was not available until now so just return null
					if (waitTime <= 0) {
						return null;
					}
				}
				
				if (end) {
					return null;
				}
			}
			
			element = bufferMap.remove(currentIndex);
		}
		
		//Send notification if waiting for the buffer to be empty
		if (waitingForEmptyBuffer) {
			synchronized (bufferMap) {
				if (bufferMap.isEmpty()) {
					bufferMap.notifyAll();
				}
			}
		}
		
		//Send notification if waitng for free space, since a line 
		//has just been removed from the buffer
		if (waitingForSpaceInBuffer) {
			synchronized (bufferMap) {
				//Only notify if there is space available
				if (bufferMap.size() <= maxBufferSize) {
					//Notify now. Maybe there is enough space now?
					bufferMap.notifyAll();
				}
			}
		}
		
		currentIndex++;
		
		return element;
	}
	
	/**
	 * Interrupts when waiting for next object. Causes {@link #take()} to return NULL.
	 * 
	 */
	public void interrupt() {
		end = true;
		
		synchronized (bufferMap) {
			bufferMap.notifyAll();
		}
	}
	
	
	/**
	 * Blocks until all elements are retrieved and the internal buffer is empty
	 * 
	 */
	public void waitForEmptyBuffer() {
		waitingForEmptyBuffer = true;
		
		synchronized (bufferMap) {
			while (! bufferMap.isEmpty()) {
				try {
					bufferMap.wait();
				} catch (InterruptedException e) { break; }
			}
		}
				
		waitingForEmptyBuffer = false;
	}
	
	

}
