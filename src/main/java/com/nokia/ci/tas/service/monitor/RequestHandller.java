package com.nokia.ci.tas.service.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.nokia.ci.tas.commons.ItemType;
import com.nokia.ci.tas.commons.MessageItem;
import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.commons.OperationResult;
import com.nokia.ci.tas.commons.Request;
import com.nokia.ci.tas.commons.Response;

/**
 * Handler for handle monitor requests.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class RequestHandller implements Runnable {

	private Socket socket;
	private ServiceListener listener;

	public RequestHandller( Socket socket, ServiceListener listener ) {
		if ( socket == null || !socket.isConnected() || !socket.isBound() || socket.isClosed() )
			throw new RuntimeException( "The Socket to be handled is not valid." );
		this.socket = socket;
		this.listener = listener;
	}

	@Override
	public void run() {
		System.out.println( "RequestHandller starting. Handle request from " + socket.getRemoteSocketAddress() );
		Timer t = new Timer();
		t.schedule( new TimerTask() {
			@Override
			public void run() {
				try {
					socket.close();
				} catch ( IOException e ) {
					System.out.println( "Mandatory closing socket request for timeout: " + socket.getRemoteSocketAddress() );
				}
			}}, new java.util.Date(System.currentTimeMillis() + 1000*30) );
		
		handle( socket );
		t.cancel();
		System.out.println( "RequestHandller ended. Handle request from " + socket.getRemoteSocketAddress() );
	}

	/**
	 * Handle incoming request and gather info according to request, then send result back.
	 * 
	 * @param socket
	 * @since Jul 31, 2012
	 */
	private void handle( Socket socket ) {
		InputStream in = null;
		OutputStream os = null;
		try {
			in = socket.getInputStream();
			byte[] data = new byte[1024];
			int read = 0;
			ByteArrayOutputStream bos = new ByteArrayOutputStream( 500 );
			while ( ( read = in.read( data ) ) != -1 ) {
				bos.write( data, 0, read );
				if ( read < 1024 )
					break;
			}
			bos.close();
			String jsonStr = new String( bos.toByteArray(), "ISO-8859-1" );
			Request request = MonitorUtils.fromJson( jsonStr, Request.class );
			if ( request == null ) {
				System.out.println( "Receive a empty Request from " + socket.getRemoteSocketAddress() );
				return;
			}
			Response response = new Response();
			if ( request != null ) {
				for ( String itemstr : request.getItems() ) {
					System.out.println( "Handle Item:" + itemstr );
					ItemType item = ItemType.parse( itemstr );
					if ( item.equals( ItemType.NULL ) ) {
						continue;
					} else if ( item.equals( ItemType.OPERATION ) ) {
						response.addItem( operation( item.getAttrs() ) );
					} else {
						response.addItem( gatherInfo( item ) );
					}
				}
			}
			os = socket.getOutputStream();
			os.write( response.toJson().getBytes( "ISO-8859-1" ) );
			os.flush();
		} catch ( Exception e ) {
			System.out.println( "Handle Request Error :" + socket.getRemoteSocketAddress() + MonitorUtils.getStack( e ) );
		} finally {
			MonitorUtils.close( os );
			MonitorUtils.close( in );
			MonitorUtils.close( socket );
		}
	}

	private MessageItem<?> operation( Map<String, String> attrs ) {
		OperationResult item = new OperationResult();
		if ( attrs != null && attrs.size() > 0 ) {
			for ( String key : attrs.keySet() ) {
				String value = attrs.get( key );
				if ( "ResetTargetStatus".equalsIgnoreCase( key ) ) {
					String result = listener.resetTargetInfo( value );
					if(result!=null) {
						item.addResult( key, result );
					}else {
						item.addResult( key, "Failed" );
					}
				} else if ("ResetClientInfo".equalsIgnoreCase( key )) {
					String result = listener.resetClientInfo( value );
					if(result!=null) {
						item.addResult( key, result );
					}else {
						item.addResult( key, "Failed" );
					}
				} else if( "RemoveTarget".equalsIgnoreCase( key ) ) {
					String result = listener.removeTarget( value );
					if(result!=null) {
						item.addResult( key, result );
					}else {
						item.addResult( key, "Failed" );
					}
				} else if( "ResetTarget".equalsIgnoreCase( key ) ) {
					String result = listener.resetTarget( value );
					if(result!=null) {
						item.addResult( key, result );
					}else {
						item.addResult( key, "Failed" );
					}
				} else if( "StopTest".equalsIgnoreCase( key ) ) {
					String result = listener.stopTest( value );
					if(result!=null) {
						item.addResult( key, result );
					}else {
						item.addResult( key, "Failed" );
					}
				}
			}
		}
		return item;
	}

	/**
	 * Gather information according to itemType.
	 * 
	 * @param itemtype
	 * @return MessageItem to be transfer back to monitor client.
	 * @since Jul 27, 2012
	 */
	private MessageItem<?> gatherInfo( ItemType itemtype ) {
		MessageItem<?> item = null;
		switch ( itemtype ) {
			case TESTNODEADAPTER:
				item = listener.gatherNodes();
				break;
			case CLIENT:
				item = listener.gatherClients();
				break;
		}
		return item;
	}
}
