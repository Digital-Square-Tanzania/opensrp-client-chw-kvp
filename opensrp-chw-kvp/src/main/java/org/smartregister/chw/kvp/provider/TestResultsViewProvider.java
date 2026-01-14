package org.smartregister.chw.kvp.provider;

import static org.smartregister.chw.kvp.util.TimeUtils.getElapsedDays;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.fragment.BaseTestResultsFragment;
import org.smartregister.chw.kvp.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.chw.kvp.R;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class TestResultsViewProvider implements RecyclerViewProvider<TestResultsViewProvider.RegisterViewHolder> {

    private final LayoutInflater inflater;
    protected View.OnClickListener onClickListener;
    private View.OnClickListener paginationClickListener;
    private Context context;
    private Set<org.smartregister.configurableviews.model.View> visibleColumns;

    public TestResultsViewProvider(Context context, View.OnClickListener paginationClickListener, View.OnClickListener onClickListener, Set visibleColumns) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.paginationClickListener = paginationClickListener;
        this.onClickListener = onClickListener;
        this.visibleColumns = visibleColumns;
        this.context = context;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient smartRegisterClient, RegisterViewHolder registerViewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) smartRegisterClient;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(pc, registerViewHolder);
        }
    }


    @SuppressLint("SetTextI18n")
    private void populatePatientColumn(CommonPersonObjectClient pc, final RegisterViewHolder viewHolder) {
        try {

            String testType = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.HEPATITIS_TEST_TYPE, false);
            String testResult;

            testResult = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.HEP_TEST_RESULTS, false);


            String testResultDateString = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.TEST_DATE, false);
            testResultDateString = testResultDateString.replace("Z", "+0000");
            Date testResultDate = null;
            if (StringUtils.isNotBlank(testResultDateString)) {
                testResultDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).parse(testResultDateString);
            }

            if (StringUtils.isBlank(testResult)) {
                viewHolder.testResultsWrapper.setVisibility(View.GONE);
                viewHolder.dueWrapper.setVisibility(View.VISIBLE);
            } else {
                viewHolder.testResult.setText(testResult);
                viewHolder.testResultsWrapper.setVisibility(View.VISIBLE);
                viewHolder.dueWrapper.setVisibility(View.GONE);

                if (testResultDate != null && getElapsedDays(testResultDate) < 30) {
                    viewHolder.dueWrapper.setVisibility(View.VISIBLE);
                    viewHolder.recordTestResults.setText(R.string.edit);
                }
            }

            viewHolder.testDate.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(testResultDate));

            if (testType.contains("hep_b"))
                viewHolder.testType.setText("Hepatitis B");
            else if (testType.contains("hep_c"))
                viewHolder.testType.setText("Hepatitis C");
            else
                viewHolder.testType.setText(testType);

            viewHolder.recordTestResults.setTag(pc);
            viewHolder.recordTestResults.setTag(R.id.VIEW_ID, BaseTestResultsFragment.CLICK_VIEW_NORMAL);
            viewHolder.recordTestResults.setOnClickListener(onClickListener);

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void getFooterView(RecyclerView.ViewHolder viewHolder, int currentPageCount, int totalPageCount, boolean hasNext, boolean hasPrevious) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        footerViewHolder.pageInfoView.setText(MessageFormat.format(context.getString(org.smartregister.R.string.str_page_info), currentPageCount, totalPageCount));

        footerViewHolder.nextPageView.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);
        footerViewHolder.previousPageView.setVisibility(hasPrevious ? View.VISIBLE : View.INVISIBLE);

        footerViewHolder.nextPageView.setOnClickListener(paginationClickListener);
        footerViewHolder.previousPageView.setOnClickListener(paginationClickListener);
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption filterOption, ServiceModeOption serviceModeOption, FilterOption filterOption1, SortOption sortOption) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {
//        implement
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String s, String s1, String s2) {
        return null;
    }

    @Override
    public LayoutInflater inflater() {
        return inflater;
    }

    @Override
    public RegisterViewHolder createViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.test_result_list_row, parent, false);
        return new RegisterViewHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.smart_register_pagination, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        return viewHolder instanceof FooterViewHolder;
    }

    public class RegisterViewHolder extends RecyclerView.ViewHolder {
        public TextView testType;
        public TextView testResult;
        public TextView testDate;
        public RelativeLayout testResultsWrapper;
        public TextView resultTitle;
        public Button recordTestResults;
        public View dueWrapper;

        public RegisterViewHolder(View itemView) {
            super(itemView);

            testType = itemView.findViewById(R.id.test_type);
            testDate = itemView.findViewById(R.id.test_date);
            testResult = itemView.findViewById(R.id.result);
            testResultsWrapper = itemView.findViewById(R.id.rlTestResultsWrapper);
            recordTestResults = itemView.findViewById(R.id.record_test_results_button);
            dueWrapper = itemView.findViewById(R.id.due_button_wrapper);
            resultTitle = itemView.findViewById(R.id.test_result_title);
        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public TextView pageInfoView;
        public Button nextPageView;
        public Button previousPageView;

        public FooterViewHolder(View view) {
            super(view);

            nextPageView = view.findViewById(org.smartregister.R.id.btn_next_page);
            previousPageView = view.findViewById(org.smartregister.R.id.btn_previous_page);
            pageInfoView = view.findViewById(org.smartregister.R.id.txt_page_info);
        }
    }
}
