package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.pojo.Notification;
import reactor.core.publisher.Flux;

public class Constants {

    public static final String exchangeFormat = "{tenant}.{serviceName}.{actionName}.{UUID}.{endUser}";
    public static final String EXCHANGE_NAME = "tapis.notifications";


}
