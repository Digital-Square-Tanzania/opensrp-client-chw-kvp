package org.smartregister.chw.kvp.presenter;

import static org.apache.commons.lang3.StringUtils.trim;

import org.smartregister.chw.kvp.contract.TestResultsFragmentContract;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;
import org.smartregister.configurableviews.model.RegisterConfiguration;
import org.smartregister.configurableviews.model.View;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.TreeSet;

public class BaseTestResultsFragmentPresenter implements TestResultsFragmentContract.Presenter {
    protected WeakReference<TestResultsFragmentContract.View> viewReference;
    protected TestResultsFragmentContract.Model model;

    protected RegisterConfiguration config;
    protected Set<View> visibleColumns = new TreeSet<>();
    protected String viewConfigurationIdentifier;


    public BaseTestResultsFragmentPresenter(TestResultsFragmentContract.View view, TestResultsFragmentContract.Model model, String viewConfigurationIdentifier) {
        this.viewReference = new WeakReference<>(view);
        this.model = model;
        this.viewConfigurationIdentifier = viewConfigurationIdentifier;
        this.config = model.defaultRegisterConfiguration();
    }

    @Override
    public String getMainCondition() {
        return " (" + Constants.TABLES.KVP_HEPATITIS_TEST_RESULTS + "." + DBConstants.KEY.HEP_TEST_RESULTS + " IS NULL )";
    }

    @Override
    public String getDefaultSortQuery() {
        return "";
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.KVP_HEPATITIS_TEST_RESULTS;
    }

    @Override
    public String getDueFilterCondition() {
        return null;
    }


    @Override
    public void processViewConfigurations() {
        //implement
    }

    @Override
    public void initializeQueries(String mainCondition) {
        String tableName = getMainTable();
        mainCondition = trim(getMainCondition()).equals("") ? mainCondition : getMainCondition();
        String countSelect = model.countSelect(tableName, mainCondition);
        String mainSelect = model.mainSelect(tableName, mainCondition);

        if (getView() != null) {

            getView().initializeQueryParams(tableName, countSelect, mainSelect);
            getView().initializeAdapter(visibleColumns);

            getView().countExecute();
            getView().filterandSortInInitializeQueries();
        }
    }


    protected TestResultsFragmentContract.View getView() {
        if (viewReference != null)
            return viewReference.get();
        else
            return null;
    }

    @Override
    public void startSync() {
        //implement
    }

    @Override
    public void searchGlobally(String s) {
        //implement
    }
}
