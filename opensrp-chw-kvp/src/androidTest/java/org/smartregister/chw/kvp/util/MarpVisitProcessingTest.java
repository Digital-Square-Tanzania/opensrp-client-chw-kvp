package org.smartregister.chw.kvp.util;

import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.Gson;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.ENQUEUE_INCOMPLETE;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.LOCAL_SUCCESS;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.NOT_ENQUEUED;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.PROJECTION_FAILED;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.QUEUED;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.STALE_OR_INVALID;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.UNVERIFIED;

@RunWith(AndroidJUnit4.class)
public class MarpVisitProcessingTest {

    private static final Gson GSON = new Gson();
    private static final String CLIENT = "synthetic-marp-client";
    private static final String VISIT = "synthetic-visit";
    private static final String SUBMISSION = "synthetic-submission";
    private SQLiteDatabase database;
    private VisitRepository repository;

    @Before
    public void setUp() {
        System.loadLibrary("sqlcipher");
        database = SQLiteDatabase.openDatabase(":memory:", "", null, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        VisitRepository.createTable(database);
        database.execSQL(VisitRepository.ADD_VISIT_GROUP_COLUMN);
        database.execSQL("CREATE TABLE event (formSubmissionId TEXT, baseEntityId TEXT,"
                + " eventType TEXT, syncStatus TEXT, json TEXT)");
        database.execSQL("CREATE TABLE ec_kvp_bio_medical_services (base_entity_id TEXT, entity_id TEXT)");
        database.execSQL("CREATE TABLE ec_kvp_structural_services (base_entity_id TEXT, entity_id TEXT)");
        repository = new VisitRepository() {
            @Override
            public SQLiteDatabase getWritableDatabase() {
                return database;
            }

            @Override
            public SQLiteDatabase getReadableDatabase() {
                return database;
            }
        };
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void manualAndScheduledSnapshotsCanClaimTheSameOlderSubmissionOnlyOnce() throws Exception {
        Visit card = saveCard(KVP_STRUCTURAL_SERVICE_VISIT, SUBMISSION, daysAgo(3), false);
        Visit scheduledSnapshot = repository.getVisitByVisitId(VISIT);
        CountDownLatch manualWriting = new CountDownLatch(1);
        CountDownLatch scheduledStarting = new CountDownLatch(1);
        CountDownLatch finishManualWrite = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> manual = workers.submit(() -> MarpVisitProcessing.enqueue(repository, card,
                    "manual", current -> true, event -> {
                        manualWriting.countDown();
                        assertTrue(finishManualWrite.await(5, TimeUnit.SECONDS));
                        writeEvent(event);
                    }));
            assertTrue(manualWriting.await(5, TimeUnit.SECONDS));
            Future<Boolean> scheduled = workers.submit(() -> {
                scheduledStarting.countDown();
                return MarpVisitProcessing.enqueue(repository, scheduledSnapshot, "scheduled",
                        KvpVisitsUtil::isReadyForScheduledProcessing, event -> fail("Claim replayed a write"));
            });
            assertTrue(scheduledStarting.await(5, TimeUnit.SECONDS));
            finishManualWrite.countDown();
            assertTrue(manual.get(5, TimeUnit.SECONDS));
            assertFalse(scheduled.get(5, TimeUnit.SECONDS));
            assertEquals(1, eventCount());
        } finally {
            finishManualWrite.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void retainedVisitIdCannotAuthorizeAReplacementOrWrongServiceDay() throws Exception {
        Visit first = saveCard(KVP_STRUCTURAL_SERVICE_VISIT, SUBMISSION, daysAgo(3), false);
        database.delete("visits", "visit_id = ?", new String[]{VISIT});
        Visit replacement = saveCard(KVP_STRUCTURAL_SERVICE_VISIT, "replacement", new Date(), false);
        assertFalse(enqueue(first));
        assertEquals(STALE_OR_INVALID, MarpVisitProcessing.getLocalOutcome(repository, first));
        Visit wrongDay = repository.getVisitByVisitId(VISIT);
        wrongDay.setDate(daysAgo(1));
        assertFalse(enqueue(wrongDay));
        assertFalse(KvpVisitsUtil.isReadyForScheduledProcessing(replacement));
        assertEquals(0, eventCount());
        assertTrue(enqueue(replacement));
    }

    @Test
    public void manualQueueCanProcessTodayAndScheduledQueueRetainsItsTwoDayThreshold() throws Exception {
        Visit card = saveCard(KVP_STRUCTURAL_SERVICE_VISIT, SUBMISSION, new Date(), false);
        card.setUpdatedAt(daysAgo(1));
        assertFalse(KvpVisitsUtil.isReadyForScheduledProcessing(card));
        card.setUpdatedAt(daysAgo(3));
        assertTrue(KvpVisitsUtil.isReadyForScheduledProcessing(card));
        assertFalse(MarpVisitProcessing.enqueue(repository, card, "scheduled",
                KvpVisitsUtil::isReadyForScheduledProcessing, this::writeEvent));
        assertTrue(enqueue(card));
    }

    @Test
    public void primaryAndFollowUpFailuresRetainStagingForRetry() throws Exception {
        Visit card = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, SUBMISSION, new Date(), true);
        assertMissingWriteKeepsStaging(card, SUBMISSION);
        assertMissingWriteKeepsStaging(card, MarpVisitProcessing.followUps(card).get(1).getFormSubmissionId());
        assertTrue(enqueue(card));
        assertEquals(4, eventCount());
        assertEquals("", repository.getVisitByVisitId(VISIT).getPreProcessedJson());
        assertTrue(MarpVisitProcessing.hasUnprocessedPrimary(repository));
    }

    @Test
    public void partialQueueResumesWithStableDistinctFollowUpsAndPreservesPrimary() throws Exception {
        Visit card = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, SUBMISSION, new Date(), true);
        Event primary = GSON.fromJson(card.getJson(), Event.class);
        List<Event> followUps = MarpVisitProcessing.followUps(card);
        List<String> existing = Arrays.asList(SUBMISSION, followUps.get(1).getFormSubmissionId());
        writeEvent(primary);
        writeEvent(followUps.get(1));

        assertEquals(ENQUEUE_INCOMPLETE, MarpVisitProcessing.getLocalOutcome(repository, card));
        List<String> writes = new ArrayList<>();
        assertTrue(MarpVisitProcessing.enqueue(repository, card, "group", current -> true, event -> {
            writes.add(event.getFormSubmissionId());
            writeEvent(event);
        }));
        assertEquals(2, writes.size());
        assertFalse(writes.contains(existing.get(0)));
        assertFalse(writes.contains(existing.get(1)));
        assertEquals(4, new HashSet<>(Arrays.asList(existing.get(0), existing.get(1),
                writes.get(0), writes.get(1))).size());
        assertEquals(followUps.get(0).getFormSubmissionId(),
                MarpVisitProcessing.followUps(card).get(0).getFormSubmissionId());
        card.setFormSubmissionId("replacement");
        assertNotEquals(followUps.get(0).getFormSubmissionId(),
                MarpVisitProcessing.followUps(card).get(0).getFormSubmissionId());
        assertEquals(4, eventCount());
    }

    @Test
    public void legacyHivIdCollisionCannotBeTreatedAsThePrimaryEvent() throws Exception {
        Visit card = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, SUBMISSION, new Date(), true);
        Event legacyHiv = KvpVisitsUtil.generateHivRegistrationForPositiveKvpClients(card.getJson(), CLIENT);
        writeEvent(legacyHiv);
        try {
            enqueue(card);
            fail("An HIV event occupied the primary submission ID");
        } catch (IllegalStateException expected) {
            assertEquals(UNVERIFIED, MarpVisitProcessing.getLocalOutcome(repository, card));
            assertFalse(repository.getVisitByVisitId(VISIT).getProcessed());
            assertEquals(1, eventCount());
        }
    }

