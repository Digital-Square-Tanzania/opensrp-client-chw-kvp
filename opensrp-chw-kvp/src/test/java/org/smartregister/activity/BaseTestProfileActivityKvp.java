package org.smartregister.activity;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.smartregister.chw.kvp.activity.BaseKvpProfileActivity;
import org.smartregister.chw.kvp.contract.KvpProfileContract;
import org.smartregister.domain.AlertStatus;
import org.smartregister.chw.kvp.R;

import static org.mockito.Mockito.validateMockitoUsage;

public class BaseTestProfileActivityKvp {
    private BaseKvpProfileActivity baseKvpProfileActivity;

    @Mock
    public KvpProfileContract.Presenter profilePresenter;

    @Mock
    public View view;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class);
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }

    @Test
    public void assertNotNull() {
        Assert.assertNotNull(baseKvpProfileActivity);
    }

    @Test
    public void setOverDueColor() {
        baseKvpProfileActivity.setOverDueColor();
        Mockito.verify(view, Mockito.never()).setBackgroundColor(Color.RED);
    }

    @Test
    public void formatTime() {
        BaseKvpProfileActivity activity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        try {
            Assert.assertEquals("25 Oct 2019", Whitebox.invokeMethod(activity, "formatTime", "25-10-2019"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkHideView() {
        baseKvpProfileActivity.hideView();
        Mockito.verify(view, Mockito.never()).setVisibility(View.GONE);
    }

    @Test
    public void checkProgressBar() {
        baseKvpProfileActivity.showProgressBar(true);
        Mockito.verify(view, Mockito.never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void medicalHistoryRefresh() {
        baseKvpProfileActivity.refreshMedicalHistory(true);
        Mockito.verify(view, Mockito.never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void onClickBackPressed() {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(view.getId()).thenReturn(R.id.title_layout);
        Mockito.doNothing().when(baseKvpProfileActivity).onBackPressed();
        baseKvpProfileActivity.onClick(view);
        Mockito.verify(baseKvpProfileActivity).onBackPressed();
    }

    @Test
    public void onClickOpenMedicalHistory() {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(view.getId()).thenReturn(R.id.rlLastVisit);
        Mockito.doNothing().when(baseKvpProfileActivity).openMedicalHistory();
        baseKvpProfileActivity.onClick(view);
        Mockito.verify(baseKvpProfileActivity).openMedicalHistory();
    }

    @Test
    public void onClickOpenUpcomingServices() {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(view.getId()).thenReturn(R.id.rlUpcomingServices);
        Mockito.doNothing().when(baseKvpProfileActivity).openUpcomingService();
        baseKvpProfileActivity.onClick(view);
        Mockito.verify(baseKvpProfileActivity).openUpcomingService();
    }

    @Test
    public void onClickOpenFamlilyServicesDue() {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(view.getId()).thenReturn(R.id.rlFamilyServicesDue);
        Mockito.doNothing().when(baseKvpProfileActivity).openFamilyDueServices();
        baseKvpProfileActivity.onClick(view);
        Mockito.verify(baseKvpProfileActivity).openFamilyDueServices();
    }

    @Test(expected = Exception.class)
    public void refreshFamilyStatusComplete() throws Exception {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        TextView textView = view.findViewById(R.id.textview_family_has);
        Whitebox.setInternalState(baseKvpProfileActivity, "tvFamilyStatus", textView);
        Mockito.doNothing().when(baseKvpProfileActivity).showProgressBar(false);
        baseKvpProfileActivity.refreshFamilyStatus(AlertStatus.complete);
        Mockito.verify(baseKvpProfileActivity).showProgressBar(false);
        PowerMockito.verifyPrivate(baseKvpProfileActivity).invoke("setFamilyStatus", "Family has nothing due");
    }

    @Test(expected = Exception.class)
    public void refreshFamilyStatusNormal() throws Exception {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        TextView textView = view.findViewById(R.id.textview_family_has);
        Whitebox.setInternalState(baseKvpProfileActivity, "tvFamilyStatus", textView);
        Mockito.doNothing().when(baseKvpProfileActivity).showProgressBar(false);
        baseKvpProfileActivity.refreshFamilyStatus(AlertStatus.complete);
        Mockito.verify(baseKvpProfileActivity).showProgressBar(false);
        PowerMockito.verifyPrivate(baseKvpProfileActivity).invoke("setFamilyStatus", "Family has services due");
    }

    @Test(expected = Exception.class)
    public void onActivityResult() throws Exception {
        BaseKvpProfileActivity baseKvpProfileActivity = Mockito.mock(BaseKvpProfileActivity.class, Mockito.CALLS_REAL_METHODS);
        Whitebox.invokeMethod(baseKvpProfileActivity, "onActivityResult", 2244, -1, null);
        Mockito.verify(profilePresenter).saveForm(null);
    }

}
