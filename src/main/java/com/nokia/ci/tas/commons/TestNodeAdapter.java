package com.nokia.ci.tas.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Represent a running tas test node.
 *  
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class TestNodeAdapter extends MessageItem<TestNodeAdapter> {
	
	public TestNodeAdapter() {
		this.setItemName( "TestNodeAdapter" );
	}

	/**
	 * test node and it's connected targets.
	 */
	private Map<String,List<Product>> nodes = new HashMap<String,List<Product>>();

	public Map<String, List<Product>> getNodes() {
		return nodes;
	}

	public void setNodes( Map<String, List<Product>> nodes ) {
		this.nodes = nodes;
	}
	
}
