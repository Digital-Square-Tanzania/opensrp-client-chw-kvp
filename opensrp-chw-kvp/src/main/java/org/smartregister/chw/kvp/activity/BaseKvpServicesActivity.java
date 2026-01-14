package org.smartregister.chw.kvp.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.adapter.BaseServiceCardAdapter;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.ServiceCard;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.handlers.BaseServiceActionHandler;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.R;
import org.smartregister.view.activity.SecuredActivity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BaseKvpServicesActivity extends SecuredActivity {
    protected BaseServiceCardAdapter serviceCardAdapter;
    protected TextView tvTitle;
    protected MemberObject memberObject;
    protected String baseEntityId;

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, BaseKvpServicesActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kvp_services);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        }
        memberObject = KvpDao.getKvpMember(baseEntityId);
        setupViews();
        initializeMainServiceContainers();

    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeMainServiceContainers();
    }

    protected void setupViews() {
        initializeRecyclerView();
        View cancelButton = findViewById(R.id.undo_button);
        cancelButton.setOnClickListener(v -> finish());
        tvTitle = findViewById(R.id.top_patient_name);
        tvTitle.setText(MessageFormat.format("{0}, {1}", memberObject.getFullName(), memberObject.getAge()));
    }

    protected void initializeRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        serviceCardAdapter = new BaseServiceCardAdapter(this, new ArrayList<>(), getServiceHandler(), baseEntityId);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(serviceCardAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    protected void initializeMainServiceContainers() {
        List<ServiceCard> serviceCards = new ArrayList<>();

        ServiceCard bioMedicalService = new ServiceCard();
        bioMedicalService.setServiceName(getString(R.string.bio_medical_services));
        bioMedicalService.setId(Constants.SERVICES.KVP_BIO_MEDICAL);
        bioMedicalService.setServiceStatus(computeServiceStatus(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT));
        bioMedicalService.setServiceIcon(R.drawable.ic_bio_medical);
        bioMedicalService.setBackground(R.drawable.purple_bg);
        bioMedicalService.setVisitsCount(getVisitCount(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT));
        bioMedicalService.setEventServiceName(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
        serviceCards.add(bioMedicalService);

        ServiceCard behavioralService = new ServiceCard();
        behavioralService.setServiceName(getString(R.string.behavioral_services));
        behavioralService.setId(Constants.SERVICES.KVP_BEHAVIORAL);
        behavioralService.setServiceStatus(computeServiceStatus(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT));
        behavioralService.setServiceIcon(R.drawable.ic_behavioral);
        behavioralService.setBackground(R.drawable.orange_bg);
        behavioralService.setVisitsCount(getVisitCount(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT));
        behavioralService.setEventServiceName(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT);
        serviceCards.add(behavioralService);

        ServiceCard structuralService = new ServiceCard();
        structuralService.setServiceName(getString(R.string.structural_services));
        structuralService.setId(Constants.SERVICES.KVP_STRUCTURAL);
        structuralService.setServiceStatus(computeServiceStatus(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT));
        structuralService.setServiceIcon(R.drawable.ic_structural);
        structuralService.setBackground(R.drawable.dark_blue_bg);
        structuralService.setVisitsCount(getVisitCount(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT));
        structuralService.setEventServiceName(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT);
        serviceCards.add(structuralService);

        ServiceCard otherService = new ServiceCard();
        otherService.setServiceName(getString(R.string.other_services));
        otherService.setId(Constants.SERVICES.KVP_OTHERS);
        otherService.setServiceStatus(computeServiceStatus(Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT));
        otherService.setServiceIcon(R.drawable.ic_others);
        otherService.setBackground(R.drawable.ocean_blue_bg);
        otherService.setVisitsCount(getVisitCount(Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT));
        otherService.setEventServiceName(Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT);
        serviceCards.add(otherService);

        serviceCardAdapter.setServiceCards(serviceCards);
        serviceCardAdapter.notifyDataSetChanged();
    }

    //TODO: cleanup -> move files to follow pattern
    private String computeServiceStatus(String visitType) {
        Visit lastVisit = getLatestVisit(visitType);
        if (lastVisit != null && !lastVisit.getProcessed()) {
            String visitStatus = "";
            if (visitType.equals(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT)) {
                visitStatus = KvpVisitsUtil.getBioMedicalStatus(lastVisit);
            }
            if (visitType.equals(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT)) {
                visitStatus = KvpVisitsUtil.getBehavioralServiceStatus(lastVisit);
            }
            if (visitType.equals(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT)) {
                visitStatus = KvpVisitsUtil.getStructuralServiceStatus(lastVisit);
            }
            if (visitType.equals(Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT)) {
                visitStatus = KvpVisitsUtil.getOtherServiceStatus(lastVisit);
            }
            if (StringUtils.isNotBlank(visitStatus)) {
                int id = getResources().getIdentifier("service_status_" + visitStatus, "string", getPackageName());
                if (id == 0) {
                    return visitStatus;
                }
                return getString(id);
            }
            return getString(R.string.service_status_pending);
        }
        if (lastVisit != null && lastVisit.getProcessed()) {
            DateTime time = new DateTime(lastVisit.getUpdatedAt());
            DateTime now = new DateTime();
            int diffMin = Minutes.minutesBetween(time, now).getMinutes();
            if (diffMin < 30) {
                return getString(R.string.service_status_processed);
            }
        }
        return getString(R.string.service_status_pending);
    }

    public Visit getLatestVisit(String visitType) {
        return KvpLibrary.getInstance().visitRepository().getLatestVisit(baseEntityId, visitType);
    }

    public int getVisitCount(String visitType) {
        List<Visit> processedVisits = KvpLibrary.getInstance().visitRepository().getAllVisitsProcessed(visitType, baseEntityId);
        return processedVisits.size();
    }

    public BaseServiceActionHandler getServiceHandler() {
        return new BaseServiceActionHandler();
    }

    @Override
    protected void onCreation() {
        //override
    }

    @Override
    protected void onResumption() {
        initializeMainServiceContainers();
    }
}
