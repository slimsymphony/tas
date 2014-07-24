package com.nokia.ci.tas.commons;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Contains various utility methods used in Test Automation Service components.
 */
public class Util {
    /**
     * Converts specified time in milliseconds into a nice string containing days, hours, minutes and seconds.
     *
     * @param milliseconds Time value to be converted
     * @return String containing days, hours, minutes and seconds from specified milliseconds value
     */
    public static String convert(long milliseconds) {

        String result = "";

        if (milliseconds < Constant.ONE_SECOND) {
            if (milliseconds > 1L) {
                result = milliseconds + " milliseconds";
            } else {
                result = milliseconds + " millisecond";
            }

            return result;
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long dSeconds = seconds % 60L;

        if (dSeconds > 0L) {
            if (dSeconds > 1L) {
                result = dSeconds + " seconds";
            } else {
                result = dSeconds + " second";
            }

            seconds -= dSeconds;
        }

        long minutes = seconds / 60L;
        long dMinutes = minutes % 60L;

        if (dMinutes > 0L) {
            if (dMinutes > 1L) {
                result = dMinutes + " minutes " + result;
            } else {
                result = dMinutes + " minute " + result;
            }

            minutes -= dMinutes;
        }

        long hours = minutes / 60L;
        long dHours = hours % 24L;

        if (dHours > 0L) {
            if (dHours > 1L) {
                result = dHours + " hours " + result;
            } else {
                result = dHours + " hour " + result;
            }

            hours -= dHours;
        }

        long days = hours / 24L;

        if (days > 0L) {
            if (days > 1L) {
                result = days + " days " + result;
            } else {
                result = days + " day " + result;
            }
        }

        return result.trim();
    }

    /**
     * Checks specified test id against any prohibited characters.
     * Returns null if test id is valid or failure message otherwise.
     *
     * @param id Test id to be checked
     * @return Null if test id is valid or failure message otherwise
     */
    public static String checkTestId(String id) {
        String failure = "Test id " + id + " is invalid";

        try {
            id = id.trim();

            if (id.isEmpty()) {
                failure = "Test id " + id + " contains only whitespaces, which is not allowed.";
            } else if (id.indexOf("/") != -1) {
                failure = "Test id " + id + " contains forward slash, which is not allowed.";
            } else if (id.indexOf("\\") != -1) {
                failure = "Test id " + id + " contains backward slash, which is not allowed.";
            } else if (id.indexOf(":") != -1) {
                failure = "Test id " + id + " contains colon, which is not allowed.";
            } else if (id.indexOf("\"") != -1) {
                failure = "Test id " + id + " contains quotation mark, which is not allowed.";
            } else if (id.indexOf("'") != -1) {
                failure = "Test id " + id + " contains apostrophe, which is not allowed.";
            } else if (id.indexOf("?") != -1) {
                failure = "Test id " + id + " contains question mark, which is not allowed.";
            } else if (id.indexOf("%") != -1) {
                failure = "Test id " + id + " contains percent mark, which is not allowed.";
            } else if (id.indexOf("*") != -1) {
                failure = "Test id " + id + " contains asterisk, which is not allowed.";
            } else if (id.indexOf("|") != -1) {
                failure = "Test id " + id + " contains vertical bar, which is not allowed.";
            } else if (id.indexOf("<") != -1) {
                failure = "Test id " + id + " contains angle brackets, which is not allowed.";
            } else if (id.indexOf(">") != -1) {
                failure = "Test id " + id + " contains angle brackets, which is not allowed.";
            } else if (id.indexOf("{") != -1) {
                failure = "Test id " + id + " contains braces, which is not allowed.";
            } else if (id.indexOf("}") != -1) {
                failure = "Test id " + id + " contains braces, which is not allowed.";
            } else if (id.indexOf("~") != -1) {
                failure = "Test id " + id + " contains tilde, which is not allowed.";
            } else if (id.indexOf("&") != -1) {
                failure = "Test id " + id + " contains ampersand, which is not allowed.";
            } else if (id.indexOf("+") != -1) {
                failure = "Test id " + id + " contains plus sign, which is not allowed.";
            } else if (id.indexOf(".") != -1) {
                failure = "Test id " + id + " contains dot mark, which is not allowed.";
            } else {
                // Nothing to complain about
                failure = null;
            }
        } catch (Exception e) {
            failure = "Test id " + id + " is invalid";
        }

        return failure;
    }

    /**
     * Splits a list of input name-value pairs into normalized name-value groups.
     * "Normalization" here means that specified names will be sorted
     * according to the list of names returned by the Product.getParameterNames() method.
     *
     * So, for example, if input string was "status:free;imei:012345678901234;",
     * the output result will be "(.)*imei:012345678901234;(.)*status:free;(.)*".
     *
     * Such string will be a proper regular expression for searches on data returned from the product's getAllNameValuePairs() method.
     *
     * The input string could also contain grouping of different products.
     * In such case each set of name-value pairs must start with the symbol Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_START_SYMBOL
     * and end with the symbol Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_END_SYMBOL.
     *
     * Note, that the list of nested groups is not allowed and will be interpreted as an input error.
     *
     * If the input set of name-value pairs is not bounded by grouping symbols, that expression will be treated as a single group.
     *
     * For example, expression "(imei:012345678901234;role:main;)(rm-code:RM-123;role:remote;)" will be treated as a request for two products:
     * for the first one with IMEI number "012345678901234" and being in "main" role,
     * and the second one with RM code "RM-123" and in "remote" role.
     *
     * At the same time expression "imei:012345678901234;role:main;" will be interpreted as a request for exactly one product
     * with IMEI number "012345678901234" being in "main" role.
     *
     * After successful processing the list of parsed and normalized name-value groups will be returned.
     * In case of any errors an empty list will be returned.
     *
     * @param nameValueGroups A list of name-value groups
     * @return A list of parsed and normalized name-value groups or empty list in case of any errors
     */
    public static List<String> getNormalizedNameValueGroups(String nameValueGroups) {
        List<String> result = new ArrayList<String>(0);

        try {
            if (nameValueGroups != null && !nameValueGroups.trim().isEmpty()) {
                // Find where are starts and ends of all groups
                List<Integer> groupStarts = new ArrayList<Integer>(0);
                List<Integer> groupEnds = new ArrayList<Integer>(0);
                int index = 0;

                do {
                    index = nameValueGroups.indexOf(Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_START_SYMBOL, index);
                    if (index != -1) {
                        groupStarts.add(index);
                        index += 1;
                    }
                } while (index != -1);

                index = 0;
                do {
                    index = nameValueGroups.indexOf(Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_END_SYMBOL, index);
                    if (index != -1) {
                        groupEnds.add(index);
                        index += 1;
                    }
                } while (index != -1);

                if (groupStarts.size() != groupEnds.size()) {
                    // A number of group starts is not equal to the number of group ends
                    return result;
                }

                List<String> nameValuePairsGroups = new ArrayList<String>(0);

                if (groupStarts.size() > 0) {
                    int numberOfGroups = groupStarts.size();

                    for (int i = 0; i < numberOfGroups; i++) {
                        try {
                            // Don't take grouping symbols into account anymore
                            String currentNameValueGroup = nameValueGroups.substring(groupStarts.get(i) + 1, groupEnds.get(i));

                            if (currentNameValueGroup != null && !currentNameValueGroup.trim().isEmpty()) {
                                nameValuePairsGroups.add(currentNameValueGroup.trim());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // Add the whole expression as a single group
                    nameValuePairsGroups.add(nameValueGroups.trim());
                }

                if (nameValuePairsGroups.size() > 0) {
                    // Ensure that each group contains an expression for product's free status and at least "main" role
                    String productFreeStatus = Product.XML_ELEMENT_STATUS + Constant.NAME_VALUE_SEPARATOR + Product.STATUS_FREE + Constant.NAME_VALUE_PAIR_SEPARATOR;
                    String productMainRole = Product.XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR + Product.ROLE_MAIN + Constant.NAME_VALUE_PAIR_SEPARATOR;

                    for (String nameValuePairsGroup : nameValuePairsGroups) {

                        // Ensure that free status is added into the group
                        if (!nameValuePairsGroup.contains(productFreeStatus)) {
                            nameValuePairsGroup += productFreeStatus;
                        }

                        // Ensure that at least "main" role is mentioned in the group
                        if (!nameValuePairsGroup.contains(Product.XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR)) {
                            nameValuePairsGroup += productMainRole;
                        }

                        // Normalize a group of name-value pairs
                        String normalizedNameValueGroup = "";

                        String[] tokenizedNameValueGroup = nameValuePairsGroup.split(Constant.NAME_VALUE_PAIR_SEPARATOR);

                        List<String> normalizedSetOfNameValuePairs = new ArrayList<String>(0);

                        // Replace all "dangerous" characters by their safe regular expression representatives
                        for (String nameValuePair : tokenizedNameValueGroup) {
                            String trimmedNameValuePair = nameValuePair.trim();

                            if (!trimmedNameValuePair.isEmpty()) {
                                // Plus sign could be in phone numbers, but in regular expressions it has a different meaning
                                trimmedNameValuePair = trimmedNameValuePair.replace("+", "\\+");
                                // Dot sign could be used anywhere, but in regular expressions it has a different meaning
                                trimmedNameValuePair = trimmedNameValuePair.replace(".", "\\.");
                                // Add a separator of name-value pairs since it was removed during tokenization
                                normalizedSetOfNameValuePairs.add(trimmedNameValuePair + Constant.NAME_VALUE_PAIR_SEPARATOR);
                            }
                        }

                        Product reference = new Product();
                        List<String> productParameterNames = reference.getParameterNames();

                        // Resort the list of name-value pairs according to the list of names returned by the product's getParameterNames() method
                        if (!normalizedSetOfNameValuePairs.isEmpty()) {
                            for (String parameterName : productParameterNames) {
                                for (String normalizedNameValuePair : normalizedSetOfNameValuePairs) {
                                    if (normalizedNameValuePair.startsWith(parameterName)) {
                                        normalizedNameValueGroup += Constant.REGULAR_EXPRESSION_FOR_ANY_CHARACTER_SEQUENCE + normalizedNameValuePair;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!normalizedNameValueGroup.isEmpty()) {
                            normalizedNameValueGroup += Constant.REGULAR_EXPRESSION_FOR_ANY_CHARACTER_SEQUENCE;
                            result.add(normalizedNameValueGroup);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Extracts environment specifications from the list of specified name-value groups
     * and creates a list of regular expressions based on them.
     * In case of any errors this methid will return an empty list.
     *
     * @param nameValueGroups A set of name-value groups representing environment specifications
     * @return A list of regular expressions describing environment requirements
     */
    public static List<String> createRegularExpressions(String nameValueGroups) {
        List<String> result = new ArrayList<String>(0);

        try {
            if (nameValueGroups != null && !nameValueGroups.trim().isEmpty()) {
                // Find where are starts and ends of all groups
                List<Integer> groupStarts = new ArrayList<Integer>(0);
                List<Integer> groupEnds = new ArrayList<Integer>(0);
                int index = 0;

                do {
                    index = nameValueGroups.indexOf(Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_START_SYMBOL, index);
                    if (index != -1) {
                        groupStarts.add(index);
                        index += 1;
                    }
                } while (index != -1);

                index = 0;
                do {
                    index = nameValueGroups.indexOf(Constant.TEST_RESOURCE_ALLOCATION_EXPRESSION_END_SYMBOL, index);
                    if (index != -1) {
                        groupEnds.add(index);
                        index += 1;
                    }
                } while (index != -1);

                if (groupStarts.size() != groupEnds.size()) {
                    // A number of group starts is not equal to the number of group ends
                    return result;
                }

                List<String> nameValuePairsGroups = new ArrayList<String>(0);

                if (groupStarts.size() > 0) {
                    int numberOfGroups = groupStarts.size();

                    for (int i = 0; i < numberOfGroups; i++) {
                        try {
                            // Don't take grouping symbols into account anymore
                            String currentNameValueGroup = nameValueGroups.substring(groupStarts.get(i) + 1, groupEnds.get(i));

                            if (currentNameValueGroup != null && !currentNameValueGroup.trim().isEmpty()) {
                                nameValuePairsGroups.add(currentNameValueGroup.trim());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // Add the whole expression as a single group
                    nameValuePairsGroups.add(nameValueGroups.trim());
                }

                if (nameValuePairsGroups.size() > 0) {
                    // Ensure that each group contains an expression for product's free status and at least "main" role
                    String productFreeStatus = Product.XML_ELEMENT_STATUS + Constant.NAME_VALUE_SEPARATOR + Product.STATUS_FREE + Constant.NAME_VALUE_PAIR_SEPARATOR;
                    String productMainRole = Product.XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR + Product.ROLE_MAIN + Constant.NAME_VALUE_PAIR_SEPARATOR;

                    for (String nameValuePairsGroup : nameValuePairsGroups) {

                        // Ensure that free status is added into the group
                        if (!nameValuePairsGroup.contains(productFreeStatus)) {
                            nameValuePairsGroup += productFreeStatus;
                        }

                        // Ensure that at least "main" role is mentioned in the group
                        if (!nameValuePairsGroup.contains(Product.XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR)) {
                            nameValuePairsGroup += productMainRole;
                        }

                        // Normalize a group of name-value pairs
                        String normalizedNameValueGroup = "";

                        String[] tokenizedNameValueGroup = nameValuePairsGroup.split(Constant.NAME_VALUE_PAIR_SEPARATOR);

                        List<String> normalizedSetOfNameValuePairs = new ArrayList<String>(0);

                        // Replace all "dangerous" characters by their safe regular expression representatives
                        for (String nameValuePair : tokenizedNameValueGroup) {
                            String trimmedNameValuePair = nameValuePair.trim();

                            if (!trimmedNameValuePair.isEmpty()) {
                                // Plus sign could be in phone numbers, but in regular expressions it has a different meaning
                                trimmedNameValuePair = trimmedNameValuePair.replace("+", "\\+");
                                // Dot sign could be used anywhere, but in regular expressions it has a different meaning
                                trimmedNameValuePair = trimmedNameValuePair.replace(".", "\\.");
                                // Add a separator of name-value pairs since it was removed during tokenization
                                normalizedSetOfNameValuePairs.add(trimmedNameValuePair + Constant.NAME_VALUE_PAIR_SEPARATOR);
                            }
                        }

                        Product reference = new Product();
                        List<String> productParameterNames = reference.getParameterNames();

                        // Resort the list of name-value pairs according to the list of names returned by the product's getParameterNames() method
                        if (!normalizedSetOfNameValuePairs.isEmpty()) {
                            for (String parameterName : productParameterNames) {
                                for (String normalizedNameValuePair : normalizedSetOfNameValuePairs) {
                                    if (normalizedNameValuePair.startsWith(parameterName)) {
                                        normalizedNameValueGroup += Constant.REGULAR_EXPRESSION_FOR_ANY_CHARACTER_SEQUENCE + normalizedNameValuePair;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!normalizedNameValueGroup.isEmpty()) {
                            normalizedNameValueGroup += Constant.REGULAR_EXPRESSION_FOR_ANY_CHARACTER_SEQUENCE;
                            result.add(normalizedNameValueGroup);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public static boolean debugFlag = false;
    
    public static String exec( String cmd, String workDir ) throws IOException, InterruptedException {
		
		ProcessBuilder pb = null;
		
		if( isWindows() ) {
			pb = new ProcessBuilder( "cmd","/C", cmd );
		}else {
			pb = new ProcessBuilder( "/bin/bash","-cl", cmd );
		}		
		
		if ( workDir != null ) {
	        File f = new File( workDir );
	        if ( f.exists() )
	                pb.directory( f );
		}
		
		pb.redirectErrorStream( true );
		Process p = null;
		int result = -1;
		InputStream in = null;
		String output = "";
		try {
			p = pb.start();
			in = p.getInputStream();
			ByteArrayOutputStream sos = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			int read = 0;
			while( ( read = in.read( data ) ) != -1 ) {
				sos.write( data, 0, read );
			}
			result = p.waitFor();
			output = sos.toString();
		} finally {
			try { in.close(); }catch(Exception ex) {}
		}
		if ( debugFlag )
			if ( result == 0 )
				System.out.println( "Exec [" + cmd + "] @" + workDir + " success!" );
			else
				System.out.println( "Exec [" + cmd + "] @" + workDir + " failed!" );
		return output;
	}
    
    public static String exec( String cmd, String workDir, Map<String,String> env ) throws IOException, InterruptedException {
		
		ProcessBuilder pb = null;
		
		if( isWindows() ) {
			if( env != null && !env.isEmpty() ) {
				StringBuffer sb = new StringBuffer();
				for( String key : env.keySet() ) {
					sb.append("set ").append( key ).append("=").append( env.get( key ) ).append( " & " );
				}
				sb.append( "call " );
				cmd = sb.toString() + cmd;
			}
			pb = new ProcessBuilder( "cmd","/C", cmd );
		}else {
			if( env != null && !env.isEmpty() ) {
				StringBuffer sb = new StringBuffer();
				for( String key : env.keySet() ) {
					sb.append( key ).append("=").append( env.get( key ) ).append( " && " );
				}
				cmd = sb.toString() + cmd;
			}
			pb = new ProcessBuilder( "/bin/bash","-cl", cmd );
		}		
		
		if ( workDir != null ) {
	        File f = new File( workDir );
	        if ( f.exists() )
	                pb.directory( f );
		}
		
		pb.redirectErrorStream( true );
		Process p = null;
		int result = -1;
		InputStream in = null;
		String output = "";
		try {
			p = pb.start();
			in = p.getInputStream();
			ByteArrayOutputStream sos = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			int read = 0;
			while( ( read = in.read( data ) ) != -1 ) {
				sos.write( data, 0, read );
			}
			result = p.waitFor();
			output = sos.toString();
		} finally {
			try { in.close(); }catch(Exception ex) {}
		}
		if ( debugFlag )
			if ( result == 0 )
				System.out.println( "Exec [" + cmd + "] @" + workDir + " success!" );
			else
				System.out.println( "Exec [" + cmd + "] @" + workDir + " failed!" );
		return output;
	}
    
    public static Process exec( String cmd, File workDir, Map<String,String> env ) throws IOException, InterruptedException {
		
		ProcessBuilder pb = null;
		
		if( isWindows() ) {
			if( env != null && !env.isEmpty() ) {
				StringBuffer sb = new StringBuffer();
				for( String key : env.keySet() ) {
					sb.append("set ").append( key ).append("=").append( env.get( key ) ).append( " & " );
				}
				sb.append( "call " );
				cmd = sb.toString() + cmd;
			}
			pb = new ProcessBuilder( "cmd","/C", cmd );
		}else {
			if( env != null && !env.isEmpty() ) {
				StringBuffer sb = new StringBuffer();
				for( String key : env.keySet() ) {
					sb.append( key ).append("=").append( env.get( key ) ).append( " && " );
				}
				cmd = sb.toString() + cmd;
			}
			pb = new ProcessBuilder( "/bin/bash","-cl", cmd );
		}		
		
		if ( workDir != null && workDir.exists() ) {
	        pb.directory( workDir );
		}
		
		pb.redirectErrorStream( true );
		return pb.start();
	}
    
    public static boolean isWindows() {
		String os = System.getProperty("os.name");
		if(os.toLowerCase().indexOf("win") >= 0) 
			return true;
		else
			return false;
	}
    
    public static String grep( String content, String key, boolean caseSensitive ) throws IOException {
    	if( content == null || content.isEmpty() || key==null || key.isEmpty() )
    		return content;
    	BufferedReader br = new BufferedReader(new StringReader(content));
    	String line = null;
    	StringBuffer sb = new StringBuffer(content.length());
    	while( (line = br.readLine()) != null ) {
    		if(caseSensitive)
    			if( line.toLowerCase().indexOf( key.toLowerCase() )>=0)
    				sb.append( line ).append( "\n" );
    		else
    			if( line.indexOf( key )>0 )
    				sb.append( line ).append( "\n" );
    	}
    	return sb.toString();
    }
    
    public static String grep(InputStream in, String key, boolean caseSensitive) throws IOException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	if(in !=null) {
    		byte[] data = new byte[1024];
    		int cnt = 0;
    		while( ( cnt = in.read(data) ) != -1 ) {
    			baos.write( data, 0, cnt );
    		}
    		return grep( baos.toString(), key, caseSensitive);
    	}
    	return null;
    }
    
    public static String getValidHostIp() {
		String IP = "127.0.0.1";
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
			if ( IP.equals( "127.0.0.1" ) && !InetAddress.getLocalHost().getHostAddress().startsWith( "169" ) )
				IP = InetAddress.getLocalHost().getHostAddress();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return IP;
	}
}
