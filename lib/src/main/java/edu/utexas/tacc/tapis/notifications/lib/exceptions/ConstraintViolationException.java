package edu.utexas.tacc.tapis.notifications.lib.exceptions;

public class ConstraintViolationException extends DAOException {

    public ConstraintViolationException(String message, Throwable err) {
        super(message, err);
    }
}