    @Test
    public void ambiguousLegacyHepatitisLinkIsNeverQueuedAgain() throws Exception {
        Visit card = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, SUBMISSION, new Date(), true);
        Event legacyHepatitis = KvpVisitsUtil.generateHepTestResultsRegistrationEvent(
                VISIT + "-hep_b", CLIENT, "hep_b", "negative", card.getDate());
        legacyHepatitis.setFormSubmissionId("legacy-hepatitis-id");
        writeEvent(legacyHepatitis);
        try {
            enqueue(card);
            fail("The legacy Hepatitis event cannot be attributed to a submission");
        } catch (IllegalStateException expected) {
            assertFalse(repository.getVisitByVisitId(VISIT).getProcessed());
            assertEquals(1, eventCount());
        }
    }

    @Test
    public void projectionRequiresMatchingSubmissionAndClientAndOverridesEarlierFailure() throws Exception {
        Visit card = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, SUBMISSION, new Date(), false);
        assertEquals(NOT_ENQUEUED, MarpVisitProcessing.getLocalOutcome(repository, card));
        assertTrue(enqueue(card));
        database.execSQL("UPDATE event SET syncStatus = ?", new Object[]{BaseRepository.TYPE_Unsynced});
        database.execSQL("INSERT INTO ec_kvp_bio_medical_services VALUES (?, ?)",
                new Object[]{SUBMISSION, "another-client"});
        database.execSQL("INSERT INTO ec_kvp_bio_medical_services VALUES (?, ?)",
                new Object[]{"older-submission", CLIENT});
        assertEquals(QUEUED, MarpVisitProcessing.getLocalOutcome(repository, card));
        MarpVisitProcessing.recordProjectionAttempt(repository, card);
        assertEquals(PROJECTION_FAILED, MarpVisitProcessing.getLocalOutcome(repository, card));
        database.execSQL("INSERT INTO ec_kvp_bio_medical_services VALUES (?, ?)",
                new Object[]{SUBMISSION, CLIENT});
        assertEquals(LOCAL_SUCCESS, MarpVisitProcessing.getLocalOutcome(repository, card));
        assertTrue(card.getJson().contains("client_status"));
        assertFalse(card.getJson().contains("hiv_status"));
        database.delete("visits", "visit_id = ?", new String[]{VISIT});
        Visit replacement = saveCard(KVP_BIO_MEDICAL_SERVICE_VISIT, "replacement", new Date(), false);
        MarpVisitProcessing.recordProjectionAttempt(repository, card);
        assertEquals(STALE_OR_INVALID, MarpVisitProcessing.getLocalOutcome(repository, card));
        assertEquals(NOT_ENQUEUED, MarpVisitProcessing.getLocalOutcome(repository, replacement));
    }

    private void assertMissingWriteKeepsStaging(Visit card, String missingSubmission) throws Exception {
        try {
            MarpVisitProcessing.enqueue(repository, card, "group", current -> true, event -> {
                if (!missingSubmission.equals(event.getFormSubmissionId())) {
                    writeEvent(event);
                }
            });
            fail("An unverified enqueue must fail");
        } catch (IllegalStateException expected) {
            Visit persisted = repository.getVisitByVisitId(VISIT);
            assertFalse(persisted.getProcessed());
            assertEquals(card.getPreProcessedJson(), persisted.getPreProcessedJson());
            assertEquals(NOT_ENQUEUED, MarpVisitProcessing.getLocalOutcome(repository, card));
            assertEquals(0, eventCount());
        }
    }

    private boolean enqueue(Visit visit) throws Exception {
        return MarpVisitProcessing.enqueue(repository, visit, "group", current -> true, this::writeEvent);
    }

    private Visit saveCard(String type, String submission, Date date, boolean withFollowUps) {
        Event event = new Event();
        event.setBaseEntityId(CLIENT);
        event.setFormSubmissionId(submission);
        event.setEventType(type);
        event.addObs(observation(type.equals(KVP_STRUCTURAL_SERVICE_VISIT) ? "gbv_screening" : "client_status",
                "recorded"));
        if (withFollowUps) {
            event.addObs(observation("hiv_status", "positive"));
            for (String test : new String[]{"hep_b", "hep_c"}) {
                event.addObs(observation(test + "_testing", "yes"));
                event.addObs(observation(test + "_result", "negative"));
            }
        }
        String payload = GSON.toJson(event);
        database.execSQL("INSERT INTO visits (visit_id, visit_type, base_entity_id, visit_date, visit_json,"
                        + " pre_processed, form_submission_id, processed, updated_at, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
                new Object[]{VISIT, type, CLIENT, date.getTime(), payload, payload, submission,
                        date.getTime(), date.getTime()});
        return repository.getVisitByVisitId(VISIT);
    }

    private Obs observation(String code, String value) {
        return new Obs().withFieldCode(code).withFormSubmissionField(code).withValue(value);
    }

    private void writeEvent(Event event) {
        database.execSQL("INSERT INTO event VALUES (?, ?, ?, ?, ?)",
                new Object[]{event.getFormSubmissionId(), event.getBaseEntityId(), event.getEventType(),
                        BaseRepository.TYPE_Unprocessed, GSON.toJson(event)});
    }

    private int eventCount() {
        try (Cursor cursor = database.rawQuery("SELECT count(*) FROM event", new String[0])) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    private Date daysAgo(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);
        return calendar.getTime();
    }
}
