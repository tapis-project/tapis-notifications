package edu.utexas.tacc.tapis.notifications.lib.models;


import edu.utexas.tacc.tapis.shared.notifications.Notification;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.validation.ValidationException;

@Test
public class TestNotificationBuilder {

    @Test
    void testValidation() {
        Notification.Builder builder = new Notification.Builder()
            .setBody("test");

        Assert.assertThrows(ValidationException.class, builder::build);
    }

    @Test
    void testShouldValidate() {
        Notification notification = new Notification.Builder()
            .setBody("hello")
            .setCreator("testService")
            .setEventType("TEST_EVENT")
            .setLevel("INFO")
            .setRecipient("testuser")
            .setTenant("testTenant")
            .build();
        Assert.assertNotNull(notification.getCreated());
        Assert.assertNull(notification.getNotificationMechanism());
    }


    @Test
    void testShouldValidateWithMechanism() {
        Notification notification = new Notification.Builder()
            .setBody("hello")
            .setCreator("testService")
            .setEventType("TEST_EVENT")
            .setLevel("INFO")
            .setRecipient("testuser")
            .setTenant("testTenant")
            .build();
        Assert.assertNotNull(notification.getCreated());
        Assert.assertEquals(notification.getNotificationMechanism().getEmailAddress(), "test@test");
    }


}
