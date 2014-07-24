package com.nokia.ci.tas;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {
		Socket so = new Socket();
		so.connect( new InetSocketAddress( "3CNL12096.NOE.Nokia.com", 33333) );
		InputStream in = so.getInputStream();
		byte[] data = new byte[1024];
		int cnt = 0;
		StringBuffer sb = new StringBuffer();
		while( (cnt = in.read( data ))!= -1 ) {
			sb.append(new String(data,0,cnt));
		}
		System.out.println("Server:"+sb.toString());
		in.close();
		so.close();
		if(sb.toString().equals( "Server connected" )) {
			ServerSocket ss = new ServerSocket( 27182 );
			try {
				while ( true ) {
					Socket s = ss.accept();
					System.out.println( "Client Recving:" + s.getInetAddress().getHostName() + ":" + s.getPort() );
					in = s.getInputStream();
					cnt = 0;
					sb = new StringBuffer();
					while( (cnt = in.read( data ))!= -1 ) {
						sb.append(new String(data,0,cnt));
					}
					in.close();
					s.close();
					System.out.println( "Send from Node:"+ sb.toString() );
					Thread.sleep( 5000 );
					break;	
				}
			} finally {
				ss.close();
			}
		}
	}

}
