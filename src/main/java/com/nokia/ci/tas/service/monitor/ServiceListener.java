package com.nokia.ci.tas.service.monitor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.TacAdapter;
import com.nokia.ci.tas.commons.TestNodeAdapter;
import com.nokia.ci.tas.commons.Product.Status;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.service.RemoteClient;
import com.nokia.ci.tas.service.TestAutomationService;
import com.nokia.ci.tas.service.TestMonitor;
import com.nokia.ci.tas.service.TestNode;

/**
 * Listener for tas service, could gather test node's information. Including TestNodes information/tas client status.
 * 
 * @author Frank Wang
 * @since Jul 25, 2012
 */
public class ServiceListener {

	private TestAutomationService testAutomationService;

	/**
	 * Constructor of ServiceListener
	 * 
	 * @param testAutomationService
	 * @since Aug 21, 2012
	 */
	public ServiceListener( TestAutomationService testAutomationService ) {
		this.testAutomationService = testAutomationService;
	}

	/**
	 * Gather information of testNodes.
	 * 
	 * @return
	 * @since Aug 21, 2012
	 */
	public TestNodeAdapter gatherNodes() {
		TestNodeAdapter ta = new TestNodeAdapter();
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "testNodes" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<TestNode> nodes = (Collection<TestNode>)f.get( testAutomationService );
			for ( TestNode node : nodes ) {
				ta.getNodes().put( node.getHostnameAndPort(), node.getProducts() );
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ta;
	}

	/**
	 * Gather information of all tas client(CI side)
	 * 
	 * @return
	 * @since Aug 21, 2012
	 */
	public TacAdapter gatherClients() {
		TacAdapter ta = new TacAdapter();
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "remoteClients" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<RemoteClient> clients = (Collection<RemoteClient>)f.get( testAutomationService );
			if( clients != null )
				for ( RemoteClient rc : clients ) {
					List<Test> ts = new ArrayList<Test>();
					f = rc.getClass().getDeclaredField( "tests" );
					f.setAccessible( true );
					@SuppressWarnings( "unchecked" )
					Collection<Test> tests = (Collection<Test>)f.get( rc );
					if( tests != null )
						ts.addAll( tests );
					ta.getClients().put( rc.getClientHostnameAndPort(), ts );
				}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ta;
	}

	/**
	 * Reset the status of assigning target.
	 * 
	 * @param imei IMEI number for the target
	 * @since Sep 3, 2012
	 */
	public String resetTargetInfo( String imei ) {
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "testNodes" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<TestNode> nodes = (Collection<TestNode>)f.get( testAutomationService );
			for ( TestNode node : nodes ) {
				for ( Product p : node.getProducts() ) {
					if ( p.getIMEI().equals( imei ) ) {
						p.setStatusDetails( "" );
						p.setStatus( Status.FREE );
						return node.getHostnameAndPort();
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Remove all the test info from special remote client.
	 * 
	 * @param hostAndPort Could be '*' for all remote clients.
	 * @return
	 * @since Sep 4, 2012
	 */
	public String resetClientInfo( String hostAndPort ) {
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "remoteClients" );
			Field mf = testAutomationService.getClass().getDeclaredField( "testMonitors" );
			f.setAccessible( true );
			mf.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<RemoteClient> clients = (Collection<RemoteClient>)f.get( testAutomationService );
			Iterator<RemoteClient> ic = clients.iterator();
			@SuppressWarnings( "unchecked" )
			Collection<TestMonitor> testMonitors = (Collection<TestMonitor>)mf.get( testAutomationService );
			if ( clients != null ) {
				if(hostAndPort.equals("*")) {
					if(testMonitors.isEmpty()) {
						clients.clear();
					}else {
						
						for ( RemoteClient rc : clients ) {
							for( TestMonitor tm : testMonitors ) {
								if ( !rc.getClientHostnameAndPort().equalsIgnoreCase( tm.getRemoteClient().getClientHostnameAndPort() ) ) {
									f = rc.getClass().getDeclaredField( "tests" );
									f.setAccessible( true );
									@SuppressWarnings( "unchecked" )
									Collection<Test> tests = (Collection<Test>)f.get( rc );
									tests.clear();
									return hostAndPort;
								}
							}
						}
					}
				}else {
					while ( ic.hasNext() ) {
						RemoteClient rc = ic.next();
						if ( rc.getClientHostnameAndPort().equalsIgnoreCase( hostAndPort ) ) {
							f = rc.getClass().getDeclaredField( "tests" );
							f.setAccessible( true );
							@SuppressWarnings( "unchecked" )
							Collection<Test> tests = (Collection<Test>)f.get( rc );
							tests.clear();
							return hostAndPort;
						}
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Remove assigned target registration info from tas.
	 * 
	 * @param imei
	 * @return
	 * @since Sep 4, 2012
	 */
	public String removeTarget( String imei ) {
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "testNodes" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<TestNode> nodes = (Collection<TestNode>)f.get( testAutomationService );
			if( nodes != null )
				for ( TestNode node : nodes ) {
					Iterator<Product> it = node.getProducts().iterator();
					while ( it.hasNext() ) {
						Product p = it.next();
						if ( p.getIMEI().equals( imei ) ) {
							it.remove();
							return "OK";
						}
					}
				}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Release assigned product from running invalid task.
	 * 
	 * @param imei
	 * @return
	 * @since Sep 4, 2012
	 */
	public String resetTarget( String imei ) {
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "testNodes" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<TestNode> nodes = (Collection<TestNode>)f.get( testAutomationService );
			if( nodes != null )
				for ( TestNode node : nodes ) {
					Iterator<Product> it = node.getProducts().iterator();
					while ( it.hasNext() ) {
						Product p = it.next();
						if ( p.getIMEI().equals( imei ) ) {
							node.freeProduct( p );
							return "OK";
						}
					}
				}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String stopTest( String testId ) {
		try {
			Field f = testAutomationService.getClass().getDeclaredField( "testMonitors" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Collection<TestMonitor> monitors = (Collection<TestMonitor>)f.get( testAutomationService );
			for( TestMonitor tm : monitors ) {
				if(tm.getTest().getId().equals( testId )) {
					tm.stopTest( "Invalid Test, stop by user." );
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
