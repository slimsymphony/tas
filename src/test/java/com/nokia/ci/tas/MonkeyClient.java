package com.nokia.ci.tas;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.nokia.ci.tas.client.TestAutomationClient;
import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;

public class MonkeyClient extends Thread implements TestAutomationServiceListener {

	private TestAutomationClient client;
    private boolean isRunning = false;
    private java.util.HashMap<String, Test> executingTests;
    
    public MonkeyClient() {
    	isRunning = true;
        executingTests = new java.util.HashMap<String, Test>(0);
    }
	/**
	 * @param args
	 */
	public static void main( String[] args ) {
		String hostname = null;

        try {
            hostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();//getCanonicalHostName();
            System.out.println("RETURNED hostname is " + hostname);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (hostname == null || hostname.isEmpty()) {
            hostname = (String) System.getenv("HOST");
            System.out.println("HOST for hostname is " + hostname);
        }

        // Start demo
        try {
        	MonkeyClient example = new MonkeyClient();
            example.start();
        }
        catch (Exception e) {
            System.out.println("Got troubles while tried to run TAS example: " + e.getClass());
            e.printStackTrace();
        }
	}
	
	@Override
    public void run() {
        try {
            client = new TestAutomationClient(System.out);
            p("Monkry Running Client created on " + client.getHostname() + ":" + client.getPort());

            Random random = new Random();

            Test.ProductReleasingMode productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;

            long currenTime = System.currentTimeMillis();

            for (int i = 0; i < 1; i++) {
                Test.Target target = Test.Target.FLASH;

                productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;
                Test test = new Test("UI Monkey test" + i, productReleasingMode, target);

                test.setWorkspacePath("C:\\develop\\eclipse-workspaces\\nokia_workspace\\s40-ci-tas\\client_workspace\\aol");
                //test.setWorkspacePath("/home/frank/tas_client/workarea/");
                test.setTimeout(2 * Constant.ONE_HOUR);
                test.setProductDisconnectionTimeout(Constant.FIVE_MINUTES);
                test.setExecutorApplication( "" );
                test.setExecutorScript("execute.bat");
                //test.setExecutorScript("execute.sh");
                test.addArtifact("execute.sh");
                Map<String,String> env = new HashMap<String,String>(){
                	{
                		this.put( "HELLO", "Jacky" );
                	}
                };
                test.setExecutorEnvparams( env );
                test.setRequiredEnvironment( "(role:Main;)" );
                /*test.addArtifact("granite.zip");
                test.addArtifact("FuseHandler.dll");
                test.addArtifact("FuseLib.dll");*/

                test.setResultsFilename("results.zip");

//                for (int p = 1; p <= 2; p++) {
//                    TestPackage testPackage = new TestPackage("tests_" + p);
//                    testPackage.addFile("tests_" + p + ".zip");
//                    testPackage.addFile("tests_" + p + ".xml");
//                    testPackage.setRequiredEnvironment("(rm-code:RM-902;)");
//
//                    test.addTestPackage(testPackage);
//                }

                executingTests.put(test.getId(), test);

                p("Starting test:\n" + test + "\n");

                // Start test on sanbox TAS
                //client.startTest(test, "oucitas01.europe.nokia.com", 33333, this);
                //client.startTest(test, "10.233.6.184", 33333, this);
                client.startTest(test, "127.0.0.1", 33333, this);
            }

            while (isRunning) {
                sleep(Constant.DECISECOND);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void testStarted(Test startedTest) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST STARTED: " + startedTest + "\n\n\n\n\n\n\n\n");

        if (executingTests.containsKey(startedTest.getId())) {
            executingTests.put(startedTest.getId(), startedTest);
        }
    }

    @Override
    public void testFinished(Test finishedTest) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST FINISHED: test ID = " + finishedTest.getId() + "\n\n\n\n\n\n\n\n");

        Test localCopy = null;

        if (executingTests.containsKey(finishedTest.getId())) {
            localCopy = executingTests.get(finishedTest.getId());

            if (localCopy.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                p("\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> SETTING ALL RESERVED PRODUCTS FREE \n\n\n");
                client.freeProducts(localCopy.getReservedProducts());
            }

            executingTests.remove(finishedTest.getId());
        }

        // Put statistics into workspace directory - a simple text file, indicating what's ended and when
        try {
            File debugFile = new File(finishedTest.getWorkspacePath() + System.getProperty("file.separator") + finishedTest.getId() + "_FINISHED_at_" + System.currentTimeMillis());
            if (!debugFile.exists()) {
                if (debugFile.createNewFile()) {
                    // Write the reason of failure into this file
                    PrintWriter fileWriter = new PrintWriter(debugFile);
                    if (localCopy != null) {
                        fileWriter.append(localCopy.toString());
                    }
                    fileWriter.close();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (executingTests.isEmpty()) {
            isRunning = false;
        }
    }

    @Override
    public void testFailed(Test failedTest, String reason) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST FAILED: test ID = " + failedTest.getId() + ", Reason: " + reason + "\n\n\n\n\n\n\n\n");

        if (executingTests.containsKey(failedTest.getId())) {
            Test test = executingTests.get(failedTest.getId());

            if (test.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                p("\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> SETTING ALL RESERVED PRODUCTS FREE \n\n\n");
                client.freeProducts(test.getReservedProducts());
            }

            executingTests.remove(failedTest.getId());
        }

        // Put statistics into workspace directory - a simple text file, indicating what's ended and when
        try {
            File debugFile = new File(failedTest.getWorkspacePath() + System.getProperty("file.separator") + failedTest.getId() + "_FAILED_at_" + System.currentTimeMillis());
            if (!debugFile.exists()) {
                if (debugFile.createNewFile()) {
                    // Write the reason of failure into this file
                    PrintWriter fileWriter = new PrintWriter(debugFile);
                    fileWriter.append(reason);
                    fileWriter.close();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (executingTests.isEmpty()) {
            isRunning = false;
        }
    }

    @Override
    public void messageFromTestAutomationService(Test test, String message) {
        p("Message from Test Automation Service about test " + test.getRuntimeId() + ": " + message);
    }

    @Override
    public InputStream readFile(String directoryPath, String fileName) {
        p("readFile(" + directoryPath + ", " + fileName + ") is called");
        return null;
    }

    @Override
    public OutputStream createFile(String directoryPath, String fileName) {
        p("createFile(" + directoryPath + ", " + fileName + ") is called");
        return null;
    }

    public void p(String message) {
        System.out.println("TASListenerExampleImplementation: " + message);
    }
}
