package org.smartregister.chw.kvp.model;

import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.chw.kvp.util.Constants.STEP_NINE;
import static org.smartregister.chw.kvp.util.Constants.STEP_ONE;

import android.os.Build;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.kvp.contract.KvpRegisterContract;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.util.JsonFormUtils;

import timber.log.Timber;

public class BaseKvpRegisterModel implements KvpRegisterContract.Model {

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject jsonObject = KvpJsonFormUtils.getFormAsJson(formName);
        KvpJsonFormUtils.getRegistrationForm(jsonObject, entityId, currentLocationId);

        JSONArray fields = jsonObject.getJSONObject(STEP_ONE).getJSONArray(JsonFormConstants.FIELDS);
        JSONObject referralHealthFacilities = JsonFormUtils.getFieldJSONObject(fields, Constants.JSON_FORM_KEY.FACILITY_NAME);
        if (referralHealthFacilities != null) {
            KvpJsonFormUtils.initializeHealthFacilitiesList(referralHealthFacilities);
        }

        return jsonObject;
    }

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId, String gender, int age) throws Exception {
        JSONObject jsonObject = KvpJsonFormUtils.getFormAsJson(formName);
        KvpJsonFormUtils.getRegistrationForm(jsonObject, entityId, currentLocationId);

        JSONArray fields = jsonObject.getJSONObject(STEP_ONE).getJSONArray(JsonFormConstants.FIELDS);
        JSONObject referralHealthFacilities = JsonFormUtils.getFieldJSONObject(fields, Constants.JSON_FORM_KEY.FACILITY_NAME);
        if (referralHealthFacilities != null) {
            KvpJsonFormUtils.initializeHealthFacilitiesList(referralHealthFacilities);
        }

        JSONArray fieldsStep9 = jsonObject.getJSONObject(STEP_NINE).getJSONArray(JsonFormConstants.FIELDS);

        JSONObject clientGroup = JsonFormUtils.getFieldJSONObject(fieldsStep9, Constants.JSON_FORM_KEY.CLIENT_GROUP);

        try {
            if (gender.equalsIgnoreCase("female") && clientGroup != null && age > 24) {
                JSONArray options = clientGroup.getJSONArray(OPTIONS);
                for (int i = 0; i < options.length(); i++) {
                    if (options.getJSONObject(i).getString("key").equalsIgnoreCase("agyw")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            options.remove(i);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        JSONObject global = jsonObject.getJSONObject("global");
        if (global != null && gender != null) {
            global.put("age", age);
            global.put("gender", gender);
        }

        return jsonObject;
    }

}
