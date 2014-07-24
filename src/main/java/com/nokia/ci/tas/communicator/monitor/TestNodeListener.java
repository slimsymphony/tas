package com.nokia.ci.tas.communicator.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.LogInfo;
import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.ProductAdapter;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAdapter;
import com.nokia.ci.tas.commons.TestNode;
import com.nokia.ci.tas.communicator.ProductExplorer;
import com.nokia.ci.tas.communicator.TestAutomationCommunicator;
import com.nokia.ci.tas.communicator.TestExecutor;

/**
 * Listener for test nodes, could gather test node's information. Including TestNodes information/Product status/
 * 
 * @author Frank Wang
 * @since Jul 25, 2012
 */
public class TestNodeListener {

	private static int MaxLogFileSize = 10 * 1024 * 1024; // Max transfer log File is 10M
	private TestAutomationCommunicator testAutomationCommunicator;

	public TestNodeListener( TestAutomationCommunicator testAutomationCommunicator ) {
		this.testAutomationCommunicator = testAutomationCommunicator;
	}

	/**
	 * Gather products status for current Test Node.
	 * 
	 * @param item
	 * @since Jul 31, 2012
	 */
	public void gatherProduct( ProductAdapter item ) {
		if ( item.getProducts() == null )
			item.setProducts( new ArrayList<Product>() );
		try {
			ProductExplorer p = testAutomationCommunicator.getProductExplorer();
			Field f = null;
			if ( p != null ) {
				f = p.getClass().getDeclaredField( "products" );
				f.setAccessible( true );
				@SuppressWarnings( "unchecked" )
				Map<String, Product> products = ( Map<String, Product> ) f.get( p );
				if ( products != null )
					item.getProducts().addAll( products.values() );
			}
		} catch ( Exception e ) {
			System.err.println( "Gather products info failed." + MonitorUtils.getStack( e ) );
		}
	}

	/**
	 * Gather log files on current Test node.
	 * 
	 * @param item
	 * @since Jul 31, 2012
	 */
	public void gatherLog( LogInfo item ) {
		File root = new File( new File( "" ).getAbsolutePath() );
		File[] logs = root.listFiles( new FilenameFilter() {
			public boolean accept( File dir, String name ) {
				if ( name != null && name.toLowerCase().endsWith( ".log" ) )
					return true;
				return false;
			}
		} );

		FileInputStream fin = null;
		byte[] data = null;
		byte[] compressedData = null;
		if ( logs != null ) {
			for ( File logFile : logs ) {
				if ( logFile != null && logFile.exists() && logFile.isFile() && ( logFile.length() < MaxLogFileSize ) ) {
					if ( logFile.length() == 0 )
						continue;
					data = new byte[( int ) logFile.length()];
					try {
						fin = new FileInputStream( logFile );
						fin.read( data );
						compressedData = MonitorUtils.compress( data );
						if ( item.getLogs() == null )
							item.setLogs( new HashMap<String, byte[]>() );
						item.getLogs().put( logFile.getName(), compressedData );
					} catch ( Exception e ) {
						System.err.println( "LogFile " + logFile.getName() + "could not be read and compressed." );
					} finally {
						MonitorUtils.close( fin );
					}
				} else {
					System.err.println( "LogFile " + logFile.getName() + "could not be transferred." );
				}
			}
		}
	}

	/**
	 * Parser for parse input test node's information to create objects to describe these information.
	 * 
	 * @author Frank Wang
	 * @since Jul 31, 2012
	 */
	public static class TestNodeAnalyzer {

		/**
		 * Parse common input information.
		 * 
		 * @param input
		 * @return
		 * @since Jul 31, 2012
		 */
		public static Map<String, String> forCommon( String input ) {
			Map<String, String> map = new HashMap<String, String>();
			BufferedReader br = new BufferedReader( new StringReader( input ) );
			String line = null;
			try {
				while ( ( line = br.readLine() ) != null ) {
					if ( line.trim().equals( "" ) )
						continue;
					else {
						String[] entry = line.split( "=" );
						if ( entry.length == 2 && ( entry[0] != null && entry[1] != null ) && ( !entry[0].trim().equals( "" ) && !entry[1].trim().equals( "" ) ) )
							map.put( entry[0], entry[1] );
					}
				}
			} catch ( IOException e ) {
				System.err.println( "TestNodeAnalyzer parse for Common input failed, input=" + input + MonitorUtils.getStack( e ) );
			}
			return map;
		}

		/**
		 * Parse network related information.
		 * 
		 * @param input
		 * @return
		 * @since Jul 31, 2012
		 */
		public static List<String> parseNet( String input ) {
			List<String> nets = new ArrayList<String>();
			BufferedReader br = new BufferedReader( new StringReader( input ) );
			String line = null;
			try {
				while ( ( line = br.readLine() ) != null ) {
					if ( line.trim().equals( "" ) || !line.startsWith( " " ) || line.trim().startsWith( "Proto" ) ) {
						continue;
					} else {
						nets.add( line.trim() );
					}
				}
			} catch ( IOException e ) {
				System.err.println( "TestNodeAnalyzer parse for network input failed, input=" + input + MonitorUtils.getStack( e ) );
			}
			return nets;
		}

