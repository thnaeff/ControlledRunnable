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
	
	
	@Test
	public void repeatingRunnableTest() throws Exception {
			
		RepeatingRunnableTestClass repeatingRunnable = new RepeatingRunnableTestClass();
		repeatingRunnable.addControlledRunnableListener(new RepeatingRunnableTestListener());
		
		Thread t = new Thread(repeatingRunnable);
		t.start();
		
		waitAndGo(repeatingRunnable);
		waitAndGo(repeatingRunnable);
		
		
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {}
		
		System.out.println("Stopping");
		repeatingRunnable.stop(true);
		
		System.out.println("Test done");
			
		
		
	}
	
	
	/**
	 * 
	 * 
	 * @param repeatingRunnable
	 */
	private void waitAndGo(RepeatingRunnableTestClass repeatingRunnable) {
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}
		
		System.out.println("Go");
		
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
			
			System.out.println("# Batch processor state changed: " + e.getStateType() + " -> " + stateTypeDetail);
			
			
		}
		
		
		
	}
	

}
