package com.nokia.ci.tas.communicator.monitor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

import com.nokia.ci.tas.commons.LogInfo;
import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.commons.OperationResult;
import com.nokia.ci.tas.commons.ProductAdapter;
import com.nokia.ci.tas.commons.TestAdapter;
import com.nokia.ci.tas.commons.TestNode;
import com.nokia.ci.tas.commons.ItemType;
import com.nokia.ci.tas.commons.MessageItem;
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
	private TestNodeListener listener;
	
	public RequestHandller( Socket socket, TestNodeListener listener ) {
		if ( socket == null || !socket.isConnected() || !socket.isBound() || socket.isClosed() )
			throw new RuntimeException( "The Socket to be handled is not valid." );
		this.socket = socket;
		this.listener = listener;
	}

	@Override
	public void run() {
		System.out.println( "Start handling a monitor request..." );
		handle( socket );
		System.out.println( "Finished handing a monitor request..." );
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
		MessageItem<?> mi = null;
		Response response = null;
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
			String jsonStr = new String( MonitorUtils.decompress( bos.toByteArray() ), "ISO-8859-1" );
			Request request = MonitorUtils.fromJson( jsonStr, Request.class );
			response = new Response();
			if ( request != null ) {
				for ( String itemstr : request.getItems() ) {
					ItemType item = ItemType.parse( itemstr );
					if ( item.equals( ItemType.NULL ) )
						continue;
					else if(item.equals( ItemType.OPERATION )) {
						mi = operation( item.getAttrs() );
					}else {
						mi = gatherInfo( item );
					}
					if(mi != null) {
						response.addItem( mi );
					} else {
						System.out.println( "Empty Item Value got" );
					}
				}
			}
			os = socket.getOutputStream();
			os.write( MonitorUtils.compressData( response.toJson() ) );
			os.flush();
		} catch ( Exception e ) {
			System.err.println( "Handle Request Error :" + socket.getRemoteSocketAddress() + ":" + socket.getPort() + MonitorUtils.getStack(e) );
		} finally {
			if( response!=null ) {
				for(MessageItem<?> item : response.getItems()) {
					item.free();
				}
			}
			MonitorUtils.close( os );
			MonitorUtils.close( in );
			MonitorUtils.close( socket );
		}
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
		try {
			switch ( itemtype ) {
				case TESTNODE:
					item = new TestNode();
					listener.gatherTestNode( ( TestNode ) item );
					break;
				case LOG:
					item = new LogInfo();
					listener.gatherLog( ( LogInfo ) item );
					break;
				case PRODUCT:
					item = new ProductAdapter();
					listener.gatherProduct( (ProductAdapter) item );
					break;
				case TEST:
					item = new TestAdapter();
					listener.gatherTests( ( TestAdapter ) item );
					break;
				case OPERATION:
					item = operation( itemtype.getAttrs() );
				} 
		}catch(Throwable e) {
			System.out.println( "Gather info met problem, item:"+itemtype + MonitorUtils.getStack(e) );
		}
		return item;
	}

	private MessageItem<?> operation( Map<String, String> attrs ) {
		OperationResult item = new OperationResult();
		if ( attrs != null && attrs.size() > 0 ) {
			for ( String key : attrs.keySet() ) {
				String value = attrs.get( key );
				if ( "StopTest".equalsIgnoreCase( key ) ) {
					if( listener.stopTest( value ) ) {
						item.addResult( key, "Success" );
					}else {
						item.addResult( key, "Failed" );
					}
				}else if( "FreeProduct".equalsIgnoreCase( key ) ) {
					if( listener.freeProduct( value ) ) {
						item.addResult( key, "Success" );
					}else {
						item.addResult( key, "Failed" );
					}
				}else if( "RemoveProduct".equalsIgnoreCase( key ) ) {
					if( listener.removeProduct( value ) ) {
						item.addResult( key, "Success" );
					}else {
						item.addResult( key, "Failed" );
					}
				}
			}
		}
		return item;
	}
}
