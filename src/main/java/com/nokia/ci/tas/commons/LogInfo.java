package com.nokia.ci.tas.commons;

import java.util.Map;

import com.nokia.ci.tas.commons.MessageItem;

/**
 * Represent Log Files on Communicator side.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class LogInfo extends MessageItem<LogInfo> {

	public LogInfo() {
		this.setItemName( "Log" );
	}
	
	private Map<String,byte[]> logs;

	public Map<String, byte[]> getLogs() {
		return logs;
	}

	public void setLogs( Map<String, byte[]> logs ) {
		this.logs = logs;
	}
	
	@Override
	public void free() {
		if(logs!=null)
			logs.clear();
	}
}
