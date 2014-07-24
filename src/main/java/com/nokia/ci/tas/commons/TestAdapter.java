package com.nokia.ci.tas.commons;

import java.util.Map;

/**
 * Test Adapter for encapsulate original Test class.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class TestAdapter extends MessageItem<TestAdapter> {
	
	public TestAdapter() {
		this.setItemName( "Test" );
	}
	
	/**
	 * waiting requests number.
	 */
	private int waitingRequests;
	
	/**
	 * All running tests.
	 */
	private Map<Test,String> tests;

	public int getWaitingRequests() {
		return waitingRequests;
	}

	public void setWaitingRequests( int waitingRequests ) {
		this.waitingRequests = waitingRequests;
	}

	public Map<Test,String> getTests() {
		return tests;
	}

	public void setTests( Map<Test,String> tests ) {
		this.tests = tests;
	}
	
	@Override
	public void free() {
		if(tests!=null)
			tests.clear();
	}
	
}
