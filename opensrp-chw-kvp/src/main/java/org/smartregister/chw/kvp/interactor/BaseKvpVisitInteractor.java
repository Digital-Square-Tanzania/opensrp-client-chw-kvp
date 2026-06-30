package org.smartregister.chw.kvp.interactor;

import android.content.Context;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.AppExecutors;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.chw.kvp.util.VisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.clientandeventmodel.User;
import org.smartregister.domain.SyncStatus;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.VisibleForTesting;

import timber.log.Timber;

public class BaseKvpVisitInteractor implements BaseKvpVisitContract.Interactor {

    protected final LinkedHashMap<String, BaseKvpVisitAction> actionList = new LinkedHashMap<>();
    private final ECSyncHelper syncHelper;
    protected AppExecutors appExecutors;
    protected Map<String, List<VisitDetail>> details = null;
    protected String visitType;
    protected Context context;
    protected MemberObject memberObject;

    @VisibleForTesting
    public BaseKvpVisitInteractor(AppExecutors appExecutors, ECSyncHelper syncHelper) {
        this.appExecutors = appExecutors;
        this.syncHelper = syncHelper;

    }

    public BaseKvpVisitInteractor() {
        this(new AppExecutors(), KvpLibrary.getInstance().getEcSyncHelper());
    }

    public BaseKvpVisitInteractor(String visitType) {
        this(new AppExecutors(), KvpLibrary.getInstance().getEcSyncHelper());
        this.visitType = visitType;
    }

    protected String getCurrentVisitType() {
        if (StringUtils.isNotBlank(visitType)) {
            return visitType;
        }
        return Constants.EVENT_TYPE.KVP_PrEP_FOLLOW_UP_VISIT;
    }

    @Override
    public void reloadMemberDetails(String memberID, String profileType, BaseKvpVisitContract.InteractorCallBack callBack) {
        memberObject = getMemberClient(memberID, profileType);
        if (memberObject != null) {
            final Runnable runnable = () -> {
                appExecutors.mainThread().execute(() -> callBack.onMemberDetailsReloaded(memberObject));
            };
            appExecutors.diskIO().execute(runnable);
        }
    }