		/**
		 * Parse process related information.
		 * 
		 * @param input
		 * @return
		 * @since Jul 31, 2012
		 */
		public static Map<Integer, String> parseProcess( String input ) {
			Map<Integer, String> map = new HashMap<Integer, String>();
			BufferedReader br = new BufferedReader( new StringReader( input ) );
			String line = null;
			try {
				while ( ( line = br.readLine() ) != null ) {
					if ( line.trim().equals( "" ) ) {
						continue;
					} else {
						String next = br.readLine();
						if ( null != next && next.trim().equals( "" ) )
							next = br.readLine();
						if ( next != null ) {
							String[] entry = line.split( "=" );
							String[] entry2 = next.split( "=" );
							if ( entry.length == 2 && ( entry[0] != null && entry[1] != null ) && ( !entry[0].trim().equals( "" ) && !entry[1].trim().equals( "" ) )
									&& entry2.length == 2 && ( entry2[0] != null && entry2[1] != null ) && ( !entry2[0].trim().equals( "" ) && !entry2[1].trim().equals( "" ) ) )
								map.put( Integer.parseInt( entry2[1] ), entry[1] );
						}
					}
				}
			} catch ( Exception e ) {
				System.err.println( "TestNodeAnalyzer parse for Processes failed, input=" + input + MonitorUtils.getStack( e ) );
			}
			return map;
		}
	}

	/**
	 * Gather test node status information.
	 * 
	 * @param item
	 * @since Jul 31, 2012
	 */
	public void gatherTestNode( TestNode item ) {
		item.setCpu( TestNodeAnalyzer.forCommon( MonitorUtils.executeCmd( "wmic cpu get name,loadpercentage /value" ) ) );
		item.setDisk( TestNodeAnalyzer.forCommon( MonitorUtils.executeCmd( "wmic LOGICALDISK get size,freespace /value" ) ) );
		Map<String, String> t1 = TestNodeAnalyzer.forCommon( MonitorUtils.executeCmd( "wmic OS get FreePhysicalMemory /value" ) );
		Map<String, String> t2 = TestNodeAnalyzer.forCommon( MonitorUtils.executeCmd( "wmic ComputerSystem get TotalPhysicalMemory /value" ) );
		for ( String key : t2.keySet() ) {
			t1.put( key, t2.get( key ) );
		}
		item.setMemory( t1 );
		item.setNetwork( TestNodeAnalyzer.parseNet( MonitorUtils.executeCmd( "netstat -an" ) ) );
		item.setOs( TestNodeAnalyzer.forCommon( MonitorUtils.executeCmd( "wmic OS get Caption,CSDVersion,Version,CSName,Status /value" ) ) );
		item.setProcesses( TestNodeAnalyzer.parseProcess( MonitorUtils.executeCmd( "wmic process get processid,commandline /value" ) ) );
	}

	/**
	 * Gather Test related information.
	 * 
	 * @param item
	 * @since Jul 31, 2012
	 */
	public void gatherTests( TestAdapter item ) {
		if ( item.getTests() == null )
			item.setTests( new HashMap<Test, String>() );
		else
			item.getTests().clear();
		// no waiting on testnode.
		item.setWaitingRequests( 0 );
		ConcurrentLinkedQueue<TestExecutor> executors = testAutomationCommunicator.getTestExecutors();
		for ( TestExecutor executor : executors ) {
			String[] progress = getTestProgress( executor );
			try {
				Field f = executor.getClass().getDeclaredField( "currentOperation" );
				f.setAccessible( true );
				Object op = f.get( executor );
				String currOp = "NONE";
				if( op != null )
					currOp = op.toString();
				if ( "EXECUTE_TEST".equals( currOp ) ) {
					item.getTests().put( executor.getTest(), currOp + "#[" + progress[0] + "]"+"$"+progress[1] );
				} else {
					item.getTests().put( executor.getTest(), currOp );
				}
			} catch ( Exception e ) {

			}

		}
	}

