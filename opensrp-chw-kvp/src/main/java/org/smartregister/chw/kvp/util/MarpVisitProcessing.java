package org.smartregister.chw.kvp.util;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.gson.Gson;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.BaseRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;

/** Local processing of one persisted MARP service card submission. */
public final class MarpVisitProcessing {

    static final String[] SERVICE_TYPES = {KVP_BIO_MEDICAL_SERVICE_VISIT, KVP_BEHAVIORAL_SERVICE_VISIT,
            KVP_STRUCTURAL_SERVICE_VISIT, KVP_OTHER_SERVICE_VISIT};
    private static final String[] HEPATITIS_ROLES = {"hep_b", "hep_c"};
    private static final String HEPATITIS_REGISTRATION = "Hepatitis Test Results Registration";
    private static final Gson GSON = new Gson();
    private static final String PARENT_SUBMISSION = "marp_parent_submission_id";
    private static final String PARENT_VISIT = "marp_parent_visit_id";
    private static final String FAILURES = "marp_visit_projection_failure";

    public enum Outcome {
        STALE_OR_INVALID, NOT_ENQUEUED, ENQUEUE_INCOMPLETE, QUEUED, PROJECTION_FAILED, LOCAL_SUCCESS, UNVERIFIED
    }

    interface Readiness {
        boolean test(Visit visit);
    }

    interface EventWriter {
        void add(Event event) throws Exception;
    }

    private MarpVisitProcessing() {
    }

