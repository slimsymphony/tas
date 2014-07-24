package com.nokia.ci.tas;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {
		ServerSocket ss = new ServerSocket( 33333 );
		try {
			while ( true ) {
				Socket s = ss.accept();
				System.out.println( "Server Recving:" + s.getInetAddress().getHostName() + ":" + s.getPort() );
				OutputStream os = s.getOutputStream();
				os.write( "Server connected".getBytes() );
				os.close();
				Thread.sleep( 5000 );
				System.out.println("Call node contact client.");
				Node.send( s.getInetAddress().getHostName(), 27182 );
				s.close();
				System.out.println( "End of receive" );
				Thread.sleep( 5000 );
				break;
			}
		} finally {
			ss.close();
		}
	}

}
