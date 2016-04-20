import ch.thn.thread.controlledrunnable.RepeatingRunnable;


/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class RepeatingRunnableTestClass extends RepeatingRunnable {

	@Override
	public boolean execute() {
		
		System.out.println("RepeatingRunnable execute()");
		
		//Pause after
		return true;
	}

}
