package com.nokia.ci.tas.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * Request object for envelop request for monitoring.
 * Including request message types.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class Request extends Message<Request> {

	private List<String> items = new ArrayList<String>();

	public List<String> getItems() {
		return items;
	}

	public void addItem( String item ) {
		this.items.add( item );
	}

	public void setItems( List<String> items ) {
		this.items = items;
	}

}
