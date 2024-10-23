package org.smartregister.chw.kvp.util;

import android.os.Build;
import android.util.Log;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.repository.LocationWithTagsRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.FormUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.RequiresApi;
import timber.log.Timber;

import static org.smartregister.chw.kvp.util.Constants.ENCOUNTER_TYPE;
import static org.smartregister.chw.kvp.util.Constants.KVP_VISIT_GROUP;
import static org.smartregister.chw.kvp.util.Constants.STEP_EIGHT;
import static org.smartregister.chw.kvp.util.Constants.STEP_FIVE;
import static org.smartregister.chw.kvp.util.Constants.STEP_FOUR;
import static org.smartregister.chw.kvp.util.Constants.STEP_NINE;
import static org.smartregister.chw.kvp.util.Constants.STEP_ONE;
import static org.smartregister.chw.kvp.util.Constants.STEP_SEVEN;
import static org.smartregister.chw.kvp.util.Constants.STEP_SIX;
import static org.smartregister.chw.kvp.util.Constants.STEP_TEN;
import static org.smartregister.chw.kvp.util.Constants.STEP_THREE;
import static org.smartregister.chw.kvp.util.Constants.STEP_TWO;

public class KvpJsonFormUtils extends org.smartregister.util.JsonFormUtils {
    public static final String METADATA = "metadata";

    public static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {

        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = kvpFormFields(jsonForm);

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = Triple.of(jsonForm != null && fields != null, jsonForm, fields);
        return registrationFormParams;
    }

    public static JSONArray kvpFormFields(JSONObject jsonForm) {
        //TODO: refactor this implementation with a O(logN) complexity
        try {
            JSONArray fields = new JSONArray();
            JSONArray fieldsOne = fields(jsonForm, STEP_ONE);
            JSONArray fieldsTwo = fields(jsonForm, STEP_TWO);
            JSONArray fieldsThree = fields(jsonForm, STEP_THREE);
            JSONArray fieldsFour = fields(jsonForm, STEP_FOUR);
            JSONArray fieldsFive = fields(jsonForm, STEP_FIVE);
            JSONArray fieldsSix = fields(jsonForm, STEP_SIX);
            JSONArray fieldsSeven = fields(jsonForm, STEP_SEVEN);
            JSONArray fieldsEight = fields(jsonForm, STEP_EIGHT);
            JSONArray fieldsNine = fields(jsonForm, STEP_NINE);
            JSONArray fieldsTen = fields(jsonForm, STEP_TEN);

            compileFields(fields, fieldsOne);
            compileFields(fields, fieldsTwo);
            compileFields(fields, fieldsThree);
            compileFields(fields, fieldsFour);
            compileFields(fields, fieldsFive);
            compileFields(fields, fieldsSix);
            compileFields(fields, fieldsSeven);
            compileFields(fields, fieldsEight);
            compileFields(fields, fieldsNine);
            compileFields(fields, fieldsTen);

            return fields;

        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    private static void compileFields(JSONArray compiledFields, JSONArray addedField) throws JSONException {
        if (addedField != null) {
            for (int i = 0; i < addedField.length(); i++) {
                compiledFields.put(addedField.get(i));
            }
        }
    }

    public static JSONArray fields(JSONObject jsonForm, String step) {
        try {

            JSONObject step1 = jsonForm.has(step) ? jsonForm.getJSONObject(step) : null;
            if (step1 == null) {
                return null;
            }

            return step1.has(FIELDS) ? step1.getJSONArray(FIELDS) : null;

        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public static Event processJsonForm(AllSharedPreferences allSharedPreferences, String
            jsonString) {

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

        if (!registrationFormParams.getLeft()) {
            return null;
        }

        JSONObject jsonForm = registrationFormParams.getMiddle();
        JSONArray fields = registrationFormParams.getRight();
        String entityId = getString(jsonForm, ENTITY_ID);
        String encounter_type = jsonForm.optString(Constants.JSON_FORM_EXTRA.ENCOUNTER_TYPE);

        if (Constants.EVENT_TYPE.KVP_PrEP_REGISTRATION.equals(encounter_type)) {
            encounter_type = Constants.TABLES.KVP_PrEP_REGISTER;
        } else if (Constants.EVENT_TYPE.KVP_PrEP_FOLLOW_UP_VISIT.equals(encounter_type)) {
            encounter_type = Constants.TABLES.KVP_PrEP_FOLLOWUP;
        }
        return org.smartregister.util.JsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA), formTag(allSharedPreferences), entityId, getString(jsonForm, ENCOUNTER_TYPE), encounter_type);
    }

    protected static FormTag formTag(AllSharedPreferences allSharedPreferences) {
        FormTag formTag = new FormTag();
        formTag.providerId = allSharedPreferences.fetchRegisteredANM();
        formTag.appVersion = KvpLibrary.getInstance().getApplicationVersion();
        formTag.databaseVersion = KvpLibrary.getInstance().getDatabaseVersion();
        return formTag;
    }

    public static void tagEvent(AllSharedPreferences allSharedPreferences, Event event) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(locationId(allSharedPreferences));

        String syncLocationId = KvpDao.getSyncLocationId(event.getBaseEntityId());
        if (syncLocationId != null) {
            // Allows setting the ID for sync purposes
            event.setLocationId(syncLocationId);
        }

        event.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        event.setClientApplicationVersion(KvpLibrary.getInstance().getApplicationVersion());
        event.setClientDatabaseVersion(KvpLibrary.getInstance().getDatabaseVersion());
    }

    public static String locationId(AllSharedPreferences allSharedPreferences) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        String userLocationId = allSharedPreferences.fetchUserLocalityId(providerId);
        if (StringUtils.isBlank(userLocationId)) {
            userLocationId = allSharedPreferences.fetchDefaultLocalityId(providerId);
        }

        return userLocationId;
    }

