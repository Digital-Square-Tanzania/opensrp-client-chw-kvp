package org.smartregister.chw.kvp.util;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.util.Utils.getAllSharedPreferences;

public class NCUtil {

    private static String[] default_obs = {"start", "end", "deviceid", "subscriberid", "simserial", "phonenumber"};

    public static SimpleDateFormat getSourceDateFormat() {
        return new SimpleDateFormat(KvpLibrary.getInstance().getSourceDateFormat(), Locale.getDefault());
    }

    public static SimpleDateFormat getSaveDateFormat() {
        return new SimpleDateFormat(KvpLibrary.getInstance().getSaveDateFormat(), Locale.getDefault());
    }

    public static void addEvent(AllSharedPreferences allSharedPreferences, Event baseEvent) throws Exception {
        if (baseEvent != null) {
            KvpJsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            getSyncHelper().addEvent(baseEvent.getBaseEntityId(), eventJson);
        }
    }

    public static void processEvent(String baseEntityID, JSONObject eventJson) throws Exception {
        if (eventJson != null) {
            getSyncHelper().addEvent(baseEntityID, eventJson);

            long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            getClientProcessorForJava().processClient(getSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        }
    }

    public static void startClientProcessing() throws Exception {
        long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);
        getClientProcessorForJava().processClient(getSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
        getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
    }

    public static ECSyncHelper getSyncHelper() {
        return KvpLibrary.getInstance().getEcSyncHelper();
    }

    public static ClientProcessorForJava getClientProcessorForJava() {
        return KvpLibrary.getInstance().getClientProcessorForJava();
    }

    public static Visit eventToVisit(Event event, String visitID) throws JSONException {
        Visit visit = new Visit();
        visit.setVisitId(visitID);
        visit.setBaseEntityId(event.getBaseEntityId());
        visit.setDate(event.getEventDate());
        visit.setVisitType(event.getEventType());
        visit.setEventId(event.getEventId());
        visit.setFormSubmissionId(event.getFormSubmissionId());
        visit.setJson(new JSONObject(JsonFormUtils.gson.toJson(event)).toString());
        visit.setProcessed(false);
        visit.setCreatedAt(new Date());
        visit.setUpdatedAt(new Date());
        Map<String, String> eventDetails = event.getDetails();
        if (eventDetails != null)
            visit.setVisitGroup(eventDetails.get(Constants.KVP_VISIT_GROUP));

        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (event.getObs() != null) {
            details = eventsObsToDetails(event.getObs(), visit.getVisitId(), null);
        }

        visit.setVisitDetails(details);
        return visit;
    }

    public static Map<String, List<VisitDetail>> eventsObsToDetails(List<Obs> obsList, String visitID, String baseEntityID) throws JSONException {
        List<String> exceptions = Arrays.asList(default_obs);
        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (obsList == null)
            return details;

        for (Obs obs : obsList) {
            if (!exceptions.contains(obs.getFormSubmissionField())) {
                VisitDetail detail = new VisitDetail();
                detail.setVisitDetailsId(JsonFormUtils.generateRandomUUIDString());
                detail.setVisitId(visitID);
                detail.setBaseEntityId(baseEntityID);
                detail.setVisitKey(obs.getFormSubmissionField());
                detail.setParentCode(obs.getParentCode());
                detail.setDetails(getDetailsValue(detail, obs.getValues().toString()));
                detail.setHumanReadable(getDetailsValue(detail, obs.getHumanReadableValues().toString()));
                detail.setJsonDetails(new JSONObject(JsonFormUtils.gson.toJson(obs)).toString());
                detail.setProcessed(false);
                detail.setCreatedAt(new Date());
                detail.setUpdatedAt(new Date());

                List<VisitDetail> currentList = details.get(detail.getVisitKey());
                if (currentList == null)
                    currentList = new ArrayList<>();

                currentList.add(detail);
                details.put(detail.getVisitKey(), currentList);
            }
        }

        return details;
    }

    public static String getFormattedDate(SimpleDateFormat source_sdf, SimpleDateFormat dest_sdf, String value) {

        try {
            Date date = source_sdf.parse(value);
            return dest_sdf.format(date);
        } catch (Exception e) {
            try {
                // fallback for long datetypes
                Date date = new Date(Long.parseLong(value));
                return dest_sdf.format(date);
            } catch (NumberFormatException | NullPointerException nfe) {
                Timber.e(e);
            }
            Timber.e(e);
        }
        return value;
    }

    // executed before processing
    public static Visit eventToVisit(Event event) throws JSONException {
        return eventToVisit(event, JsonFormUtils.generateRandomUUIDString());
    }

    public static void processKvpVisit(EventClient baseEvent) {
        processKvpVisit(baseEvent, null);
    }

    public static void processSubKvpVisit(EventClient baseEvent, String parentEventType) {
        processKvpVisit(baseEvent, null, parentEventType);
    }

    public static void processSubKvpVisit(EventClient baseEvent, SQLiteDatabase database, String parentEventType) {
        processKvpVisit(baseEvent, database, parentEventType);
    }

    public static void processKvpVisit(EventClient baseEvent, SQLiteDatabase database) {
        processKvpVisit(baseEvent, database, null);
    }

    public static void processKvpVisit(EventClient baseEvent, SQLiteDatabase database, String parentEventType) {
        try {
            Visit visit = KvpLibrary.getInstance().visitRepository().getVisitByFormSubmissionID(baseEvent.getEvent().getFormSubmissionId());
            if (visit == null) {
                visit = eventToVisit(baseEvent.getEvent());

                if (StringUtils.isNotBlank(parentEventType) && !parentEventType.equalsIgnoreCase(visit.getVisitType())) {
                    String parentVisitID = KvpLibrary.getInstance().visitRepository().getParentVisitEventID(visit.getBaseEntityId(), parentEventType, visit.getDate());
                    visit.setParentVisitID(parentVisitID);
                }

                if (database != null) {
                    KvpLibrary.getInstance().visitRepository().addVisit(visit, database);
                } else {
                    KvpLibrary.getInstance().visitRepository().addVisit(visit);
                }
                if (visit.getVisitDetails() != null) {
                    for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
                        if (entry.getValue() != null) {
                            for (VisitDetail detail : entry.getValue()) {
                                if (database != null) {
                                    KvpLibrary.getInstance().visitDetailsRepository().addVisitDetails(detail, database);
                                } else {
                                    KvpLibrary.getInstance().visitDetailsRepository().addVisitDetails(detail);
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }


    // executed by event client processor
    public static Visit eventToVisit(org.smartregister.domain.Event event) throws JSONException {
        List<String> exceptions = Arrays.asList(default_obs);

        Visit visit = new Visit();
        visit.setVisitId(org.smartregister.chw.kvp.util.KvpJsonFormUtils.generateRandomUUIDString());
        visit.setBaseEntityId(event.getBaseEntityId());
        visit.setDate(event.getEventDate().toDate());
        visit.setVisitType(event.getEventType());
        visit.setEventId(event.getEventId());
        visit.setFormSubmissionId(event.getFormSubmissionId());
        visit.setJson(new JSONObject(org.smartregister.chw.kvp.util.KvpJsonFormUtils.gson.toJson(event)).toString());
        visit.setProcessed(true);
        visit.setCreatedAt(new Date());
        visit.setUpdatedAt(new Date());
        Map<String, String> eventDetails = event.getDetails();
        if (eventDetails != null)
            visit.setVisitGroup(eventDetails.get(Constants.KVP_VISIT_GROUP));

        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (event.getObs() != null) {
            for (org.smartregister.domain.Obs obs : event.getObs()) {
                if (!exceptions.contains(obs.getFormSubmissionField())) {
                    VisitDetail detail = new VisitDetail();
                    detail.setVisitDetailsId(org.smartregister.chw.kvp.util.KvpJsonFormUtils.generateRandomUUIDString());
                    detail.setVisitId(visit.getVisitId());
                    detail.setVisitKey(obs.getFormSubmissionField());
                    detail.setParentCode(obs.getParentCode());
                    detail.setDetails(getDetailsValue(detail, obs.getValues().toString()));
                    detail.setHumanReadable(getDetailsValue(detail, obs.getHumanReadableValues().toString()));
                    detail.setProcessed(true);
                    detail.setCreatedAt(new Date());
                    detail.setUpdatedAt(new Date());

                    List<VisitDetail> currentList = details.get(detail.getVisitKey());
                    if (currentList == null)
                        currentList = new ArrayList<>();

                    currentList.add(detail);
                    details.put(detail.getVisitKey(), currentList);
                }
            }
        }

        visit.setVisitDetails(details);
        return visit;
    }

    public static String getDetailsValue(VisitDetail detail, String val) {
        String clean_val = KvpJsonFormUtils.cleanString(val);
        if (detail.getVisitKey().contains("date")) {
            return getFormattedDate(getSourceDateFormat(), getSaveDateFormat(), clean_val);
        }

        return clean_val;
    }


    public static String removeSpaces(String s) {
        return s.replace(" ", "_").toLowerCase();
    }

    /**
     * Extract value from VisitDetail
     *
     * @return
     */
    @NotNull
    public static String getText(@Nullable VisitDetail visitDetail) {
        if (visitDetail == null)
            return "";

        String val = visitDetail.getHumanReadable();
        if (StringUtils.isNotBlank(val))
            return val.trim();

        return (StringUtils.isNotBlank(visitDetail.getDetails())) ? visitDetail.getDetails().trim() : "";
    }

    @NotNull
    public static String getText(@Nullable List<VisitDetail> visitDetails) {
        if (visitDetails == null)
            return "";

        List<String> vals = new ArrayList<>();
        for (VisitDetail vd : visitDetails) {
            String val = getText(vd);
            if (StringUtils.isNotBlank(val))
                vals.add(val);
        }

        return toCSV(vals);
    }

    public static List<String> getTexts(@Nullable List<VisitDetail> visitDetails) {
        if (visitDetails == null)
            return null;

        List<String> texts = new ArrayList<>();
        for (VisitDetail vd : visitDetails) {
            String val = getText(vd);
            if (StringUtils.isNotBlank(val))
                texts.add(val);
        }

        return texts;
    }

    public static String toCSV(List<String> list) {
        String result = "";
        if (list.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s).append(", ");
            }
            result = sb.deleteCharAt(sb.length() - 2).toString();
        }
        return result.trim();
    }
}
