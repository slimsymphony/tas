package com.nokia.ci.tas;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

public class IpTest {

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {
		System.out.println( InetAddress.getLoopbackAddress().getHostAddress() );
		System.out.println( InetAddress.getLocalHost().getHostAddress() );
		System.out.println( InetAddress.getLocalHost().getHostName() );
		System.out.println( InetAddress.getLocalHost().getCanonicalHostName() );
		System.out.println(getValidHostIp());
	}

	public static String getValidHostIp() {
		String IP = InetAddress.getLoopbackAddress().getHostAddress();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface networkInterface = null;
			while ( networkInterfaces.hasMoreElements() ) {
				networkInterface = networkInterfaces.nextElement();
				if ( networkInterface.isLoopback() )
					continue;
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while ( inetAddresses.hasMoreElements() ) {
					InetAddress inetAddress = inetAddresses.nextElement();
					if ( inetAddress.getHostAddress().startsWith( "10." )
							|| inetAddress.getHostAddress().startsWith( "172." ) ) {
						IP = inetAddress.getHostAddress();
						break;
					}
				}
			}
			if ( IP.equals( InetAddress.getLoopbackAddress().getHostAddress() ) )
				IP = InetAddress.getLocalHost().getHostAddress();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return IP;
	}

	public static Collection<InetAddress> getAllHostAddress() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			Collection<InetAddress> addresses = new ArrayList<InetAddress>();

			while ( networkInterfaces.hasMoreElements() ) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while ( inetAddresses.hasMoreElements() ) {
					InetAddress inetAddress = inetAddresses.nextElement();
					addresses.add( inetAddress );
				}
			}

			return addresses;
		} catch ( SocketException e ) {
			throw new RuntimeException( e.getMessage(), e );
		}
	}

	public static Collection<String> getAllNoLoopbackAddresses() {
		Collection<String> noLoopbackAddresses = new ArrayList<String>();
		Collection<InetAddress> allInetAddresses = getAllHostAddress();

		for ( InetAddress address : allInetAddresses ) {
			if ( !address.isLoopbackAddress() ) {
				noLoopbackAddresses.add( address.getHostAddress() );
			}
		}

		return noLoopbackAddresses;
	}

	public static String getFirstNoLoopbackAddress() {
		Collection<String> allNoLoopbackAddresses = getAllNoLoopbackAddresses();
		return allNoLoopbackAddresses.iterator().next();
	}

}