    public static boolean isServiceType(String type) {
        for (String serviceType : SERVICE_TYPES) {
            if (serviceType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    static boolean enqueue(VisitRepository repository, Visit snapshot, String visitGroup,
                           Readiness ready, EventWriter writer) throws Exception {
        if (snapshot == null || StringUtils.isBlank(snapshot.getVisitId())) {
            return false;
        }
        SQLiteDatabase database = repository.getWritableDatabase();
        // The writer transaction itself is the claim. Visit replacement and the event repository
        // use the same database. A moved or completed submission cannot win a second claim.
        database.beginTransactionNonExclusive();
        try {
            Visit current = repository.getVisitByVisitId(database, snapshot.getVisitId());
            if (!sameSubmission(snapshot, current) || Boolean.TRUE.equals(current.getProcessed())
                    || !ready.test(current)) {
                return false;
            }

            Event primary = GSON.fromJson(current.getPreProcessedJson(), Event.class);
            Event retained = GSON.fromJson(current.getJson(), Event.class);
            if (!matches(current, primary) || !matches(current, retained)) {
                throw new IllegalStateException("MARP submission does not match its saved payloads");
            }
            primary.addDetails(Constants.KVP_VISIT_GROUP, visitGroup);
            List<Event> followUps = followUps(current);
            for (Event followUp : followUps) {
                if (HEPATITIS_REGISTRATION.equals(followUp.getEventType())
                        && hasAmbiguousLegacyHepatitis(database, current, hepatitisVisitLink(followUp))) {
                    throw new IllegalStateException("The earlier Hepatitis event has no parent submission ID");
                }
            }

            enqueueOnce(database, current, primary, writer);
            for (Event followUp : followUps) {
                enqueueOnce(database, current, followUp, writer);
            }
            if (!repository.completeProcessing(database, current)) {
                throw new IllegalStateException("The claimed MARP submission could not be completed");
            }

            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    private static boolean sameSubmission(Visit snapshot, Visit current) {
        return current != null && isServiceType(current.getVisitType())
                && StringUtils.isNotBlank(snapshot.getFormSubmissionId())
                && StringUtils.isNotBlank(current.getBaseEntityId())
                && current.getVisitId().equals(snapshot.getVisitId())
                && current.getFormSubmissionId() != null
                && current.getFormSubmissionId().equals(snapshot.getFormSubmissionId())
                && current.getBaseEntityId().equals(snapshot.getBaseEntityId())
                && current.getVisitType().equals(snapshot.getVisitType())
                && current.getDate().equals(snapshot.getDate());
    }

    private static boolean matches(Visit visit, Event event) {
        return event != null && visit.getBaseEntityId().equals(event.getBaseEntityId())
                && visit.getVisitType().equals(event.getEventType())
                && visit.getFormSubmissionId().equals(event.getFormSubmissionId());
    }

    static List<Event> followUps(Visit visit) {
        List<Event> events = new ArrayList<>();
        if (!KVP_BIO_MEDICAL_SERVICE_VISIT.equalsIgnoreCase(visit.getVisitType())) {
            return events;
        }

        if ("positive".equalsIgnoreCase(KvpVisitsUtil.getHivStatus(visit))) {
            Event hiv = KvpVisitsUtil.generateHivRegistrationForPositiveKvpClients(visit.getJson(),
                    visit.getBaseEntityId());
            addFollowUp(events, visit, hiv, "hiv");
        }
        for (String testType : HEPATITIS_ROLES) {
            if ("yes".equals(KvpVisitsUtil.getHepTesting(visit, testType + "_testing"))) {
                Event hepatitis = KvpVisitsUtil.generateHepTestResultsRegistrationEvent(
                        visit.getVisitId() + "-" + testType, visit.getBaseEntityId(), testType,
                        KvpVisitsUtil.getHepTesting(visit, testType + "_result"), visit.getDate());
                addFollowUp(events, visit, hepatitis, testType);
            }
        }
        return events;
    }

    private static void addFollowUp(List<Event> events, Visit visit, Event event, String role) {
        event.setFormSubmissionId(followUpId(visit, role));
        event.setEventId(null);
        event.addDetails(PARENT_SUBMISSION, visit.getFormSubmissionId());
        event.addDetails(PARENT_VISIT, visit.getVisitId());
        events.add(event);
    }

    private static String followUpId(Visit visit, String role) {
        String key = "marp:" + role + ":" + visit.getVisitId() + ":" + visit.getFormSubmissionId();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void enqueueOnce(SQLiteDatabase database, Visit visit, Event event, EventWriter writer)
            throws Exception {
        JSONObject stored = readEvent(database, event.getFormSubmissionId());
        if (stored == null) {
            writer.add(event);
            stored = readEvent(database, event.getFormSubmissionId());
        }
        boolean followUp = !visit.getFormSubmissionId().equals(event.getFormSubmissionId());
        JSONObject details = stored == null ? null : stored.optJSONObject("details");
        if (!matchesEvent(stored, event.getFormSubmissionId(), event.getBaseEntityId(), event.getEventType())
                || (followUp && (details == null
                || !visit.getVisitId().equals(details.optString(PARENT_VISIT))
                || !visit.getFormSubmissionId().equals(details.optString(PARENT_SUBMISSION))))) {
            throw new IllegalStateException("The MARP event was not verified in the local queue");
        }
    }

    private static JSONObject readEvent(SQLiteDatabase database, String submissionId) throws JSONException {
        try (Cursor cursor = database.rawQuery(
                "SELECT json, baseEntityId, eventType FROM event WHERE formSubmissionId = ? LIMIT 2",
                new String[]{submissionId})) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            JSONObject event = new JSONObject(cursor.getString(0));
            if (!matchesEvent(event, submissionId, cursor.getString(1), cursor.getString(2))) {
                throw new IllegalStateException("The MARP event payload does not match its queue row");
            }
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one local event has the MARP submission ID");
            }
            return event;
        }
    }

    private static boolean matchesEvent(JSONObject event, String submissionId, String clientId, String type) {
        return event != null && submissionId.equals(event.optString("formSubmissionId"))
                && clientId != null && clientId.equals(event.optString("baseEntityId"))
                && type != null && type.equals(event.optString("eventType"));
    }

    private static String hepatitisVisitLink(Event event) {
        for (Obs observation : event.getObs()) {
            if (DBConstants.KEY.VISIT_ID.equals(observation.getFieldCode())) {
                return observation.getValue().toString();
            }
        }
        throw new IllegalStateException("MARP Hepatitis event has no visit link");
    }

    private static boolean hasAmbiguousLegacyHepatitis(SQLiteDatabase database, Visit visit, String visitLink)
            throws JSONException {
        try (Cursor cursor = database.rawQuery("SELECT json FROM event WHERE baseEntityId = ?"
                        + " AND eventType = ? AND instr(json, ?) > 0",
                new String[]{visit.getBaseEntityId(), HEPATITIS_REGISTRATION, visitLink})) {
            while (cursor.moveToNext()) {
                JSONObject event = new JSONObject(cursor.getString(0));
                JSONObject details = event.optJSONObject("details");
                if (details != null && !StringUtils.isBlank(details.optString(PARENT_SUBMISSION))) {
                    continue;
                }
                JSONArray observations = event.optJSONArray("obs");
                if (observations == null) {
                    continue;
                }
                for (int i = 0; i < observations.length(); i++) {
                    JSONObject observation = observations.getJSONObject(i);
                    JSONArray values = observation.optJSONArray("values");
                    if (DBConstants.KEY.VISIT_ID.equals(observation.optString("fieldCode"))
                            && values != null && visitLink.equals(values.optString(0))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Reads the current card, its primary event and its service projection. Callers should use
     * LOCAL_SUCCESS for closure evidence and take HTS from that exact Biomedical visit payload.
     */
    public static Outcome getLocalOutcome(Visit snapshot) {
        try {
            return getLocalOutcome(KvpLibrary.getInstance().visitRepository(), snapshot);
        } catch (Exception exception) {
            Timber.e(exception, "Unable to verify local MARP processing");
            return Outcome.UNVERIFIED;
        }
    }

    static Outcome getLocalOutcome(VisitRepository repository, Visit snapshot) throws JSONException {
        if (snapshot == null || StringUtils.isBlank(snapshot.getVisitId())) {
            return Outcome.STALE_OR_INVALID;
        }
        SQLiteDatabase database = repository.getWritableDatabase();
        boolean ownsTransaction = !database.inTransaction();
        if (ownsTransaction) {
            database.beginTransactionNonExclusive();
        }
        try {
            Visit current = repository.getVisitByVisitId(database, snapshot.getVisitId());
            if (!sameSubmission(snapshot, current)) {
                return Outcome.STALE_OR_INVALID;
            }
            JSONObject event = readEvent(database, current.getFormSubmissionId());
            if (event == null) {
                return Outcome.NOT_ENQUEUED;
            }
            if (!matchesEvent(event, current.getFormSubmissionId(), current.getBaseEntityId(), current.getVisitType())) {
                return Outcome.UNVERIFIED;
            }
            if (!Boolean.TRUE.equals(current.getProcessed())) {
                return Outcome.ENQUEUE_INCOMPLETE;
            }
            if (hasServiceProjection(database, current)) {
                JSONObject retained = new JSONObject(current.getJson());
                if (!matchesEvent(retained, current.getFormSubmissionId(), current.getBaseEntityId(), current.getVisitType())
                        || retained.optJSONArray("obs") == null) {
                    return Outcome.UNVERIFIED;
                }
                return Outcome.LOCAL_SUCCESS;
            }
            if (exists(database, "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", FAILURES)
                    && exists(database, "SELECT 1 FROM " + FAILURES + " WHERE visit_id = ? AND submission_id = ?",
                    current.getVisitId(), current.getFormSubmissionId())) {
                return Outcome.PROJECTION_FAILED;
            }
            return Outcome.QUEUED;
        } finally {
            if (ownsTransaction) {
                database.endTransaction();
            }
        }
    }

    static boolean hasUnprocessedPrimary(VisitRepository repository) {
        return exists(repository.getReadableDatabase(), "SELECT 1 FROM event e JOIN visits v"
                        + " ON e.formSubmissionId = v.form_submission_id AND e.baseEntityId = v.base_entity_id"
                        + " AND e.eventType = v.visit_type WHERE e.syncStatus = ? AND v.processed = 1"
                        + " AND v.visit_type IN (?, ?, ?, ?) LIMIT 1",
                BaseRepository.TYPE_Unprocessed, SERVICE_TYPES[0], SERVICE_TYPES[1], SERVICE_TYPES[2], SERVICE_TYPES[3]);
    }

    /** Invoked after the CHW processor returns from attempting this exact primary event. */
    public static void recordProjectionAttempt(org.smartregister.domain.Event event) {
        if (event == null || !isServiceType(event.getEventType()) || StringUtils.isBlank(event.getFormSubmissionId())) {
            return;
        }
        try {
            VisitRepository repository = KvpLibrary.getInstance().visitRepository();
            Visit visit = repository.getVisitByFormSubmissionID(event.getFormSubmissionId());
            if (visit != null && visit.getVisitType().equals(event.getEventType())
                    && event.getBaseEntityId() != null && event.getBaseEntityId().equals(visit.getBaseEntityId())) {
                recordProjectionAttempt(repository, visit);
            }
        } catch (Exception exception) {
            Timber.e(exception, "Unable to record the MARP projection attempt");
        }
    }

    static void recordProjectionAttempt(VisitRepository repository, Visit snapshot) throws JSONException {
        SQLiteDatabase database = repository.getWritableDatabase();
        boolean ownsTransaction = !database.inTransaction();
        if (ownsTransaction) {
            database.beginTransactionNonExclusive();
        }
        try {
            Visit current = repository.getVisitByVisitId(database, snapshot.getVisitId());
            if (!sameSubmission(snapshot, current) || !Boolean.TRUE.equals(current.getProcessed())
                    || !matchesEvent(readEvent(database, current.getFormSubmissionId()), current.getFormSubmissionId(),
                    current.getBaseEntityId(), current.getVisitType()) || hasServiceProjection(database, current)) {
                return;
            }
            // Upgrade existing installations without changing the application database version.
            database.execSQL("CREATE TABLE IF NOT EXISTS " + FAILURES
                    + " (visit_id TEXT NOT NULL, submission_id TEXT NOT NULL, PRIMARY KEY (visit_id, submission_id))");
            ContentValues receipt = new ContentValues();
            receipt.put("visit_id", current.getVisitId());
            receipt.put("submission_id", current.getFormSubmissionId());
            database.insertWithOnConflict(FAILURES, null, receipt, SQLiteDatabase.CONFLICT_IGNORE);
            if (ownsTransaction) {
                database.setTransactionSuccessful();
            }
        } finally {
            if (ownsTransaction) {
                database.endTransaction();
            }
        }
    }

    private static boolean hasServiceProjection(SQLiteDatabase database, Visit visit) {
        String table;
        switch (visit.getVisitType()) {
            case KVP_BIO_MEDICAL_SERVICE_VISIT: table = "ec_kvp_bio_medical_services"; break;
            case KVP_BEHAVIORAL_SERVICE_VISIT: table = "ec_kvp_behavioral_services"; break;
            case KVP_STRUCTURAL_SERVICE_VISIT: table = "ec_kvp_structural_services"; break;
            case KVP_OTHER_SERVICE_VISIT: table = "ec_kvp_other_services"; break;
            default: return false;
        }
        if (!exists(database, "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", table)) {
            return false;
        }
        return exists(database, "SELECT 1 FROM " + table + " WHERE base_entity_id = ? AND entity_id = ?",
                visit.getFormSubmissionId(), visit.getBaseEntityId());
    }

    private static boolean exists(SQLiteDatabase database, String sql, String... arguments) {
        try (Cursor cursor = database.rawQuery(sql, arguments)) {
            return cursor.moveToFirst();
        }
    }
}
