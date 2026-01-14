package org.smartregister.fragment;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.smartregister.chw.kvp.activity.BaseKvpProfileActivity;
import org.smartregister.chw.kvp.fragment.BaseKvpRegisterFragment;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import static org.mockito.Mockito.times;

public class BaseTestRegisterFragmentKvp {
    @Mock
    public BaseKvpRegisterFragment baseKvpRegisterFragment;

    @Mock
    public CommonPersonObjectClient client;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = Exception.class)
    public void openProfile() throws Exception {
        Whitebox.invokeMethod(baseKvpRegisterFragment, "openProfile", client);
        PowerMockito.mockStatic(BaseKvpProfileActivity.class);
        BaseKvpProfileActivity.startProfileActivity(null, null);
        PowerMockito.verifyStatic(BaseKvpProfileActivity.class, times(1));
        BaseKvpProfileActivity.startProfileActivity(null, null);
    }
}
