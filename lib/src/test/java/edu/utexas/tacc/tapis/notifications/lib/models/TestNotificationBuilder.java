package edu.utexas.tacc.tapis.notifications.lib.models;


import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.validation.ValidationException;

@Test
public class TestNotificationBuilder {

    @Test
    void testValidation() {
        Notification.Builder builder = new Notification.Builder()
            .setData("test");

        Assert.assertThrows(ValidationException.class, builder::build);
    }

    @Test
    void testShouldValidate() {
        Notification notification = new Notification.Builder()
            .setData("hello")
            .setSubject("TEST_EVENT")
            .setTenantId("testTenant")
            .build();
        Assert.assertNotNull(notification.getTime());
    }

}
