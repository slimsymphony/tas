package com.nokia.ci.tas.communicator.monitor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.communicator.TestAutomationCommunicator;

/**
 * Listener for Communicator. accept monitor request and dispatch request to handler.  
 * 
 * @author Frank Wang
 * @since Aug 21, 2012
 */
public class MonitorListener extends Thread {

	private final static int DEFAILT_PORT = 5452;
	private int port; // default port is 5451
	private ServerSocket listener;
	/**
	 * running flag
	 */
	private boolean isRunning = true;
	/**
	 * Thread pool, with cached size
	 */
	private ExecutorService pool = Executors.newCachedThreadPool();
	
	/**
	 * Listener for gather test node information.
	 */
	private TestNodeListener nodeListener;

	/**
	 * Constructor of MonitorListener
	 * 
	 * @param testAutomationCommunicator instance of tas service.
	 * @since Aug 21, 2012
	 */
	public MonitorListener( TestAutomationCommunicator testAutomationCommunicator ) {
		this( DEFAILT_PORT, testAutomationCommunicator );
	}
	
	/**
	 * Constructor of MonitorListener
	 * 
	 * @param port assigned socket port number.
	 * @param testAutomationCommunicator instance of tas service.
	 * @since Aug 21, 2012
	 */
	public MonitorListener( int port, TestAutomationCommunicator testAutomationCommunicator ) {
		this.port = port;
		this.setDaemon( true ); //this listener won't effect normal process.
		this.nodeListener = new TestNodeListener( testAutomationCommunicator );
	}
	
	/**
	 * Reset the running flag to make it work.
	 * 
	 * @since Aug 21, 2012
	 */
	public void restart() {
		this.isRunning = true;
		System.out.println( "Restart MonitorListener ..." );
	}

	/**
	 * Set running flag to pause.
	 * 
	 * @since Aug 21, 2012
	 */
	public void pause() {
		this.isRunning = false;
		System.out.println( "Pause MonitorListener ..." );
	}

	@Override
	public void run() {
		System.out.println( "MonitorListener starting..." );
		Socket socket = null;
		try {
			while ( true ) {
				listener = new ServerSocket( port ); // use default's 50 as queue size.
				while ( isRunning ) {
					try {
						socket = listener.accept();
						pool.execute( new RequestHandller( socket, nodeListener ) );
					} catch ( Exception e ) {
						System.err.println( "MonitorListerner got Exception while received a request:" + MonitorUtils.getStack(e) );
					}
					sleep( Constant.DECISECOND );
				}
				sleep( Constant.DECISECOND );
			}
		} catch ( IOException e ) {
			System.err.println( "MonitorListerner met IOException:" + MonitorUtils.getStack(e) );
		} catch ( InterruptedException e ) {
			System.err.println( "MonitorListerner met InterruptedException:" + MonitorUtils.getStack(e) );
		} catch ( Exception e ) {
			System.err.println( "MonitorListerner met Error:" + MonitorUtils.getStack(e) );
		} finally {
			MonitorUtils.close( listener );
		}
		System.out.println( "MonitorListener ending..." );
	}

}
