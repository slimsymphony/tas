package com.nokia.ci.tas.commons;



/**
 * Abstract class for all model classes.Provide base json transform ability.
 * 
 * @author Frank Wang
 * @since Jul 25, 2012
 */
public abstract class Message<T extends Message<?>>{
	
	public String toString() {
		return MonitorUtils.toJson( this );
	}
	
	/**
	 * Get Json string for this object.
	 * 
	 * @return
	 * @since Jul 31, 2012
	 */
	public String toJson() {
		return MonitorUtils.toJson( this );
	}
}
