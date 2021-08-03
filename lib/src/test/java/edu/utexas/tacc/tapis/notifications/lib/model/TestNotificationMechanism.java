package edu.utexas.tacc.tapis.notifications.lib.model;


import org.testng.Assert;
import org.testng.annotations.Test;

import javax.validation.ValidationException;

@Test
public class TestNotificationMechanism {

    @Test
    public void testBuilderBadEmail() {
        Assert.assertThrows(ValidationException.class, ()-> {
                   NotificationMechanism mechanism = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "badddddd");
        });
    }

    @Test
    public void testBuilderGoodEmail() {

        NotificationMechanism mech = new NotificationMechanism(
            NotificationMechanismEnum.EMAIL,
            "test@test.com"
        );
        Assert.assertNotNull(mech);
        Assert.assertEquals(mech.getTarget(), "test@test.com");
    }

    @Test
    public void testBuildNoWebhookURL() {

        Assert.assertThrows(ValidationException.class, ()->{
            NotificationMechanism mech  = new NotificationMechanism(NotificationMechanismEnum.WEBHOOK, "baaaadurl");
        });
    }


    @Test
    public void testBuildGoodURL() {
        NotificationMechanism mech = new NotificationMechanism(
            NotificationMechanismEnum.WEBHOOK,
            "http://goodURL.edu");

        Assert.assertNotNull(mech);
        Assert.assertEquals(mech.getTarget(), "http://goodURL.edu");

    }

}
