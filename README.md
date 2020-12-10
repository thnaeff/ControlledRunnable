# ControlledRunnable
**A framework for controlling a java.lang.Runnable. Implements functionality for pause, reset and stop**

---


[![License](http://img.shields.io/badge/License-Apache_v2.0-802879.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Java Version](http://img.shields.io/badge/Java-1.6%2B-2E6CB8.svg)](https://java.com)
[![Apache Maven ready](http://img.shields.io/badge/Apache_Maven_ready-3.3.9%2B-FF6804.svg)](https://maven.apache.org/)


---


The `ControlledRunnable` implements `java.lang.Runnable` and provides functionality to pause, reset and stop a running process. Those procedures are usually the same for any running process, the process is requested to pause for example, so the program has to implement certain points in its flow where the process can be paused. Once the process is paused, it has to react to a request to continue running. The pausing of the process and resuming it is implemented in `ControlledRunnable` and can be used within a program flow with just one method call. The same applies to the reset and stop functionality.


## Listeners

Implementing `ControlledRunnableListener` and adding it to a `ControlledRunnable` implementation allows you to listen to the state of the runnable. The listener gets informed of any state change, e.g. if a pause is requested, once the pause is reached, if a stop is requested and once the stop is reached etc. To be precise, the listener event contains only the type of the state change (e.g. pause, reset, stop, ...), checking the source of the event (the controlled runnable) will give more detailed information about its state.


## Quick usage

To give an overview, here is a quick usage example.

Create a class and extend it with `ControlledRunnable`. Then implement your process and put the pausing, reset 
and stopping methods wherever they make sense.

```Java

	public class SomeRunnableClass extends ControlledRunnable {
	
		//...your code
	
		public void run() {
			runStart();
			
			//Main loop. Keeping the thread running
			while (! isStopRequested()) {
				//The reset point of the program could be here
				runReset();
				//Pause (if requested). Do not exit when reset is called.
				//This method implements the pausing and blocks as long as the pause state is set.
				runPause(boolean);
		
				if (isStopRequested()) {
					//Exit the while-loop to end the thread
					break;
				}
		
				if (isResetRequested()}) {
					//Reset could just mean "start again at the beginning". It is up to the
					//implementation on how to use it.
					continue;
				}
		
				//Do your stuff...
				//Use any of the offered methods to control the program flow
				//depending on the controlled runnable state.
				//Methods to use in addition to the ones mentioned already in this example:
				//controlledWait()
				//controlledWait(long)
				//isPauseRequested()
				//Any of the pause() methods to request a pause
				//
				//Hint: Instead of using wait() on a object directly, use one of the controlledWait
				//methods if you want the implementation to react to any pause/reset/stop etc. calls.
				//The controlledWait methods use the wait() internally but exit the wait if
				//reset() or stop() is called.
		
			}
		
			runEnd();
		}
	

	}

```

Start the runnable in a Thread

```Java

	SomeRunnableClass theRunnable = new SomeRunnableClass();
	Thread t = new Thread(theRunnable);
	t.start();
	
	//Do stuff
	theRunnable.pause(true);
	theRunnable.pause(false);
	theRunnable.reset();
	theRunnable.stop();
	
	
```


## Important Methods

The following methods will be needed to implement a `ControlledRunnable`

### To be used internally to control the program flow

* Methods to react to a state
	* isPauseRequested()
	* isResetRequested()
	* isStopRequested()

* Methods to perform what is needed to reach the state, e.g. if pause is requested, runPause has to be executed
	* controlledWait()
	* controlledWait(long)
	* runEnd()
	* runPause(boolean)
	* runReset()
	* runStart()



### To be used to control the runnable

* Request a state
	* pause(boolean)
	* pause(boolean, boolean)
	* pause(long)
	* reset()
	* stop()
	* stop(boolean)

* Check if the runnable is in the process of getting into the requested state
	* willPause()
	* willReset()
	* willStop()

* Check the current state of the runnable
	* isPaused()
	* isRunning()
	* isStopped()



-----


# RepeatingRunnable
**An extension of the ControlledRunnable to provide functionality to repeatedly execute a process and wait as long as the process is not in use**

The `RepeatingRunnable` simplifies running a process repeatedly. Its implementation waits for the go() command, 
and then executes the process as many times as specified in the go() method call (can be 0 for indefinitely, or 1 
for just one execution, ...).



## Quick usage

To give an overview, here is a quick usage example.

The only method which has to be implemented is the execute() method. Once the go() method is called on this class, 
the execute() method is called as many times as specified in the go() call.

```Java
	
	public class SomeRepeatingRunnable extends RepeatingRunnable {
	
		@Override
		public boolean execute() {
		
			//Your code...
			
			//Return and go into waiting state. If "false" is returned, this method 
			//is executed again (if multiple runs have been requested when go() was called).
			return true;
		}
	
	}

```


---


<img src="http://maven.apache.org/images/maven-logo-black-on-white.png" alt="Built with Maven" width="150">

This project can be built with Maven

Maven command:
```
$ mvn clean install
```

pom.xml entry in your project:
```
<dependency>
	<groupId>ch.thn.thread</groupId>
	<artifactId>controlledrunnable</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

