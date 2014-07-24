package com.nokia.ci.tas.commons;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

/**
 * ItemType for Request messages.
 * 
 * @author Frank Wang
 * @since Jul 26, 2012
 */
public enum ItemType {
	TESTNODE, CLIENT, OPERATION, TESTNODEADAPTER, LOG, PRODUCT, TEST, NULL;

	private Map<String,String> attrs = new HashMap<String,String>();
	
	public Map<String,String> getAttrs(){
		return this.attrs;
	}
	
	public void addAttr(String key,String value) {
		this.attrs.put( key, value );
	}
	/**
	 * Parse string to ItemType.
	 * 
	 * @param str to be parsed
	 * @return Related ItemType. Or NULL if not found corresponding one.
	 * @since Jul 26, 2012
	 */
	public static ItemType parse( String str ) {
		if( str == null )
			return NULL;
		else if( str.startsWith( "{" ) ) {
			ItemType t = ItemType.OPERATION;
			t.attrs = MonitorUtils.fromJson( str, new TypeToken<Map<String,String>>(){}.getType() );
			return t;
		}
		for ( ItemType item : ItemType.values() ) {
			if ( item.name().equalsIgnoreCase( str ) ) {
				return item;
			}
		}
		return NULL;
	}
}
