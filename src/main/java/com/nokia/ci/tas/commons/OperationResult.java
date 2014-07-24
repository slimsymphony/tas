package com.nokia.ci.tas.commons;

import java.util.HashMap;
import java.util.Map;

import com.nokia.ci.tas.commons.MessageItem;

/**
 * Operation result, save the execution result of remote operation.
 *  
 * @author Frank Wang
 * @since Sep 3, 2012
 */
public class OperationResult extends MessageItem<OperationResult> {
	/**
	 * Constructor of OperationResult
	 * 
	 * @since Sep 3, 2012
	 */
	public OperationResult() {
		this.setItemName( "OperationResult" );
	}

	/**
	 * result container
	 */
	private Map<String, String> results = new HashMap<String, String>();

	/**
	 * Add a new result record.
	 *  
	 * @param op operation 
	 * @param result result info
	 * @since Sep 3, 2012
	 */
	public void addResult( String op, String result ) {
		results.put( op, result );
	}

	/**
	 * Get all operation results infos.
	 * 
	 * @return
	 * @since Sep 3, 2012
	 */
	public Map<String, String> getResults() {
		return results;
	}
}
