package edu.utexas.tacc.tapis.notifications.exceptions;

public class InvalidInputException extends DAOException {
    public InvalidInputException(String message, Throwable err) {
        super(message, err);
    }
}
