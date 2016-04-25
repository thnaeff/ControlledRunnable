package ch.thn.thread.controlledrunnable;
import org.junit.Test;

import ch.thn.thread.controlledrunnable.ControlledRunnableEvent;
import ch.thn.thread.controlledrunnable.ControlledRunnableListener;
import ch.thn.thread.controlledrunnable.ControlledRunnableEvent.StateTypeDetail;


/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class RepeatingRunnableTest {
	
	private static int goCount = 0;
	
	
	@Test
	public void repeatingRunnableTest() throws Exception {
			
		RepeatingRunnableTestClass repeatingRunnable = new RepeatingRunnableTestClass();
		repeatingRunnable.addControlledRunnableListener(new RepeatingRunnableTestListener());
		
		Thread t = new Thread(repeatingRunnable);
		t.start();
		
		Thread.sleep(1000);
		
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		
		waitAndGo(repeatingRunnable, true);
		
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		waitAndGo(repeatingRunnable, false);
		
		waitAndGo(repeatingRunnable, true);
		waitAndGo(repeatingRunnable, true);
		waitAndGo(repeatingRunnable, true);
		
		
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		System.out.println("Stopping");
		repeatingRunnable.stop(true);
		
		System.out.println("Test done");
			
		
		
	}
	
	
	/**
	 * 
	 * 
	 * 
	 * @param repeatingRunnable
	 * @param pause
	 */
	private void waitAndGo(RepeatingRunnableTestClass repeatingRunnable, boolean pause) {
		
		if (pause) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		goCount++;
		
		System.out.println("Go " + goCount);
		
		repeatingRunnable.go();
		
	}
	
	
	
	/********************************************************************************************************
	 * 
	 * 
	 *
	 * @author Thomas Naeff (github.com/thnaeff)
	 *
	 */
	private class RepeatingRunnableTestListener implements ControlledRunnableListener {

		@Override
		public void runnableStateChanged(ControlledRunnableEvent e) {
			
			StateTypeDetail stateTypeDetail = e.getStateTypeDetail();
			
			System.out.println("# Batch processor state changed: " 
			+ e.getStateType() + " -> " + stateTypeDetail + " (" + e.getLocationIdentifier() + ")");
			
			
		}
		
		
		
	}
	

}
