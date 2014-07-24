package com.nokia.ci.tas.service.monitor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.service.TestAutomationService;

/**
 * Listener for tas service. accept monitor request and dispatch request to handler.  
 * 
 * @author Frank Wang
 * @since Aug 21, 2012
 */
public class MonitorListener extends Thread {

	private final static int DEFAULT_PORT = 5451;
	private int port; // default port is 5451
	private ServerSocket listener;
	private boolean isRunning = true;
	private ExecutorService pool = Executors.newCachedThreadPool();
	private ServiceListener nodeListener;

	/**
	 * Constructor of MonitorListener
	 * 
	 * @param testAutomationService
	 * @since Aug 21, 2012
	 */
	public MonitorListener( TestAutomationService testAutomationService ) {
		this( DEFAULT_PORT, testAutomationService );
	}

	/**
	 * Constructor of MonitorListener
	 * 
	 * @param port
	 * @param testAutomationService
	 * @since Aug 21, 2012
	 */
	public MonitorListener( int port, TestAutomationService testAutomationService ) {
		this.port = port;
		if( port <= 0 )
			this.port = DEFAULT_PORT;
		this.setDaemon( true );
		this.nodeListener = new ServiceListener( testAutomationService );
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
						System.out.println( "MonitorListener Received a request..." );
						pool.execute( new RequestHandller( socket, nodeListener ) );
					} catch ( Exception e ) {
						System.out.println( "MonitorListerner got Exception while received a request:" + MonitorUtils.getStack(e) );
					}
					sleep( Constant.DECISECOND );
				}
				sleep( Constant.DECISECOND );
			}
		} catch ( IOException e ) {
			System.out.println( "MonitorListerner met IOException:" + MonitorUtils.getStack(e) );
		} catch ( InterruptedException e ) {
			System.out.println( "MonitorListerner met InterruptedException:" + MonitorUtils.getStack(e) );
		} catch ( Exception e ) {
			System.out.println( "MonitorListerner met Error:" + MonitorUtils.getStack(e) );
		} finally {
			MonitorUtils.close( listener );
		}
		System.out.println( "MonitorListener ending..." );
	}

}
