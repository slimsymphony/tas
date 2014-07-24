package com.nokia.ci.tas.service;

import java.net.Socket;

/**
 * Representation of a remote HTTP request issued to the Test Automation Service.
 */
public class HttpRequest {

    /**
     * A socket connection to HTTP requester.
     */
    private Socket connection;

    /**
     * Issued HTTP request.
     */
    private String request = "/";

    /**
     * Time of HTTP request object creation.
     */
    private long creationTime = 0L;

    /**
     * Default constructor.
     * 
     * @param connection Socket connection to HTTP requester.
     * @param request An issued HTTP request
     */
    public HttpRequest(Socket connection, String request) {
        this.connection = connection;
        this.request = request;
        creationTime = System.currentTimeMillis();
    }

    /**
     * Returns socket connection to the HTTP requester.
     * 
     * @return Socket connection to the HTTP requester
     */
    public Socket getConnection() {
        return connection;
    }

    /**
     * Issued HTTP request.
     * 
     * @return Issued HTTP request
     */
    public String getRequest() {
        return request;
    }

    /**
     * Time of HTTP request object creation.
     * 
     * @return Time of HTTP request object creation
     */
    public long getCreationTime() {
        return creationTime;
    }
}
