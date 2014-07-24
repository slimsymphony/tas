package com.nokia.ci.tas.commons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Common utility methods collection.
 * 
 * @author Frank Wang
 * @since Jul 26, 2012
 */
public class MonitorUtils {
	
	public static String getStack( Throwable t ) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw) );
		return sw.toString();
	}
	
	/**
	 * Execute given command.
	 * 
	 * @param cmd Command to be executed
	 * @return output
	 * @since Aug 20, 2012
	 */
	public static String executeCmd( String cmd ) {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		PumpStreamHandler psh = new PumpStreamHandler( stdout );
		ExecuteWatchdog watchdog = new ExecuteWatchdog( 10000 );
		Executor exec = new DefaultExecutor();
		exec.setWatchdog( watchdog );
		exec.setStreamHandler( psh );
		CommandLine cl = CommandLine.parse( cmd );
		int exitvalue = -1;
		try {
			exitvalue = exec.execute( cl );
		} catch ( ExecuteException e ) {
			System.err.println( "Execute CMD [" + cmd + "] failed." + getStack(e) );
		} catch ( IOException e ) {
			System.err.println( "Execute CMD [" + cmd + "] failed." + getStack(e) );
		} finally {
			System.err.println( "Execute CMD [" + cmd + "], result:" + exitvalue );
		}
		return stdout.toString();
	}

	/**
	 * Execute given command and return without waiting for the result. 
	 * 
	 * @param cmd Command to be executed
	 * @return ExecuteResultHandler
	 * @since Aug 20, 2012
	 */
	public static ExecuteResultHandler executeCmdNonLock( String cmd ) {
		ExecuteWatchdog watchdog = new ExecuteWatchdog( 30000 );
		ExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		Executor exec = new DefaultExecutor();
		exec.setWatchdog( watchdog );
		CommandLine cl = CommandLine.parse( cmd );
		try {
			exec.execute( cl, resultHandler );
		} catch ( ExecuteException e ) {
			System.err.println( "Execute CMD [" + cmd + "] failed." + getStack(e) );
		} catch ( IOException e ) {
			System.err.println( "Execute CMD [" + cmd + "] failed." + getStack(e) );
		} finally {
			System.err.println( "Execute CMD [" + cmd + "]" );
		}
		return resultHandler;
	}

	/**
	 * Compress given String.
	 * 
	 * @param str String to be compressed
	 * @return compressed String
	 * @since Aug 20, 2012
	 */
	public static String compress( String str ) {
		try {
			byte[] input = str.getBytes("ISO-8859-1");
			return new String( compress( input ), "ISO-8859-1" );
		} catch ( Exception e ) {
			System.err.println( "Compress String failed." + getStack(e) );
			return str;
		}
	}
	
	/**
	 * Compress String to byte array. Encode will use ISO-8859-1.
	 * 
	 * @param str String to be compressed
	 * @return byte array
	 * @since Aug 21, 2012
	 */
	public static byte[] compressData( String str ) {
		try {
			byte[] input = str.getBytes("ISO-8859-1");
			return compress( input );
		} catch ( Exception e ) {
			System.err.println( "Compress String failed." + getStack(e) );
			return str.getBytes();
		}
	}

	/**
	 * Decompress encode String to original String  
	 * 
	 * @param str string to be decompress
	 * @return 
	 * @since Aug 21, 2012
	 */
	public static String decompress( String str ) {
		try {
			byte[] compressedData = str.getBytes("ISO-8859-1");
			return new String( decompress( compressedData ), "ISO-8859-1" ) ;
		} catch ( Exception e ) {
			System.err.println( "DeCompress String failed." + getStack(e) );
			return str;
		}
	}
	
	/**
	 * Decompress String to byte array.
	 * 
	 * @param str String to be decompressed
	 * @return byte array
	 * @since Aug 21, 2012
	 */
	public static byte[] decompressData( String str ) {
		try {
			byte[] compressedData = str.getBytes("ISO-8859-1");
			return decompress( compressedData );
		} catch ( Exception e ) {
			System.err.println( "DeCompress String failed." + getStack(e) );
			return str.getBytes();
		}
	}
	
	/**
	 * Compress byte array data.
	 * 
	 * @param input date to be compressed.
	 * @return compressed byte array
	 * @since Aug 21, 2012
	 */
	public static byte[] compress( byte[] input ) {
		try {
			return CompressUtils.compress( input, null );
		} catch ( Exception e ) {
			System.err.println( "Compress Data failed." + getStack(e) );
			return input;
		}
	}

	/**
	 * Decompress byte array data.
	 * 
	 * @param compressedData
	 * @return
	 * @since Aug 21, 2012
	 */
	public static byte[] decompress( byte[] compressedData ) {
		try {
			return CompressUtils.decompress( compressedData, null );
		} catch ( Exception e ) {
			System.err.println( "DeCompress Data failed." + getStack(e) );
			return compressedData;
		}
	}

	/**
	 * Close socket quietly.
	 * 
	 * @param socket
	 * @since Aug 21, 2012
	 */
	public static void close( Socket socket ) {
		if ( socket == null )
			return;
		try {
			socket.close();
		} catch ( IOException e ) {
			System.err.println( "Close Socket failed." + getStack(e) );
		}
	}

	/**
	 * Close server socket quietly.
	 * 
	 * @param socket
	 * @since Aug 21, 2012
	 */
	public static void close( ServerSocket socket ) {
		if ( socket == null )
			return;
		try {
			socket.close();
		} catch ( IOException e ) {
			System.err.println( "Close ServerSocket failed." + getStack(e) );
		}
	}
	
	/**
	 * Close input stream quietly.
	 * 
	 * @param in
	 * @since Aug 21, 2012
	 */
	public static void close( InputStream in ) {
		if ( in == null )
			return;
		try {
			in.close();
		} catch ( IOException e ) {
			System.err.println( "Close InputStream failed." + getStack(e) );
		}
	}
	
	/**
	 * Close reader quietly.
	 * 
	 * @param in
	 * @since Aug 21, 2012
	 */
	public static void close( Reader in ) {
		if ( in == null )
			return;
		try {
			in.close();
		} catch ( IOException e ) {
			System.err.println( "Close Reader failed." + getStack(e) );
		}
	}

	/**
	 * Close output Stream quietly.
	 * 
	 * @param os
	 * @since Aug 21, 2012
	 */
	public static void close( OutputStream os ) {
		if ( os == null )
			return;
		try {
			os.close();
		} catch ( IOException e ) {
			System.err.println( "Close OutputStream failed." + getStack(e) );
		}
	}
	
	/**
	 * Close writer quietly.
	 * 
	 * @param os
	 * @since Aug 21, 2012
	 */
	public static void close( Writer os ) {
		if ( os == null )
			return;
		try {
			os.close();
		} catch ( IOException e ) {
			System.err.println( "Close Writer failed." + getStack(e) );
		}
	}

	/**
	 * Gson base for json conversion.
	 */
	final private static Gson gson = new GsonBuilder().registerTypeAdapter( Response.class, new Response.ResponseTypeAdpater() ).registerTypeAdapter( Response.class, new Response.ResponseTypeAdpater()).create();
	
	/**
	 * Transform object to json.
	 * 
	 * @param obj
	 * @return
	 * @since Aug 21, 2012
	 */
	public static String toJson( Object obj ) {
		return gson.toJson( obj );
	}

	/**
	 * Transform json String to Object.
	 * 
	 * @param str json string
	 * @param clazz Type of Object to be converted
	 * @return
	 * @since Aug 21, 2012
	 */
	public static <T> T fromJson( String str, Class<T> clazz ) {
		return gson.fromJson( str, clazz );
	}

	/**
	 * Transform json String to provided Type.
	 * 
	 * @param jsonStr json string
	 * @param type Type of Object to be converted
	 * @return
	 * @since Aug 21, 2012
	 */
	public static <T> T fromJson( String jsonStr, Type type ) {
		return gson.fromJson( jsonStr, type );
	}
}
