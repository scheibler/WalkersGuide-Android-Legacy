package org.walkersguide.exceptions;

public class DepartureListParsingException extends Exception{
    public DepartureListParsingException(String exc) {
        super(exc);
    }
    public String getMessage() {
        return super.getMessage();
    }
}
