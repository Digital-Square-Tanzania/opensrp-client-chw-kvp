package org.smartregister.chw.kvp.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.KvpProfileContract;
import org.smartregister.chw.kvp.custom_views.BaseKvpFloatingMenu;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.interactor.BaseKvpProfileInteractor;
import org.smartregister.chw.kvp.listener.OnClickFloatingMenu;
import org.smartregister.chw.kvp.presenter.BaseKvpProfilePresenter;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpUtil;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.util.PrEPVisitsUtil;
import org.smartregister.domain.AlertStatus;
import org.smartregister.helper.ImageRenderHelper;
import org.smartregister.kvp.R;
import org.smartregister.view.activity.BaseProfileActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import timber.log.Timber;


public class BaseKvpProfileActivity extends BaseProfileActivity implements KvpProfileContract.View, KvpProfileContract.InteractorCallBack {
    protected MemberObject memberObject;
    protected KvpProfileContract.Presenter profilePresenter;
    protected CircleImageView imageView;
    protected TextView textViewName;
    protected TextView textViewGender;
    protected TextView textViewLocation;
    protected TextView textViewUniqueID;
    protected TextView textViewRecordKvp;
    protected TextView textViewRecordAnc;
    protected TextView textview_register;
    protected RelativeLayout pendingPrEPRegistration;
    protected View view_last_visit_row;
    protected View view_most_due_overdue_row;
    protected View view_family_row;
    protected View viewSeparator1;
    protected RelativeLayout rlLastVisit;
    protected RelativeLayout rlUpcomingServices;
    protected RelativeLayout rlFamilyServicesDue;
    protected RelativeLayout visitStatus;
    protected ImageView imageViewCross;
    protected TextView textViewUndo;
    protected TextView textViewVisitDone;
    protected TextView textViewId;
    protected TextView textViewDominantKvpGroup;
    protected TextView textViewOtherKvpGroups;
    protected RelativeLayout visitDone;
    protected RelativeLayout visitInProgress;
    protected TextView textViewContinue;
    protected LinearLayout recordVisits;
    protected TextView textViewVisitDoneEdit;
    protected TextView textViewRecordAncNotDone;
    protected TextView manualProcessVisit;
    protected TextView prep_status;
    protected BaseKvpFloatingMenu baseKvpFloatingMenu;
    protected String profileType;
    private TextView tvUpComingServices;
    private TextView tvFamilyStatus;
    protected RelativeLayout rlTestResults;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private ProgressBar progressBar;

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, BaseKvpProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_kvp_profile);
        Toolbar toolbar = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        profileType = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
            upArrow.setColorFilter(getResources().getColor(R.color.text_blue), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }

        toolbar.setNavigationOnClickListener(v -> BaseKvpProfileActivity.this.finish());
        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(getToolbarTitle(profileType));
        appBarLayout = this.findViewById(R.id.collapsing_toolbar_appbarlayout);
        if (Build.VERSION.SDK_INT >= 21) {
            appBarLayout.setOutlineProvider(null);
        }

        textViewName = findViewById(R.id.textview_name);
        textViewGender = findViewById(R.id.textview_gender);
        textViewLocation = findViewById(R.id.textview_address);
        textViewUniqueID = findViewById(R.id.textview_id);
        view_last_visit_row = findViewById(R.id.view_last_visit_row);
        view_most_due_overdue_row = findViewById(R.id.view_most_due_overdue_row);
        view_family_row = findViewById(R.id.view_family_row);
        viewSeparator1 = findViewById(R.id.separator_1);
        imageViewCross = findViewById(R.id.tick_image);
        tvUpComingServices = findViewById(R.id.textview_name_due);
        tvFamilyStatus = findViewById(R.id.textview_family_has);
        rlLastVisit = findViewById(R.id.rlLastVisit);
        rlUpcomingServices = findViewById(R.id.rlUpcomingServices);
        rlFamilyServicesDue = findViewById(R.id.rlFamilyServicesDue);
        textViewVisitDone = findViewById(R.id.textview_visit_done);
        visitStatus = findViewById(R.id.record_visit_not_done_bar);
        visitDone = findViewById(R.id.visit_done_bar);
        recordVisits = findViewById(R.id.record_visits);
        progressBar = findViewById(R.id.progress_bar);
        textViewRecordAncNotDone = findViewById(R.id.textview_record_anc_not_done);
        textViewVisitDoneEdit = findViewById(R.id.textview_edit);
        textViewRecordKvp = findViewById(R.id.textview_record_kvp);
        textViewRecordAnc = findViewById(R.id.textview_record_anc);
        textViewUndo = findViewById(R.id.textview_undo);
        imageView = findViewById(R.id.imageview_profile);
        visitInProgress = findViewById(R.id.record_visit_in_progress);
        textViewContinue = findViewById(R.id.textview_continue);
        textview_register = findViewById(R.id.textview_register);
        pendingPrEPRegistration = findViewById(R.id.record_prep_registration);
        textViewId = findViewById(R.id.textview_uic_id);
        textViewDominantKvpGroup = findViewById(R.id.main_kvp_group);
        textViewOtherKvpGroups = findViewById(R.id.other_kvp_groups);
        manualProcessVisit = findViewById(R.id.textview_manual_process);
        prep_status = findViewById(R.id.prep_status);
        rlTestResults = findViewById(R.id.rlTestResults);

        textViewRecordAncNotDone.setOnClickListener(this);
        textViewVisitDoneEdit.setOnClickListener(this);
        rlLastVisit.setOnClickListener(this);
        rlUpcomingServices.setOnClickListener(this);
        rlFamilyServicesDue.setOnClickListener(this);
        textViewRecordKvp.setOnClickListener(this);
        textViewRecordAnc.setOnClickListener(this);
        textViewUndo.setOnClickListener(this);
        textViewContinue.setOnClickListener(this);
        textview_register.setOnClickListener(this);
        rlTestResults.setOnClickListener(this);

        imageRenderHelper = new ImageRenderHelper(this);
        if (StringUtils.isNotBlank(profileType) && profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PROFILE)) {
            memberObject = KvpDao.getKvpMember(baseEntityId);
        } else if (StringUtils.isNotBlank(profileType) && profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            memberObject = KvpDao.getPrEPMember(baseEntityId);
        } else {
            memberObject = KvpDao.getMember(baseEntityId);
        }
        initializePresenter();
        profilePresenter.fillProfileData(memberObject);
        setupViews();
        initializeFloatingMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        refreshMedicalHistory(true);
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        setupViews();
    }

    private int getToolbarTitle(String profileType) {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PROFILE)) {
            return R.string.return_to_kvp_clients;
        }
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            return R.string.return_to_prep_clients;
        }
        return R.string.return_to_kvp_prep_clients;
    }

    private int getServiceBtnText(String profileType) {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            return R.string.record_prep;
        }
        return R.string.record_kvp;
    }

    @Override
    protected void setupViews() {
        recordAnc(memberObject);
        recordPnc(memberObject);
        textViewRecordKvp.setText(getServiceBtnText(profileType));
        setupKvpProfile();
        setupPrEPProfile();
        setupButtons();
        hideButtonsOnClosed();
        showUICID(memberObject.getBaseEntityId());
        showKvpGroups(memberObject.getBaseEntityId());
        showInitiatedStatusForPrep(profileType, memberObject.getBaseEntityId());
    }

    protected void setupKvpProfile() {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PROFILE)) {
            try {
                KvpVisitsUtil.processVisits();
            } catch (Exception e) {
                Timber.e(e);
            }
            if (isPrEPRegistrationPending()) {
                pendingPrEPRegistration.setVisibility(View.VISIBLE);
            } else {
                pendingPrEPRegistration.setVisibility(View.GONE);
            }
        }
    }

    protected void setupPrEPProfile() {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            try {
                PrEPVisitsUtil.processVisits();
            } catch (Exception e) {
               Timber.e(e);
            }

            Visit lastPrepVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.PrEP_FOLLOWUP_VISIT);
            if (lastPrepVisit != null && !lastPrepVisit.getProcessed() && PrEPVisitsUtil.getPrEPVisitStatus(lastPrepVisit).equalsIgnoreCase(PrEPVisitsUtil.Complete)) {
                manualProcessVisit.setVisibility(View.VISIBLE);
                manualProcessVisit.setOnClickListener(view -> {
                    try {
                        PrEPVisitsUtil.manualProcessVisit(lastPrepVisit);
                        displayToast(R.string.prep_visit_conducted);
                        setupViews();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                });
            } else {
                manualProcessVisit.setVisibility(View.GONE);
            }

        }
    }

    protected void setupButtons() {
        if (isVisitOnProgress(profileType)) {
            textViewRecordKvp.setVisibility(View.GONE);
            visitInProgress.setVisibility(View.VISIBLE);
        } else {
            textViewRecordKvp.setVisibility(View.VISIBLE);
            visitInProgress.setVisibility(View.GONE);
        }
    }

    protected void hideButtonsOnClosed() {
        if (KvpDao.isClientClosed(memberObject.getBaseEntityId(), profileType)) {
            textViewRecordKvp.setVisibility(View.GONE);
            visitInProgress.setVisibility(View.GONE);
        }
    }

    protected void showUICID(String baseEntityId) {
        if (!profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            String tableName = profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PrEP_PROFILE) ? Constants.TABLES.KVP_PrEP_REGISTER : Constants.TABLES.KVP_REGISTER;
            String UIC_ID = KvpDao.getUIC_ID(baseEntityId, tableName);
            if (StringUtils.isNotBlank(UIC_ID)) {
                textViewId.setVisibility(View.VISIBLE);
                textViewId.setText(getString(R.string.uic_id, UIC_ID.toUpperCase(Locale.ROOT)));
            } else {
                textViewId.setVisibility(View.GONE);
            }
        } else {
            textViewId.setVisibility(View.GONE);
        }

    }

    protected void showKvpGroups(String baseEntityId) {
        if (!profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            String dominantKVPGroup = KvpDao.getDominantKVPGroup(baseEntityId);
            if (StringUtils.isNotBlank(dominantKVPGroup)) {
                textViewDominantKvpGroup.setVisibility(View.VISIBLE);
                List<String> dominantKvpGroup = new ArrayList<>(Arrays.asList(dominantKVPGroup));
                textViewDominantKvpGroup.setText(getString(R.string.dominant_kvp_group, readStringResourcesWithPrefix(dominantKvpGroup, "kvp_")));
            } else {
                textViewDominantKvpGroup.setVisibility(View.GONE);
            }

            List<String> otherKvpGroups = KvpDao.getOtherKvpGroups(baseEntityId);
            if (otherKvpGroups != null && !(otherKvpGroups.size() == 1 && otherKvpGroups.get(0).equalsIgnoreCase("none"))) {
                textViewOtherKvpGroups.setVisibility(View.VISIBLE);
                textViewOtherKvpGroups.setText(getString(R.string.other_kvp_groups, readStringResourcesWithPrefix(otherKvpGroups, "kvp_")));
            } else {
                textViewOtherKvpGroups.setVisibility(View.GONE);
            }

        } else {
            textViewDominantKvpGroup.setVisibility(View.GONE);
            textViewOtherKvpGroups.setVisibility(View.GONE);
        }

    }

    protected String readStringResourcesWithPrefix(List<String> keys, String prefix) {
        Resources resources = getResources();
        String packageName = getPackageName();
        List<String> resourceValues = new ArrayList<>();

        for (String key : keys) {
            String resourceKey = prefix + key;
            int resourceId = resources.getIdentifier(resourceKey, "string", packageName);

            if (resourceId != 0) {
                String resourceValue = resources.getString(resourceId);
                resourceValues.add(resourceValue);
            } else {
                System.out.println("Resource not found for key: " + resourceKey);
            }
        }

        return String.join(", ", resourceValues);
    }

    protected void showInitiatedStatusForPrep(String profileType, String baseEntityId) {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            if (KvpDao.isPrEPInitiated(baseEntityId)) {
                prep_status.setVisibility(View.VISIBLE);
            } else {
                prep_status.setVisibility(View.GONE);
            }
        }
    }

    protected boolean isPrEPRegistrationPending() {
        // boolean screeningEligible = KvpDao.isClientEligibleForPrEPFromScreening(memberObject.getBaseEntityId());
        boolean htsNegative = KvpDao.isClientHTSResultsNegative(memberObject.getBaseEntityId());
        boolean isPrEPMember = KvpDao.isRegisteredForPrEP(memberObject.getBaseEntityId());
        //if the client on screening was eligible for PrEP and
        //if the client on bio-medical services hts results was negative
        //and if the client does not exist on the PrEP register
        //and if the client age is greater than 15
        return htsNegative && !isPrEPMember && memberObject.getAge() >= 15;
    }

    @Override
    public void recordAnc(MemberObject memberObject) {
        //implement
    }

    @Override
    public void recordPnc(MemberObject memberObject) {
        //implement
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.title_layout) {
            onBackPressed();
        } else if (id == R.id.rlLastVisit) {
            this.openMedicalHistory();
        } else if (id == R.id.rlUpcomingServices) {
            this.openUpcomingService();
        } else if (id == R.id.rlFamilyServicesDue) {
            this.openFamilyDueServices();
        } else if (id == R.id.textview_record_kvp || id == R.id.textview_continue) {
            this.openFollowupVisit();
        } else if (id == R.id.textview_register) {
            this.startPrEPRegistration();
        }else if (id == R.id.rlTestResults) {
            this.openTestResults();
        }
    }

    public void openTestResults() {
        //implement
    }

    protected void startPrEPRegistration() {
        //Override
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        profilePresenter = new BaseKvpProfilePresenter(this, new BaseKvpProfileInteractor(), memberObject);
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
    }


    public void initializeFloatingMenu() {
        baseKvpFloatingMenu = new BaseKvpFloatingMenu(this, memberObject);
        checkPhoneNumberProvided(StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        if (showReferralView()) {
            baseKvpFloatingMenu.findViewById(R.id.refer_to_facility_layout).setVisibility(View.VISIBLE);
        } else {
            baseKvpFloatingMenu.findViewById(R.id.refer_to_facility_layout).setVisibility(View.GONE);
        }
        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            if (viewId == R.id.kvp_fab) {//Animates the actual FAB
                baseKvpFloatingMenu.animateFAB();
            } else if (viewId == R.id.call_layout) {
                baseKvpFloatingMenu.launchCallWidget();
                baseKvpFloatingMenu.animateFAB();
            } else if (viewId == R.id.refer_to_facility_layout) {
                startReferralForm();
            } else {
                Timber.d("Unknown FAB action");
            }
        };

        baseKvpFloatingMenu.setFloatMenuClickListener(onClickFloatingMenu);
        baseKvpFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseKvpFloatingMenu, linearLayoutParams);
    }

    private void checkPhoneNumberProvided(boolean hasPhoneNumber) {
        BaseKvpFloatingMenu.redrawWithOption(baseKvpFloatingMenu, hasPhoneNumber);
    }

    protected boolean showReferralView() {
        //in chw return true; in hf return false;
        return false;
    }

    public void startReferralForm() {
        //implement in chw
    }

    @Override
    public void hideView() {
        textViewRecordKvp.setVisibility(View.GONE);
    }

    @Override
    public void openFollowupVisit() {
        //Implement in application
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void setProfileViewWithData() {
        textViewName.setText(String.format("%s %s %s, %d", memberObject.getFirstName(),
                memberObject.getMiddleName(), memberObject.getLastName(), memberObject.getAge()));
        textViewGender.setText(KvpUtil.getGenderTranslated(this, memberObject.getGender()));
        textViewLocation.setText(memberObject.getAddress());
        textViewUniqueID.setText(memberObject.getUniqueId());


    }

    @Override
    public void setOverDueColor() {
        textViewRecordKvp.setBackground(getResources().getDrawable(R.drawable.record_btn_selector_overdue));
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        return null;
    }

    @Override
    protected void fetchProfileData() {
        //fetch profile data
    }

    @Override
    public void showProgressBar(boolean status) {
        progressBar.setVisibility(status ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        showProgressBar(false);
        rlLastVisit.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
    }

    @Override
    public void refreshUpComingServicesStatus(String service, AlertStatus status, Date date) {
        showProgressBar(false);
        if (status == AlertStatus.complete)
            return;
        view_most_due_overdue_row.setVisibility(View.VISIBLE);
        rlUpcomingServices.setVisibility(View.VISIBLE);

        if (status == AlertStatus.upcoming) {
            tvUpComingServices.setText(KvpUtil.fromHtml(getString(R.string.vaccine_service_upcoming, service, dateFormat.format(date))));
        } else {
            tvUpComingServices.setText(KvpUtil.fromHtml(getString(R.string.vaccine_service_due, service, dateFormat.format(date))));
        }
    }

    @Override
    public void refreshFamilyStatus(AlertStatus status) {
        showProgressBar(false);
        if (status == AlertStatus.complete) {
            setFamilyStatus(getString(R.string.family_has_nothing_due));
        } else if (status == AlertStatus.normal) {
            setFamilyStatus(getString(R.string.family_has_services_due));
        } else if (status == AlertStatus.urgent) {
            tvFamilyStatus.setText(KvpUtil.fromHtml(getString(R.string.family_has_service_overdue)));
        }
    }

    private void setFamilyStatus(String familyStatus) {
        view_family_row.setVisibility(View.VISIBLE);
        rlFamilyServicesDue.setVisibility(View.VISIBLE);
        tvFamilyStatus.setText(familyStatus);
    }

    @Override
    public void openMedicalHistory() {
        //implement
    }

    @Override
    public void openUpcomingService() {
        //implement
    }

    @Override
    public void openFamilyDueServices() {
        //implement
    }

    @Nullable
    private String formatTime(Date dateTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
            return formatter.format(dateTime);
        } catch (Exception e) {
            Timber.d(e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            profilePresenter.saveForm(data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON));
            finish();
        }
    }

    protected boolean isVisitOnProgress(String profileType) {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PROFILE)) {
            Visit bioMedicalVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
            Visit behavioralVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT);
            Visit structuralVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT);
            Visit otherServiceVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT);

            if (bioMedicalVisit != null && !bioMedicalVisit.getProcessed()) {
                return true;
            }
            if (behavioralVisit != null && !behavioralVisit.getProcessed()) {
                return true;
            }
            if (structuralVisit != null && !structuralVisit.getProcessed()) {
                return true;
            }
            return otherServiceVisit != null && !otherServiceVisit.getProcessed();
        }
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            Visit lastPrepVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.PrEP_FOLLOWUP_VISIT);
            return lastPrepVisit != null && !lastPrepVisit.getProcessed();
        }

        return false;
    }
}
