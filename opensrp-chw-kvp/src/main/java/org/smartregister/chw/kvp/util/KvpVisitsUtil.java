package org.smartregister.chw.kvp.util;

import static org.smartregister.util.JsonFormUtils.getFieldValue;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

public class KvpVisitsUtil extends VisitUtils {

    private static final String CERVICAL_SCREENING_DONE = "is-cervical_cancer_screening-done";

    public static String Complete = "complete";
    public static String Pending = "pending";
    public static String Ongoing = "ongoing";

    public static void processVisits() throws Exception {
        VisitRepository repository = KvpLibrary.getInstance().visitRepository();
        List<Visit> visits = repository.getAllUnSynced();
        boolean queued = false;
        for (String type : MarpVisitProcessing.SERVICE_TYPES) {
            String visitGroupId = UUID.randomUUID().toString();
            for (Visit visit : visits) {
                if (!type.equalsIgnoreCase(visit.getVisitType()) || !isReadyForScheduledProcessing(visit)) {
                    continue;
                }
                try {
                    queued |= MarpVisitProcessing.enqueue(repository, visit, visitGroupId,
                            KvpVisitsUtil::isReadyForScheduledProcessing, KvpVisitsUtil::addMarpEvent);
                } catch (Exception exception) {
                    Timber.e(exception, "Unable to queue a scheduled MARP service card");
                }
            }
        }
        if (queued || MarpVisitProcessing.hasUnprocessedPrimary(repository)) {
            processLocalQueue();
        }
    }

    static boolean isReadyForScheduledProcessing(Visit visit) {
        if (TimeUtils.getElapsedDays(visit.getUpdatedAt()) <= 1) {
            return false;
        }

        String type = visit.getVisitType();
        if (Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT.equalsIgnoreCase(type)) {
            return Complete.equals(getBioMedicalStatus(visit));
        }
        if (Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT.equalsIgnoreCase(type)) {
            return Complete.equals(getBehavioralServiceStatus(visit));
        }
        if (Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT.equalsIgnoreCase(type)) {
            return Complete.equals(getStructuralServiceStatus(visit));
        }
        return Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT.equalsIgnoreCase(type)
                && Complete.equals(getOtherServiceStatus(visit));
    }

