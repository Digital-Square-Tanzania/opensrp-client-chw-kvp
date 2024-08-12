package org.smartregister.chw.kvp.util;

import static org.smartregister.util.JsonFormUtils.getFieldValue;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

public class PrEPVisitsUtil extends VisitUtils {

    public static String Complete = "complete";
    public static String Pending = "pending";
    public static String Ongoing = "ongoing";

    public static void processVisits() throws Exception {
        processVisits(KvpLibrary.getInstance().visitRepository(), KvpLibrary.getInstance().visitDetailsRepository());
    }

    private static void processVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository) throws Exception {
        List<Visit> visits = visitRepository.getAllUnSynced();
        List<Visit> prepFollowupVisit = new ArrayList<>();

        for (Visit v : visits) {
            Date updatedAtDate = new Date(v.getUpdatedAt().getTime());
            int daysDiff = TimeUtils.getElapsedDays(updatedAtDate);
            if (daysDiff > 1) {
                if (v.getVisitType().equalsIgnoreCase(Constants.EVENT_TYPE.PrEP_FOLLOWUP_VISIT) && getPrEPVisitStatus(v).equals(Complete)) {
                    prepFollowupVisit.add(v);
                    try {
                        if (getHepTesting(v, "tested_hbv").equals("yes")) {
                            Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(v.getVisitId() + "-hep_b", v.getBaseEntityId(), "hep_b", getHepTesting(v, "hbv_results"), v.getDate());
                            if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                                baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                            AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                            KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                            NCUtil.addEvent(allSharedPreferences, baseEvent);
                            NCUtil.startClientProcessing();
                        }

                        if (getHepTesting(v, "tested_hcv").equals("yes")) {
                            Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(v.getVisitId() + "-hep_c", v.getBaseEntityId(), "hep_c", getHepTesting(v, "hcv_results"), v.getDate());
                            if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                                baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                            AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                            KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                            NCUtil.addEvent(allSharedPreferences, baseEvent);
                            NCUtil.startClientProcessing();
                        }

                        if (getHepTesting(v, "tested_crcl").equals("yes")) {
                            Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(v.getVisitId() + "-crcl", v.getBaseEntityId(), "crcl", getHepTesting(v, "crcl_results"), v.getDate());
                            if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                                baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                            AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                            KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                            NCUtil.addEvent(allSharedPreferences, baseEvent);
                            NCUtil.startClientProcessing();
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
            }
        }
        if (!prepFollowupVisit.isEmpty()) {
            processVisits(prepFollowupVisit, visitRepository, visitDetailsRepository);
            for (Visit v : prepFollowupVisit) {
                if (shouldCreateCloseVisitEvent(v)) {
                    createCancelledEvent(v.getJson());
                }
            }
        }

    }

    private static void createCancelledEvent(String json) throws Exception {
        Event baseEvent = new Gson().fromJson(json, Event.class);
        baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
        baseEvent.setEventType(Constants.EVENT_TYPE.PrEP_CLIENT_NOT_ELIGIBLE);
        AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
        NCUtil.addEvent(allSharedPreferences, baseEvent);
        NCUtil.startClientProcessing();
    }

    public static String getPrEPVisitStatus(Visit lastVisit) {
        HashMap<String, Boolean> completionObject = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(lastVisit.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");

            completionObject.put("is-visit_type-done", computeCompletionStatus(obs, "place"));
            completionObject.put("is-prep_screening-done", computeCompletionStatus(obs, "diabetes"));
            if (checkIfShouldInitiateToPrEP(obs)) {
                completionObject.put("is-prep_initiation-done", computeCompletionStatus(obs, "prep_status"));
                completionObject.put("is-other_services-done", computeCompletionStatus(obs, "health_edu_sti_provided"));
            }


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
        List<Visit> manualProcessedVisits = new ArrayList<>();
        VisitDetailsRepository visitDetailsRepository = KvpLibrary.getInstance().visitDetailsRepository();
        VisitRepository visitRepository = KvpLibrary.getInstance().visitRepository();
        manualProcessedVisits.add(visit);
        processVisits(manualProcessedVisits, visitRepository, visitDetailsRepository);
        if (shouldCreateCloseVisitEvent(visit)) {
            createCancelledEvent(visit.getJson());
        }

        try {
            if (getHepTesting(visit, "tested_hbv").equals("yes")) {
                Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(visit.getVisitId() + "-hep_b", visit.getBaseEntityId(), "hep_b", getHepTesting(visit, "hbv_results"), visit.getDate());
                if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                    baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                NCUtil.addEvent(allSharedPreferences, baseEvent);
                NCUtil.startClientProcessing();
            }

            if (getHepTesting(visit, "tested_hcv").equals("yes")) {
                Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(visit.getVisitId() + "-hep_c", visit.getBaseEntityId(), "hep_c", getHepTesting(visit, "hcv_results"), visit.getDate());
                if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                    baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                NCUtil.addEvent(allSharedPreferences, baseEvent);
                NCUtil.startClientProcessing();
            }

            if (getHepTesting(visit, "tested_crcl").equals("yes")) {
                Event baseEvent = generateHepAndCrclTestResultsRegistrationEvent(visit.getVisitId() + "-crcl", visit.getBaseEntityId(), "crcl", getHepTesting(visit, "crcl_results"), visit.getDate());
                if (StringUtils.isBlank(baseEvent.getFormSubmissionId()))
                    baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                AllSharedPreferences allSharedPreferences = KvpLibrary.getInstance().context().allSharedPreferences();
                KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                NCUtil.addEvent(allSharedPreferences, baseEvent);
                NCUtil.startClientProcessing();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static boolean checkIfShouldInitiateToPrEP(JSONArray obs) throws JSONException {
        String shouldInitiate = "";
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject checkObj = obs.getJSONObject(i);
            if (checkObj.getString("fieldCode").equalsIgnoreCase("should_initiate")) {
                JSONArray values = checkObj.getJSONArray("values");
                shouldInitiate = values.getString(0);
                break;
            }
        }
        return shouldInitiate.equalsIgnoreCase("yes");
    }

    public static boolean checkIfShouldRemainToPrEP(JSONArray obs) throws JSONException {
        String reasons_stopping_prep = "";
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject checkObj = obs.getJSONObject(i);
            if (checkObj.getString("fieldCode").equalsIgnoreCase("reasons_stopping_prep")) {
                JSONArray values = checkObj.getJSONArray("values");
                reasons_stopping_prep = values.toString();
                break;
            }
        }
        return reasons_stopping_prep.contains("hiv_positive");
    }

    private static boolean shouldCreateCloseVisitEvent(Visit v) {
        try {
            JSONObject jsonObject = new JSONObject(v.getJson());
            JSONArray obs = jsonObject.getJSONArray("obs");
            return checkIfShouldRemainToPrEP(obs);
        } catch (Exception e) {
            Timber.e(e);
        }
        return false;
    }

    protected static Event generateHepAndCrclTestResultsRegistrationEvent(String visitId, String baseEntityId, String testType, String testResults, Date testsDate) {
        Event hepTestResultsRegistrationEvent = new Event();

        hepTestResultsRegistrationEvent.setEntityType("ec_prep_hepatitis_and_crcl_test_results");
        hepTestResultsRegistrationEvent.setEventType("Hepatitis and CrCL Test Results Registration");
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
}
