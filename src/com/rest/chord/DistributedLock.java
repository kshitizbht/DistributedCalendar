package com.rest.chord;

public class DistributedLock {

	  private boolean isLocked = false;
	  /**
	   * Method to lock and as long as it is locked it asks user the wait.
	   * @throws InterruptedException
	   */
	  public synchronized void lock() throws InterruptedException{
	    while(isLocked){
	      wait();
	    }
	    isLocked = true;
	  }
	  
	  /**
	   * Method to unlock, once unlocked it will notify each object waiting on the lock
	   */
	  public synchronized void unlock(){
	    isLocked = false;
	    notify();
	  }

	  /**
	   * Return the status of lock
	   * @return
	   */
	  public boolean isLocked(){
		  return isLocked;
	  }
}