    public static void getRegistrationForm(JSONObject jsonObject, String entityId, String
            currentLocationId) throws JSONException {
        jsonObject.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);
        jsonObject.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, entityId);
        jsonObject.put(DBConstants.KEY.RELATIONAL_ID, entityId);
    }

    public static JSONObject getFormAsJson(String formName) throws Exception {
        return FormUtils.getInstance(KvpLibrary.getInstance().context().applicationContext()).getFormJson(formName);
    }

    public static String cleanString(String dirtyString) {
        if (StringUtils.isBlank(dirtyString))
            return "";

        return dirtyString.substring(1, dirtyString.length() - 1);
    }

    public static void populateForm(@Nullable JSONObject jsonObject, Map<String, @Nullable List<VisitDetail>> details) {
        if (details == null || jsonObject == null) return;
        try {
            // x steps
            String count_str = jsonObject.getString(JsonFormConstants.COUNT);

            int step_count = StringUtils.isNotBlank(count_str) ? Integer.valueOf(count_str) : 1;
            while (step_count > 0) {
                JSONArray jsonArray = jsonObject.getJSONObject(MessageFormat.format("step{0}", step_count)).getJSONArray(JsonFormConstants.FIELDS);

                int field_count = jsonArray.length() - 1;
                while (field_count >= 0) {

                    JSONObject jo = jsonArray.getJSONObject(field_count);
                    String key = jo.getString(JsonFormConstants.KEY);
                    List<VisitDetail> detailList = details.get(key);

                    if (detailList != null) {
                        if (jo.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.CHECK_BOX)) {
                            jo.put(JsonFormConstants.VALUE, getValue(jo, detailList));
                        } else {
                            String value = getValue(detailList.get(0));
                            if (key.contains("date")) {
                                value = NCUtil.getFormattedDate(NCUtil.getSaveDateFormat(), NCUtil.getSourceDateFormat(), value);
                            }
                            jo.put(JsonFormConstants.VALUE, value);
                        }
                    }

                    field_count--;
                }

                step_count--;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static String getValue(VisitDetail visitDetail) {
        String humanReadable = visitDetail.getHumanReadable();
        if (StringUtils.isNotBlank(humanReadable))
            return humanReadable;

        return visitDetail.getDetails();
    }

    public static JSONArray getValue(JSONObject jo, List<VisitDetail> visitDetails) throws JSONException {
        JSONArray values = new JSONArray();
        if (jo.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.CHECK_BOX)) {
            JSONArray options = jo.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
            HashMap<String, NameID> valueMap = new HashMap<>();

            int x = options.length() - 1;
            while (x >= 0) {
                JSONObject object = options.getJSONObject(x);
                valueMap.put(object.getString(JsonFormConstants.KEY), new NameID(object.getString(JsonFormConstants.KEY), x));
                x--;
            }

            for (VisitDetail d : visitDetails) {
                String val = getValue(d);
                List<String> checkedList = new ArrayList<>(Arrays.asList(val.split(", ")));
                if (checkedList.size() > 1) {
                    for (String item : checkedList) {
                        NameID nid = valueMap.get(item);
                        if (nid != null) {
                            values.put(nid.name);
                            options.getJSONObject(nid.position).put(JsonFormConstants.VALUE, true);
                        }
                    }
                } else {
                    NameID nid = valueMap.get(val);
                    if (nid != null) {
                        values.put(nid.name);
                        options.getJSONObject(nid.position).put(JsonFormConstants.VALUE, true);
                    }
                }
            }
        } else {
            for (VisitDetail d : visitDetails) {
                String val = getValue(d);
                if (StringUtils.isNotBlank(val)) {
                    values.put(val);
                }
            }
        }
        return values;
    }

    public static Event processVisitJsonForm(AllSharedPreferences allSharedPreferences, String entityId, String encounterType, Map<String, String> jsonStrings, String tableName) {

        // aggregate all the fields into 1 payload
        JSONObject jsonForm = null;
        JSONObject metadata = null;

        List<JSONObject> fields_obj = new ArrayList<>();

        for (Map.Entry<String, String> map : jsonStrings.entrySet()) {
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(map.getValue());

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            if (jsonForm == null) {
                jsonForm = registrationFormParams.getMiddle();
            }

            if (metadata == null) {
                metadata = getJSONObject(jsonForm, METADATA);
            }

            // add all the fields to the event while injecting a new variable for grouping
            JSONArray local_fields = registrationFormParams.getRight();
            int x = 0;
            while (local_fields.length() > x) {
                try {
                    JSONObject obj = local_fields.getJSONObject(x);
                    obj.put(KVP_VISIT_GROUP, map.getKey());
                    fields_obj.add(obj);
                } catch (JSONException e) {
                    Timber.e(e);
                }
                x++;
            }
        }

        if (metadata == null) {
            metadata = new JSONObject();
        }

        JSONArray fields = new JSONArray(fields_obj);
        String derivedEncounterType = StringUtils.isBlank(encounterType) && jsonForm != null ? getString(jsonForm, ENCOUNTER_TYPE) : encounterType;

        return org.smartregister.util.JsonFormUtils.createEvent(fields, metadata, formTag(allSharedPreferences), entityId, derivedEncounterType, tableName);
    }

    private static class NameID {
        private String name;
        private int position;

        public NameID(String name, int position) {
            this.name = name;
            this.position = position;
        }
    }

    public static void initializeHealthFacilitiesList(JSONObject referralHealthFacilities) {
        LocationWithTagsRepository locationRepository = new LocationWithTagsRepository();
        List<Location> locations = locationRepository.getAllLocationsWithTags();
        if (locations != null && referralHealthFacilities != null) {

            try {
                JSONArray options = referralHealthFacilities.getJSONArray("options");
                String healthFacilityWithMsdCodeTagName = "Facility";
                for (Location location : locations) {
                    Set<LocationTag> locationTags = location.getLocationTags();
                    if (locationTags.iterator().next().getName().equalsIgnoreCase(healthFacilityWithMsdCodeTagName)) {
                        JSONObject optionNode = new JSONObject();
                        optionNode.put("text", StringUtils.capitalize(location.getProperties().getName()));
                        optionNode.put("key", StringUtils.capitalize(location.getProperties().getName()));
                        JSONObject propertyObject = new JSONObject();
                        propertyObject.put("presumed-id", location.getProperties().getUid());
                        propertyObject.put("confirmed-id", location.getProperties().getUid());
                        optionNode.put("property", propertyObject);

                        options.put(optionNode);
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void removeOptionFromCheckboxListWithKey(JSONObject formQuestion, String key) throws Exception {
        //from the form question, get a jsonArray of options
        JSONArray options = formQuestion.getJSONArray("options");
        // from the options look for the option with the key that matches
        int optionsLength = options.length();
        int iterator;
        boolean found = false;
        int foundIndex = 0;
        for (iterator = 0; iterator < optionsLength; iterator++) {
            JSONObject option = options.getJSONObject(iterator);
            // when you get the option, take it's position and set found to true
            if (option.getString("key").equalsIgnoreCase(key)) {
                found = true;
                foundIndex = iterator;
                break;
            }
        }
        // if found, delete the option at that index
        if (found) {
            options.remove(foundIndex);
        }
    }

}
