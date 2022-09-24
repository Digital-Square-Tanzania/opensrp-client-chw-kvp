package org.smartregister.chw.kvp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.smartregister.chw.kvp.adapter.BaseServiceCardAdapter;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.ServiceCard;
import org.smartregister.chw.kvp.handlers.BaseServiceActionHandler;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.kvp.R;
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

    protected void initializeMainServiceContainers() {
        List<ServiceCard> serviceCards = new ArrayList<>();

        ServiceCard bioMedicalService = new ServiceCard();
        bioMedicalService.setServiceName("Bio-Medical Services");
        bioMedicalService.setId(Constants.SERVICES.KVP_BIO_MEDICAL);
        bioMedicalService.setActionItems(20);
        bioMedicalService.setServiceStatus("In-Progress");
        bioMedicalService.setServiceIcon(R.drawable.ic_bio_medical);
        bioMedicalService.setBackground(R.drawable.purple_bg);
        bioMedicalService.setEventServiceName(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
        serviceCards.add(bioMedicalService);

        ServiceCard behavioralService = new ServiceCard();
        behavioralService.setServiceName("Behavioral Services");
        behavioralService.setId(Constants.SERVICES.KVP_BEHAVIORAL);
        behavioralService.setActionItems(20);
        behavioralService.setServiceStatus("Not Started");
        behavioralService.setServiceIcon(R.drawable.ic_behavioral);
        behavioralService.setBackground(R.drawable.orange_bg);
        behavioralService.setEventServiceName(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT);
        serviceCards.add(behavioralService);

        ServiceCard structuralService = new ServiceCard();
        structuralService.setServiceName("Structural Services");
        structuralService.setId(Constants.SERVICES.KVP_STRUCTURAL);
        structuralService.setActionItems(20);
        structuralService.setServiceStatus("Complete");
        structuralService.setServiceIcon(R.drawable.ic_structural);
        structuralService.setBackground(R.drawable.dark_blue_bg);
        structuralService.setEventServiceName(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT);
        serviceCards.add(structuralService);

        ServiceCard otherService = new ServiceCard();
        otherService.setServiceName("Other Services");
        otherService.setId(Constants.SERVICES.KVP_OTHERS);
        otherService.setActionItems(20);
        otherService.setServiceStatus("Not Started");
        otherService.setServiceIcon(R.drawable.ic_others);
        otherService.setBackground(R.drawable.ocean_blue_bg);
        otherService.setEventServiceName(Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT);
        serviceCards.add(otherService);

        serviceCardAdapter.setServiceCards(serviceCards);
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
        //override
    }
}