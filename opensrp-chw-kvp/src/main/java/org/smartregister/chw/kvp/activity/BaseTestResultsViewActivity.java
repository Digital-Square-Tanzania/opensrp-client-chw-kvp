package org.smartregister.chw.kvp.activity;

import androidx.fragment.app.FragmentTransaction;

import org.json.JSONObject;
import org.smartregister.chw.kvp.fragment.BaseTestResultsFragment;
import org.smartregister.kvp.R;
import org.smartregister.view.activity.SecuredActivity;

public class BaseTestResultsViewActivity extends SecuredActivity {

    @Override
    protected void onCreation() {
        setContentView(R.layout.base_test_results_view_activity);
        loadFragment();
    }


    @Override
    protected void onResumption() {
        //overridden
    }

    public BaseTestResultsFragment getBaseFragment() {
        return new BaseTestResultsFragment();
    }

    private void loadFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.fragment_placeholder, getBaseFragment());
        ft.commit();
    }

    public void startFormActivity(JSONObject jsonObject) {
        //implement
    }


}
