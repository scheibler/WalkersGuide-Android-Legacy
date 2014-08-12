package org.walkersguide.exceptions;

public class RouteParsingException extends Exception{
    public RouteParsingException(String exc) {
        super(exc);
    }
    public String getMessage() {
        return super.getMessage();
    }
}
