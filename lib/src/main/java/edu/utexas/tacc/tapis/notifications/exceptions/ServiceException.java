package edu.utexas.tacc.tapis.notifications.exceptions;

public class ServiceException extends Exception {
    public ServiceException(String message) {super(message);}
    public ServiceException(String message, Throwable err) {super(message, err);}
}
