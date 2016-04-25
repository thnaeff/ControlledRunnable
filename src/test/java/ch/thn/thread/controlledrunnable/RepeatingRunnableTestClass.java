package ch.thn.thread.controlledrunnable;
import ch.thn.thread.controlledrunnable.RepeatingRunnable;


/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class RepeatingRunnableTestClass extends RepeatingRunnable {
	
	private static int runCount = 0;
	
	private Object monitor = new Object();

	@Override
	public boolean execute() {
		
		runCount++;
		int runCountLocal = runCount;
		
		System.out.println(RepeatingRunnableTestClass.class.getSimpleName() + " execute() started: " + runCountLocal);
		
		synchronized (monitor) {
			try {
				monitor.wait(1000);
			} catch (InterruptedException e) {}
		}
		
		
		System.out.println(RepeatingRunnableTestClass.class.getSimpleName() + " execute() ended: " + runCountLocal);
		
		//Pause after
		return true;
	}

}
