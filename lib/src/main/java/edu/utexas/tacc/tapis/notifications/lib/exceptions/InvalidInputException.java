package edu.utexas.tacc.tapis.notifications.lib.exceptions;

public class InvalidInputException extends DAOException {
    public InvalidInputException(String message, Throwable err) {
        super(message, err);
    }
}
