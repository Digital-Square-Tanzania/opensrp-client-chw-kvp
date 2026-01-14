package org.smartregister.activity;

import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.smartregister.chw.kvp.activity.BaseKvpRegisterActivity;
import org.smartregister.chw.kvp.contract.KvpRegisterContract;

public class BaseTestRegisterActivityKvp {
    @Mock
    public Intent data;
    private BaseKvpRegisterActivity baseKvpRegisterActivity;
    private KvpRegisterContract.Presenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        presenter = Mockito.mock(KvpRegisterContract.Presenter.class);
        baseKvpRegisterActivity = Mockito.mock(BaseKvpRegisterActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(presenter).when(baseKvpRegisterActivity).presenter();
    }

    @Test
    public void assertNotNull() {
        Assert.assertNotNull(baseKvpRegisterActivity);
    }

    @Test
    public void testFormConfig() {
        Assert.assertNull(baseKvpRegisterActivity.getFormConfig());
    }

    @Test
    public void checkIdentifier() {
        Assert.assertNotNull(baseKvpRegisterActivity.getViewIdentifiers());
    }

    @Test(expected = Exception.class)
    public void onActivityResult() throws Exception {
        Whitebox.invokeMethod(baseKvpRegisterActivity, "onActivityResult", 2244, -1, data);
        Mockito.verify(presenter).saveForm(null);
    }

}
