package com.nokia.ci.tas;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Node {

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {
		
	}
	
	public static void send( String hostName, int port ) throws Exception {
		Socket s = new Socket();
		s.connect( new InetSocketAddress(hostName,port) );
		OutputStream os = s.getOutputStream();
		InputStream in = s.getInputStream();
		try {
		os.write( "I'll send you file.".getBytes() );
		os.close();
		s.close();
		}finally {
			try {os.close();}catch(Exception ex) {}
			try {in.close();}catch(Exception ex) {}
		}
	}

}
