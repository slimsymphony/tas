package com.nokia.ci.tas;

import static com.nokia.ci.tas.commons.Util.exec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProcessTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main( String[] args ) throws IOException, InterruptedException {
		Map<String,String> env = new HashMap<String,String>();
		env.put( "ABC", "123" );
		env.put( "BCD", "2341" );
		env.put( "CDE", "345" );
		String output = exec( "echo %BCD%", (String)null, env );
		System.out.println(output);
	}

}
