package edu.utexas.tacc.tapis.notifications.lib.models;


import org.testng.Assert;
import org.testng.annotations.Test;

import javax.validation.ValidationException;

@Test
public class TestNotificationMechanism {

    @Test
    public void testBuilderBadEmail() {

        NotificationMechanism.Builder builder = new NotificationMechanism.Builder()
            .setMechanism(NotificationMechanismEnum.EMAIL)
            .setEmailAddress("badddddd");

        Assert.assertThrows(ValidationException.class, builder::build);
    }

    @Test
    public void testBuilderGoodEmail() {

        NotificationMechanism mech = new NotificationMechanism.Builder()
            .setMechanism(NotificationMechanismEnum.EMAIL)
            .setEmailAddress("test@test")
            .build();

        Assert.assertNotNull(mech);
        Assert.assertEquals(mech.getEmailAddress(), "test@test");
    }

    @Test
    public void testBuildNoWebhookURL() {
        NotificationMechanism.Builder builder = new NotificationMechanism.Builder()
            .setMechanism(NotificationMechanismEnum.WEBHOOK)
            .setEmailAddress("badddddd");

        Assert.assertThrows(ValidationException.class, builder::build);
    }

    @Test
    public void testBuildBadURL() {
        NotificationMechanism.Builder builder = new NotificationMechanism.Builder()
            .setMechanism(NotificationMechanismEnum.WEBHOOK)
            .setWebhookURL("notAURL");

        Assert.assertThrows(ValidationException.class, builder::build);
    }

    @Test
    public void testBuildGoodURL() {
        NotificationMechanism mech = new NotificationMechanism.Builder()
            .setMechanism(NotificationMechanismEnum.WEBHOOK)
            .setWebhookURL("http://goodURL.edu")
            .build();

        Assert.assertNotNull(mech);
        Assert.assertEquals(mech.getWebhookURL(), "http://goodURL.com");

    }

}
