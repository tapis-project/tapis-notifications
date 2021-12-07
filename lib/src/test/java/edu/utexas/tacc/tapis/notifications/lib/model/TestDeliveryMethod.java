package edu.utexas.tacc.tapis.notifications.lib.model;


import org.testng.Assert;
import org.testng.annotations.Test;

import javax.validation.ValidationException;

import edu.utexas.tacc.tapis.notifications.lib.model.DeliveryMethod.DeliveryMethodEnum;

@Test
public class TestDeliveryMethod
{
    @Test
    public void testBuilderBadEmail() {
        Assert.assertThrows(ValidationException.class, ()-> {
                   DeliveryMethod method = new DeliveryMethod(DeliveryMethodEnum.EMAIL, "badddddd");
        });
    }

    @Test
    public void testBuilderGoodEmail() {

        DeliveryMethod method = new DeliveryMethod(
            DeliveryMethodEnum.EMAIL,
            "test@test.com"
        );
        Assert.assertNotNull(method);
        Assert.assertEquals(method.getTarget(), "test@test.com");
    }

    @Test
    public void testBuildNoWebhookURL()
    {
        Assert.assertThrows(ValidationException.class, ()->{
            DeliveryMethod meth  = new DeliveryMethod(DeliveryMethodEnum.WEBHOOK, "baaaadurl");
        });
    }

    @Test
    public void testBuildGoodURL() {
        DeliveryMethod method = new DeliveryMethod(
            DeliveryMethodEnum.WEBHOOK,
            "http://goodURL.edu");

        Assert.assertNotNull(method);
        Assert.assertEquals(method.getTarget(), "http://goodURL.edu");
    }
}
