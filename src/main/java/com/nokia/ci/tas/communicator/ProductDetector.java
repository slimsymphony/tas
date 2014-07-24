package com.nokia.ci.tas.communicator;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.SimCard;
import com.nokia.ci.tas.commons.Util;

import static com.nokia.ci.tas.commons.Util.isWindows;
import static com.nokia.ci.tas.commons.Util.exec;

public class ProductDetector extends Thread {

	final static String WORKDIR = "ADB_HOME";
	private ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<String, Product>();
	private ConcurrentHashMap<String, Product> backup;

	/**
	 * Handler of all discovered products.
	 */
	private ProductExplorer productExplorer;

	public static boolean debugFlag = false;

	public ProductDetector( ProductExplorer productExplorer ) {
		this.productExplorer = productExplorer;
	}

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception {
		ProductDetector p = new ProductDetector( null );
		p.start();
		Thread.sleep( 15000 );
		p.waitingSchedule = 1500l;
		Thread.sleep( 15000 );
		p.running = false;
	}

	public boolean running = true;

	public long waitingSchedule = 15000l;

	@Override
	public void run() {
		System.out.println( "ProductDetector Start!" );
		while ( running ) {
			try {
				refresh();
				Thread.sleep( waitingSchedule );
			} catch ( Exception e ) {
				System.err.println( "ProductDetector met exception when refreshing devices status. Ex:"
						+ e.getMessage() );
				e.printStackTrace();
			}
		}
		System.out.println( "ProductDetector Stop!" );
	}

	public Map<String, Product> listProducts() {
		return products;
	}
	
	public void refresh() throws Exception {
		if ( backup != null )
			backup.clear();
		backup = new ConcurrentHashMap<String, Product>( products );
		products.clear();
		if( isWindows() )
			parseProducts( exec( "%ADB_HOME%/adb devices", null) );
		else
			parseProducts( exec( "$ADB_HOME/adb devices", null) );
	}

	private void parseProducts( String str ) {
		BufferedReader br = new BufferedReader( new StringReader( str ) );
		String line = null;
		String sn = null;
		try {
			line = br.readLine();
			while ( ( line = br.readLine() ) != null ) {
				try {
					line = line.trim();
					if ( line.indexOf( "device" ) > 0 && (line.charAt( 8 ) == '\t' || line.charAt( 7 ) == '\t')) {
						sn = line.substring( 0, 8 ).trim();
						Product p = getProductInfo( sn );
						if ( backup.containsKey( p.getIMEI() ) ) {
							// update
							productExplorer.updateProduct( p );
						} else {
							// append
							productExplorer.addProduct( p );
						}
						products.put( p.getIMEI(), p );
						if( debugFlag )
							System.out.println( "Append product:" + sn + "\n" + p.toJSON() );
					}
				} catch ( Exception e ) {
					System.err.println( "Parse product sn failed. line=" + line );
					e.printStackTrace();
				}
			}
			for ( String key : backup.keySet() ) {
				if ( !products.containsKey( key ) ) {
					// remove
					productExplorer.removeProduct( backup.get( key ) );
				}
			}
		} catch ( Exception ex ) {
			System.err.println( "Parse products Info met error.ex:" + ex.getMessage() );
			ex.printStackTrace();
		}
	}

	private Map<String, String> parseProps( String str ) {
		Map<String, String> props = new HashMap<String, String>();
		BufferedReader br = new BufferedReader( new StringReader( str ) );
		String line = null;
		try {
			line = br.readLine();
			while ( ( line = br.readLine() ) != null ) {
				try {
					line = line.trim();
					if ( line.indexOf( "[" ) < 0 )
						continue;
					if ( line.indexOf( "[" ) == line.lastIndexOf( "[" )
							|| line.indexOf( "]" ) == line.lastIndexOf( "]" ) )
						continue;
					String key = line.substring( line.indexOf( "[" ) + 1, line.indexOf( "]" ) );
					String value = line.substring( line.lastIndexOf( "[" ) + 1, line.lastIndexOf( "]" ) );
					props.put( key, value );
				} catch ( Exception e ) {
					System.err.println( "Parse product props failed. line=" + line );
					e.printStackTrace();
				}
			}
		} catch ( Exception ex ) {
			System.err.println( "Parse product props met error.ex:" + ex.getMessage() );
			ex.printStackTrace();
		}
		return props;
	}

