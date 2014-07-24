package com.nokia.ci.tas.commons;

/**
 * Parent Class for all monitor message pieces.
 * @author Frank Wang
 * @since Jul 31, 2012 
 * @param <V>
 */
public class MessageItem<V extends MessageItem<?>> extends Message<V> {

	/**
	 * name of message item.
	 */
	private String itemName;

	public String getItemName() {
		return itemName;
	}

	public void setItemName( String itemName ) {
		this.itemName = itemName;
	}
	
	/**
	 * Provide method For release resources. 
	 * 
	 * @since Feb 20, 2013
	 */
	public void free() {
		return;
	}
}
