package com.nokia.ci.tas.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nokia.ci.tas.commons.MessageItem;

/**
 * Adapter for tas client object. Encapsulate client information and running tests information.
 *  
 * @author Frank Wang
 * @since Aug 21, 2012
 */
public class TacAdapter extends MessageItem<TacAdapter> {
	public TacAdapter() {
		this.setItemName( "CLIENT" );
	}

	/**
	 * all the connected clients related with according tests. 
	 */
	private Map<String, List<Test>> clients = new HashMap<String, List<Test>>();

	public Map<String, List<Test>> getClients() {
		return clients;
	}

	public void setClients( Map<String, List<Test>> clients ) {
		this.clients = clients;
	}
}