    /**
     * Default if profile type is not provided is KVP/PrEP member
     *
     * @param memberID    unique identifier for the user
     * @param profileType profile type being used
     * @return MemberObject wrapper for the user's data
     */
    @Override
    public MemberObject getMemberClient(String memberID, String profileType) {
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.KVP_PROFILE)) {
            return KvpDao.getKvpMember(memberID);
        }
        if (profileType.equalsIgnoreCase(Constants.PROFILE_TYPES.PrEP_PROFILE)) {
            return KvpDao.getPrEPMember(memberID);
        }
        return KvpDao.getMember(memberID);
    }

    @Override
    public void saveRegistration(String jsonString, boolean isEditMode, BaseKvpVisitContract.InteractorCallBack callBack) {
        Timber.v("saveRegistration");
    }

    @Override
    public void calculateActions(final BaseKvpVisitContract.View view, MemberObject memberObject, final BaseKvpVisitContract.InteractorCallBack callBack) {
        context = view.getContext();
        getDetailsOnEdit(view, memberObject);

        populateActionList(callBack);
    }

    protected void getDetailsOnEdit(BaseKvpVisitContract.View view, MemberObject memberObject) {
        if (view.getEditMode()) {
            Visit lastVisit = KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), getCurrentVisitType());

            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(KvpLibrary.getInstance().visitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }
    }

    protected BaseKvpVisitAction.Builder getBuilder(String title) {
        return new BaseKvpVisitAction.Builder(context, title);
    }

    protected void populateActionList(BaseKvpVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            try {
                evaluateSampleAction(details);
            } catch (BaseKvpVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateSampleAction(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        BaseKvpVisitAction ba = getBuilder("Sample Action")
                .withSubtitle("")
                .withOptional(false)
                .withFormName("anc")
                .build();
        actionList.put("Sample Action", ba);

    }


    @Override
    public void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseKvpVisitAction> map, final BaseKvpVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            boolean result = true;
            try {
                submitVisit(editMode, memberID, map, "");
            } catch (Exception e) {
                Timber.e(e);
                result = false;
            }

            final boolean finalResult = result;
            appExecutors.mainThread().execute(() -> callBack.onSubmitted(finalResult));
        };

        appExecutors.diskIO().execute(runnable);
    }

    protected void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseKvpVisitAction> map, String parentEventType) throws Exception {
        // create a map of the different types

        Map<String, BaseKvpVisitAction> externalVisits = new HashMap<>();
        Map<String, String> combinedJsons = new HashMap<>();
        String payloadType = null;
        String payloadDetails = null;

        // aggregate forms to be processed
        for (Map.Entry<String, BaseKvpVisitAction> entry : map.entrySet()) {
            String json = entry.getValue().getJsonPayload();
            if (StringUtils.isNotBlank(json)) {
                // do not process events that are meant to be in detached mode
                // in a similar manner to the the aggregated events

                BaseKvpVisitAction action = entry.getValue();
                BaseKvpVisitAction.ProcessingMode mode = action.getProcessingMode();

                if (mode == BaseKvpVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    externalVisits.put(entry.getKey(), entry.getValue());
                } else {
                    combinedJsons.put(entry.getKey(), json);
                }

                payloadType = action.getPayloadType().name();
                payloadDetails = action.getPayloadDetails();
            }
        }

        String type = StringUtils.isBlank(parentEventType) ? getEncounterType() : getEncounterType();

        // persist to database
        Visit visit = saveVisit(editMode, memberID, type, combinedJsons, parentEventType);
        if (visit != null) {
            saveVisitDetails(visit, payloadType, payloadDetails);
            processExternalVisits(visit, externalVisits, memberID);
        }

        if (KvpLibrary.isSubmitOnSave()) {
            List<Visit> visits = new ArrayList<>(1);
            visits.add(visit);
            VisitUtils.processVisits(visits, KvpLibrary.getInstance().visitRepository(), KvpLibrary.getInstance().visitDetailsRepository());

            Context context = KvpLibrary.getInstance().context().applicationContext();

        }
    }

    /**
     * recursively persist visits to the db
     *
     * @param visit
     * @param externalVisits
     * @param memberID
     * @throws Exception
     */
    protected void processExternalVisits(Visit visit, Map<String, BaseKvpVisitAction> externalVisits, String memberID) throws Exception {
        if (visit != null && !externalVisits.isEmpty()) {
            for (Map.Entry<String, BaseKvpVisitAction> entry : externalVisits.entrySet()) {
                Map<String, BaseKvpVisitAction> subEvent = new HashMap<>();
                subEvent.put(entry.getKey(), entry.getValue());

                String subMemberID = entry.getValue().getBaseEntityID();
                if (StringUtils.isBlank(subMemberID))
                    subMemberID = memberID;

                submitVisit(false, subMemberID, subEvent, visit.getVisitType());
            }
        }
    }

    protected @Nullable Visit saveVisit(boolean editMode, String memberID, String encounterType,
                                        final Map<String, String> jsonString,
                                        String parentEventType
    ) throws Exception {

        AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();

        String derivedEncounterType = StringUtils.isBlank(parentEventType) ? encounterType : "";
        Event baseEvent = KvpJsonFormUtils.processVisitJsonForm(allSharedPreferences, memberID, derivedEncounterType, jsonString, getTableName());

        // only tag the first event with the date
        if (StringUtils.isBlank(parentEventType)) {
            prepareEvent(baseEvent);
        } else {
            prepareSubEvent(baseEvent);
        }

        if (baseEvent != null) {
            baseEvent.setFormSubmissionId(KvpJsonFormUtils.generateRandomUUIDString());
            KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);

            String visitID = (editMode) ?
                    visitRepository().getLatestVisit(memberID, getEncounterType()).getVisitId() :
                    KvpJsonFormUtils.generateRandomUUIDString();

            // reset database
            if (editMode) {
                deleteProcessedVisit(visitID, memberID);
                deleteOldVisit(visitID);
            }

            Visit visit = NCUtil.eventToVisit(baseEvent, visitID);
            visit.setPreProcessedJson(new Gson().toJson(baseEvent));
            visit.setParentVisitID(getParentVisitEventID(visit, parentEventType));

            visitRepository().addVisit(visit);
            return visit;
        }
        return null;
    }

    protected String getParentVisitEventID(Visit visit, String parentEventType) {
        return visitRepository().getParentVisitEventID(visit.getBaseEntityId(), parentEventType, visit.getDate());
    }

    @VisibleForTesting
    public VisitRepository visitRepository() {
        return KvpLibrary.getInstance().visitRepository();
    }

    protected void deleteOldVisit(String visitID) {
        visitRepository().deleteVisit(visitID);
        KvpLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(visitID);

        List<Visit> childVisits = visitRepository().getChildEvents(visitID);
        for (Visit v : childVisits) {
            visitRepository().deleteVisit(v.getVisitId());
            KvpLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(v.getVisitId());
        }
    }


    protected void deleteProcessedVisit(String visitID, String baseEntityId) {
        // check if the event
        AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
        Visit visit = visitRepository().getVisitByVisitId(visitID);
        if (visit == null || !visit.getProcessed()) return;

        Event processedEvent = HpsDao.getEventByFormSubmissionId(visit.getFormSubmissionId());
        if (processedEvent == null) return;

        deleteSavedEvent(allSharedPreferences, baseEntityId, processedEvent.getEventId(), processedEvent.getFormSubmissionId(), "event");
        HpsLibrary.getInstance().visitRepository().deleteVisit(visitID);
    }

    protected void deleteSavedEvent(AllSharedPreferences allSharedPreferences, String baseEntityId, String eventId, String formSubmissionId, String type) {
        Event event = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(Constants.EVENT_TYPE.DELETE_EVENT)
                .withLocationId(HpsJsonFormUtils.locationId(allSharedPreferences))
                .withProviderId(allSharedPreferences.fetchRegisteredANM())
                .withEntityType(type)
                .withFormSubmissionId(UUID.randomUUID().toString())
                .withDateCreated(new Date());

        event.addDetails(Constants.JSON_FORM_EXTRA.DELETE_EVENT_ID, eventId);
        event.addDetails(Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID, formSubmissionId);

        try {
            syncHelper.addEvent(event.getBaseEntityId(), new JSONObject(HpsJsonFormUtils.gson.toJson(event)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void saveVisitDetails(Visit visit, String payloadType, String payloadDetails) {
        if (visit.getVisitDetails() == null) return;

        for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
            if (entry.getValue() != null) {
                for (VisitDetail d : entry.getValue()) {
                    d.setPreProcessedJson(payloadDetails);
                    d.setPreProcessedType(payloadType);
                    KvpLibrary.getInstance().visitDetailsRepository().addVisitDetails(d);
                }
            }
        }
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add kvp_visit_date date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            baseEvent.addObs(new Obs("concept", "text", "kvp_visit_date", "",
                    list, new ArrayList<>(), null, "kvp_visit_date"));
        }
    }

    /**
     * injects additional meta data to the event
     *
     * @param baseEvent
     */
    protected void prepareSubEvent(Event baseEvent) {
        Timber.v("You can add information to sub events");
    }

    protected String getEncounterType() {
        return Constants.EVENT_TYPE.KVP_PrEP_FOLLOW_UP_VISIT;
    }

    protected String getTableName() {
        return Constants.TABLES.KVP_PrEP_FOLLOWUP;
    }
}