	private String adb() {
		if( isWindows() )
			return "%ADB_HOME%\\adb";
		else
			return "$ADB_HOME/adb";
	}
	
	private Product getProductInfo( String sn ) throws Exception {
		Product p = new Product();
		p.setSn( sn );

		String propstr = exec( adb()+" -s "+ sn +" shell getprop" , null );
		Map<String, String> props = parseProps( propstr );

		String imei = exec( adb() + " -s " + sn + " shell dumpsys iphonesubinfo|grep 'Device ID'" , null );
		try {
			imei = imei.split( "=" )[1].trim();
		} catch ( Exception e ) {
			System.err.println( "Parse Imei failed. Imei=" + imei );
		}
		p.setIMEI( imei );
		String rmcode = props.get( "ro.product.product" );
		if( rmcode == null || rmcode.isEmpty() ) {
			rmcode = props.get( "ro.product.rmcode" );
		}
		p.setRMCode( rmcode );
		p.setHardwareType( props.get( "ro.product.hw.id" ) );
		p.setSwVer( props.get( "apps.setting.product.swversion" ) );
		p.setFingerprint( props.get( "ro.build.fingerprint" ) );
		p.setProductCode( props.get( "ro.ril.product.code" ) );
		boolean sim1ok = false;
		boolean sim2ok = false;
		if ( props.get( "gsm.sim.state" ) != null && !props.get( "gsm.sim.state" ).equals( "ABSENT" ) ) {
			sim1ok = true;
			SimCard sim1 = new SimCard( SimCard.XML_ELEMENT_SIM_CARD_1 );
			p.setSim1( sim1 );
			sim1.setOperator( props.get( "gsm.sim.operator.alpha" ) );
			sim1.setOperatorCode( props.get( "gsm.sim.operator.numeric" ) );
			sim1.setOperatorCountry( props.get( "gsm.sim.operator.iso-country" ) );
		}

		if ( props.get( "gsm.sim2.state" ) != null && !props.get( "gsm.sim2.state" ).equals( "ABSENT" ) ) {
			sim2ok = true;
			SimCard sim2 = new SimCard( SimCard.XML_ELEMENT_SIM_CARD_2 );
			p.setSim2( sim2 );
			sim2.setOperator( props.get( "gsm.sim2.operator.alpha" ) );
			sim2.setOperatorCode( props.get( "gsm.sim2.operator.numeric" ) );
			sim2.setOperatorCountry( props.get( "gsm.sim2.operator.iso-country" ) );
		}

		if ( sim1ok || sim2ok ) {
			String signalStrength = exec( adb() + " -s " + sn + " shell dumpsys telephony.msim.registry|grep mSignalStrength", null );
			BufferedReader br = new BufferedReader( new StringReader( signalStrength.trim() ) );
			String line = null;
			int index = 0;
			while ( ( line = br.readLine() ) != null ) {
				line = line.trim();
				if ( line.indexOf( "mSignalStrength" ) >= 0 ) {
					line = line.substring( line.indexOf( ":" ) + 1 ).trim();
					line = line.substring( 0, line.indexOf( " " ) );
					try {
						int aus = Integer.parseInt( line );
						int dBm = 0;
						if ( aus != 99 )
							dBm = aus * 2 - 113;
						line = String.valueOf( dBm ) + "dBm";
					} catch ( Exception e ) {
						line = line + "aus";
					}
					if ( sim1ok && index == 0 && p.getSim1() != null )
						p.getSim1().setSignal( line );
					else if ( sim2ok && index == 1 && p.getSim2() != null )
						p.getSim2().setSignal( line );
					index++;
				}
			}
		}

		return p;
	}
}