	private String[] getTestProgress( TestExecutor executor ) {
		String currentCase = "Not Started";
		String progress = "Unknown";
		try {
			Test test = executor.getTest();
			File ws = executor.getTestWorkspace();
			if ( !ws.exists() )
				return new String[] {progress,currentCase};
			File graniteFolder = new File( ws, "granite" );
			if ( !graniteFolder.exists() )
				return new String[] {progress,currentCase};
			File scriptFile = null;
			File testsetFile = null;
			for ( String art : test.getArtifacts() ) {
				if ( art.indexOf( "execute.py" ) >= 0 ) {
					scriptFile = new File( ws, art );
					break;
				}
			}
			String tsFileName = null;
			if ( scriptFile != null && scriptFile.exists() ) {
				BufferedReader br = null;
				try {
					br = new BufferedReader( new FileReader( scriptFile ) );
					String line = null;
					while ( ( line = br.readLine() ) != null ) {
						if ( line.indexOf( "--test_set" ) > 0 ) {
							for ( String sub : line.split( " " ) ) {
								if ( sub.indexOf( ".testset" ) > 0 ) {
									tsFileName = sub.substring( sub.indexOf( "test_sets/" ) + 10, sub.lastIndexOf( "\"" ) );
									testsetFile = new File( graniteFolder, "test_sets/" + tsFileName );
									break;
								}
							}
							if ( testsetFile != null )
								break;
						}
					}
				} catch ( Exception e ) {
					return new String[] {progress,currentCase};
				} finally {
					MonitorUtils.close( br );
				}
			}
	
			if ( testsetFile != null && testsetFile.exists() ) {
				List<String> cases = parseTestset( testsetFile );
				File glog = new File( graniteFolder, "framework/granite.log" );
				if ( glog.exists() ) {
					BufferedReader fr = null;
					try {
						fr = new BufferedReader( new FileReader( glog ) );
						String line = null;
						while ( ( line = fr.readLine() ) != null ) {
							try {
								if ( line.indexOf( "Start test case:" ) > 0 ) {
									currentCase = line.substring( line.indexOf( "Start test case:" ) + 16 ).trim();
								}
							} catch ( Exception e2 ) {
								System.err.println( "parse granite log met problem >>> " + line );
							}
						}
					} catch ( Exception e ) {
						System.err.println( "Find granite log failed." + MonitorUtils.getStack( e ) );
					} finally {
						MonitorUtils.close( fr );
					}
					if ( currentCase != null ) {
						progress = calculateProgress( currentCase, cases );
					}
				}
			}
		}catch(Exception ex) {
			System.err.println("Get test progress failed." + MonitorUtils.getStack( ex ) );
		}
		return new String[] {progress, currentCase};
	}

	private String calculateProgress( String currentCase, List<String> cases ) {
		int idx = cases.indexOf( currentCase );
		return idx+"/"+cases.size();
	}

	private List<String> parseTestset( File testsetFile ) {
		List<String> cases = new ArrayList<String>();
		BufferedReader fr = null;
		try {
			fr = new BufferedReader( new FileReader( testsetFile ) );
			String line = null;
			while ( ( line = fr.readLine() ) != null ) {
				if ( line.indexOf( "<testcase" ) >= 0 ) {
					String[] arr = line.split( "\"" );
					for( int i=0; i<arr.length; i++ ) {
						if( arr[i]!=null && arr[i].toLowerCase().endsWith( "name=" ) ) {
							cases.add(arr[i+1].trim());
							break;
						}
					}
				}
			}
		} catch ( Exception ex ) {
			System.err.println( "Parse testset file failed." + MonitorUtils.getStack( ex ) );
		} finally {
			MonitorUtils.close( fr );
		}
		return cases;
	}
	
	public boolean stopTest( String testId ) {
		TestExecutor te = testAutomationCommunicator.getTestExecutor( testId );
		if( te != null ) {
			try {
				te.stopWorking( "Test Stop by User with monitor tool." );
				testAutomationCommunicator.getTestExecutors().remove( te );
				testAutomationCommunicator.stopTest(te.getTest(),true);
				te = null;
				System.out.println("Stop test["+testId+"] by monitor tool succ.");
				return true;
			}catch(Exception e) {
				System.err.println( "Stop test with monitor tool failed.test:" + testId );
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean freeProduct( String imei ) {
		try {
			ProductExplorer pe = testAutomationCommunicator.getProductExplorer();
			pe.setProductFree(imei);
			System.out.println("Free product["+imei+"] by monitor tool succ.");
			return true;
		}catch(Exception e) {
			System.err.println("Free product with monitor tool failed.Imei:"+imei);
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean removeProduct( String imei ) {
		try {
			ProductExplorer pe = testAutomationCommunicator.getProductExplorer();
			Field f = ProductExplorer.class.getDeclaredField( "products" );
			f.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			Map<String, Product> products = (Map<String, Product>)f.get( pe );
			Product p = products.get( imei );
			if( p != null ) {
				Method method = ProductExplorer.class.getDeclaredMethod( "removeProduct", Product.class );
				method.setAccessible( true );
				method.invoke( pe, p );
				System.out.println("Free product["+imei+"] by monitor tool succ.");
				return true;
			}
		}catch(Exception e) {
			System.err.println("Free product with monitor tool failed.Imei:"+imei);
			e.printStackTrace();
		}
		return false;
	}
}
