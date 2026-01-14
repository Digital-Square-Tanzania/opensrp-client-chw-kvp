package org.smartregister.presenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.smartregister.chw.kvp.contract.KvpRegisterFragmentContract;
import org.smartregister.chw.kvp.presenter.BaseKvpRegisterFragmentPresenter;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;
import org.smartregister.configurableviews.model.RegisterConfiguration;
import org.smartregister.configurableviews.model.View;

import java.util.Set;
import java.util.TreeSet;

public class BaseKvpRegisterFragmentPresenterKvp {
    @Mock
    protected KvpRegisterFragmentContract.View view;

    @Mock
    protected KvpRegisterFragmentContract.Model model;

    private BaseKvpRegisterFragmentPresenter baseKvpRegisterFragmentPresenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(model.defaultRegisterConfiguration()).thenReturn(new RegisterConfiguration());

        baseKvpRegisterFragmentPresenter = new BaseKvpRegisterFragmentPresenter(view, model, "");
    }

    @Test
    public void assertNotNull() {
        Assert.assertNotNull(baseKvpRegisterFragmentPresenter);
    }

    @Test
    public void getMainCondition() {
        Assert.assertEquals(Constants.TABLES.KVP_PrEP_REGISTER + "." + DBConstants.KEY.IS_CLOSED + " IS 0",
                baseKvpRegisterFragmentPresenter.getMainCondition());
    }

    @Test
    public void getDueFilterCondition() {
        Assert.assertEquals(" (cast( julianday(STRFTIME('%Y-%m-%d', datetime('now'))) -  julianday(IFNULL(SUBSTR(kvp_test_date,7,4)|| '-' || SUBSTR(kvp_test_date,4,2) || '-' || SUBSTR(kvp_test_date,1,2),'')) as integer) between 7 and 14) ", baseKvpRegisterFragmentPresenter.getDueFilterCondition());
    }

    @Test
    public void getDefaultSortQuery() {
        Assert.assertEquals(Constants.TABLES.KVP_PrEP_REGISTER + "." + DBConstants.KEY.LAST_INTERACTED_WITH + " DESC ", baseKvpRegisterFragmentPresenter.getDefaultSortQuery());
    }

    @Test
    public void getMainTable() {
        Assert.assertEquals(Constants.TABLES.KVP_PrEP_REGISTER, baseKvpRegisterFragmentPresenter.getMainTable());
    }

    @Test
    public void initializeQueries() {
        Set<View> visibleColumns = new TreeSet<>();
        Mockito.when(model.countSelect(Mockito.anyString(), Mockito.anyString())).thenReturn("countSelect");
        Mockito.when(model.mainSelect(Mockito.anyString(), Mockito.anyString())).thenReturn("mainSelect");

        baseKvpRegisterFragmentPresenter.initializeQueries(null);

        Mockito.verify(view).initializeQueryParams(Constants.TABLES.KVP_PrEP_REGISTER, "countSelect", "mainSelect");
        ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(view).initializeAdapter(captor.capture());
        Assert.assertEquals(visibleColumns, captor.getValue());
        Mockito.verify(view).countExecute();
        Mockito.verify(view).filterandSortInInitializeQueries();
    }

}