    protected static Event generateHivRegistrationForPositiveKvpClients(String jsonString, String baseEntityId) {
        Event hivRegistrationEvent = new Gson().fromJson(jsonString, Event.class);

        hivRegistrationEvent.setEntityType("ec_hiv_register");
        hivRegistrationEvent.setEventType("HIV Registration");
        hivRegistrationEvent.setBaseEntityId(baseEntityId);
        hivRegistrationEvent.addObs(
                (new Obs())
                        .withFormSubmissionField("hiv_registration_date")
                        .withValue(System.currentTimeMillis())
                        .withFieldCode("hiv_registration_date")
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));
        hivRegistrationEvent.addObs(
                (new Obs())
                        .withFormSubmissionField("client_hiv_status_during_registration")
                        .withValue("positive")
                        .withFieldCode("client_hiv_status_during_registration")
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));
        hivRegistrationEvent.addObs(
                (new Obs())
                        .withFieldCode("client_hiv_status_after_testing")
                        .withFormSubmissionField("test_results")
                        .withValue("positive")
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));

        return hivRegistrationEvent;
    }

    protected static Event generateHepTestResultsRegistrationEvent(String visitId, String baseEntityId, String testType, String testResults, Date testsDate) {
        Event hepTestResultsRegistrationEvent = new Event();

        hepTestResultsRegistrationEvent.setEntityType("ec_kvp_hepatitis_test_results");
        hepTestResultsRegistrationEvent.setEventType("Hepatitis Test Results Registration");
        hepTestResultsRegistrationEvent.setBaseEntityId(baseEntityId);
        hepTestResultsRegistrationEvent.addObs(
                (new Obs())
                        .withFormSubmissionField(DBConstants.KEY.VISIT_ID)
                        .withValue(visitId)
                        .withFieldCode(DBConstants.KEY.VISIT_ID)
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));

        hepTestResultsRegistrationEvent.addObs(
                (new Obs())
                        .withFormSubmissionField("test_type")
                        .withValue(testType)
                        .withFieldCode("test_type")
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));

        hepTestResultsRegistrationEvent.addObs(
                (new Obs())
                        .withFormSubmissionField("test_date")
                        .withValue(testsDate)
                        .withFieldCode("test_date")
                        .withFieldType("formsubmissionField")
                        .withFieldDataType("text")
                        .withParentCode("")
                        .withHumanReadableValues(new ArrayList<>()));

        if (testResults != null) {
            hepTestResultsRegistrationEvent.addObs(
                    (new Obs())
                            .withFormSubmissionField("test_results")
                            .withValue(testResults)
                            .withFieldCode("test_results")
                            .withFieldType("formsubmissionField")
                            .withFieldDataType("text")
                            .withParentCode("")
                            .withHumanReadableValues(new ArrayList<>()));

        }
        return hepTestResultsRegistrationEvent;
    }

    public static String getBioMedicalStatus(Visit lastVisit) {
        HashMap<String, Boolean> completionObject = new HashMap<>();
        String gender = getKvpMemberGender(lastVisit.getBaseEntityId());
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            completionObject.put("is-client_status-done", computeCompletionStatus(obs, "client_status"));
            completionObject.put("is-condom_provision-done", computeCompletionStatus(obs, "condoms_given"));
            completionObject.put("is-hts-done", computeCompletionStatus(obs, "tested_hiv"));
            completionObject.put("is-hepatitis-done", computeCompletionStatus(obs, "hep_b_testing"));
            completionObject.put("is-family_planning-done", computeCompletionStatus(obs, "family_planning_service"));

            //TODO add check to see if this action was shown
            //completionObject.put("is-mat-done", computeCompletionStatus(obs, "mat_provided"));

            if (gender.equalsIgnoreCase(Constants.MALE)) {
                String vmmcServices = KvpDao.getKvpVmmcServices(lastVisit.getBaseEntityId());
                if (StringUtils.isBlank(vmmcServices) || vmmcServices.equalsIgnoreCase("not_provided"))
                    completionObject.put("is-vmmc-done", computeCompletionStatus(obs, "vmcc_provided"));
            } else {
                completionObject.put(CERVICAL_SCREENING_DONE, computeCompletionStatus(obs, "cervical_cancer_screening"));
            }
            completionObject.put("is-tb_screening-done", computeCompletionStatus(obs, "tb_screening"));

            if (!completionObject.get("is-family_planning-done")
                    && KvpLibrary.getInstance().isFamilyPlanningExempt(lastVisit, gender)) {
                completionObject.remove("is-family_planning-done");
            }
            if (Boolean.FALSE.equals(completionObject.get(CERVICAL_SCREENING_DONE))
                    && KvpLibrary.getInstance().isCervicalScreeningExempt(lastVisit, gender)) {
                completionObject.remove(CERVICAL_SCREENING_DONE);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return getActionStatus(completionObject);
    }

    public static String getHivStatus(Visit lastVisit) {
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            for (int i = 0; i < obs.length(); i++) {
                JSONObject checkObj = obs.getJSONObject(i);
                if (checkObj.getString("fieldCode").equalsIgnoreCase("hiv_status")) {
                    JSONArray values = checkObj.getJSONArray("values");
                    return values.getString(0);
                }
            }

            return getFieldValue(obs, "hiv_status");
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    public static String getHepTesting(Visit lastVisit, String key) {
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            for (int i = 0; i < obs.length(); i++) {
                JSONObject checkObj = obs.getJSONObject(i);
                if (checkObj.getString("fieldCode").equalsIgnoreCase(key)) {
                    JSONArray values = checkObj.getJSONArray("values");
                    return values.getString(0);
                }
            }
            return getFieldValue(obs, key);
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    public static String getBehavioralServiceStatus(Visit lastVisit) {
        HashMap<String, Boolean> completionObject = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            KvpLibrary.BehavioralCompletion behavioralCompletion = KvpLibrary.getBehavioralCompletion();
            if (behavioralCompletion != null) {
                return behavioralCompletion.isComplete(obs) ? Complete : Pending;
            }

            completionObject.put("is-iec_sbcc-done", computeCompletionStatus(obs, "iec_sbcc_materials"));
            completionObject.put("is-health_education-done", computeCompletionStatus(obs, "health_education_provided"));

        } catch (Exception e) {
            Timber.e(e);
        }
        return getActionStatus(completionObject);
    }

    public static String getStructuralServiceStatus(Visit lastVisit) {
        HashMap<String, Boolean> completionObject = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            completionObject.put("is-gbv_analysis-done", computeCompletionStatus(obs, "gbv_screening"));

        } catch (Exception e) {
            Timber.e(e);
        }
        return getActionStatus(completionObject);
    }

    public static String getOtherServiceStatus(Visit lastVisit) {
        HashMap<String, Boolean> completionObject = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            completionObject.put("is-other_services_and_referrals-done", computeCompletionStatus(obs, "other_services_referrals_provided"));

        } catch (Exception e) {
            Timber.e(e);
        }
        return getActionStatus(completionObject);
    }

    public static String getActionStatus(Map<String, Boolean> checkObject) {
        for (Map.Entry<String, Boolean> entry : checkObject.entrySet()) {
            if (entry.getValue()) {
                if (checkObject.containsValue(false)) {
                    return Ongoing;
                }
                return Complete;
            }
        }
        return Pending;
    }

    public static boolean computeCompletionStatus(JSONArray obs, String checkString) throws JSONException {
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject checkObj = obs.getJSONObject(i);
            if (checkObj.getString("fieldCode").equalsIgnoreCase(checkString)) {
                return true;
            }
        }
        return false;
    }

    public static void manualProcessVisit(Visit visit) throws Exception {
        KvpLibrary library = KvpLibrary.getInstance();
        if (visit != null && !MarpVisitProcessing.isServiceType(visit.getVisitType())) {
            processVisits(Collections.singletonList(visit), library.visitRepository(),
                    library.visitDetailsRepository());
        } else if (MarpVisitProcessing.enqueue(library.visitRepository(), visit, UUID.randomUUID().toString(),
                current -> true, KvpVisitsUtil::addMarpEvent)) {
            processLocalQueue();
        }
    }

    private static void addMarpEvent(Event event) throws Exception {
        NCUtil.addEvent(KvpLibrary.getInstance().context().allSharedPreferences(), event);
    }

    private static void processLocalQueue() throws Exception {
        // Lock the processor before reading the normal queue so these two MARP routes
        // cannot both hand it the same detached unprocessed event list.
        synchronized (NCUtil.getClientProcessorForJava()) {
            NCUtil.startClientProcessing();
        }
    }

    public static String getKvpMemberGender(String baseEntityId) {
        MemberObject memberObject = KvpDao.getKvpMember(baseEntityId);
        if (memberObject != null) {
            return memberObject.getGender();
        }
        return "";
    }
}
