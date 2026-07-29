package org.smartregister.chw.kvp.util;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.smartregister.chw.kvp.domain.VisitDetail;

import java.util.Collections;

public class KvpJsonFormUtilsTest {

    @Test
    public void getValueRestoresCheckboxFromStoredKeyInsteadOfHumanReadableText() throws Exception {
        JSONObject field = checkboxField(
                option("rch", "RCH"),
                option("none", "None")
        );
        VisitDetail detail = visitDetail("none", "None");

        JSONArray values = KvpJsonFormUtils.getValue(field, Collections.singletonList(detail));

        Assert.assertEquals("[\"none\"]", values.toString());
        Assert.assertFalse(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(0).optBoolean(JsonFormConstants.VALUE));
        Assert.assertTrue(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(1).getBoolean(JsonFormConstants.VALUE));
    }

    @Test
    public void getValueRestoresCaseInsensitiveCheckboxKey() throws Exception {
        JSONObject field = checkboxField(option("rch", "RCH"));
        VisitDetail detail = visitDetail("RCH", "RCH");

        JSONArray values = KvpJsonFormUtils.getValue(field, Collections.singletonList(detail));

        Assert.assertEquals("[\"rch\"]", values.toString());
        Assert.assertTrue(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(0).getBoolean(JsonFormConstants.VALUE));
    }

    @Test
    public void getValueRestoresMultipleCheckboxKeys() throws Exception {
        JSONObject field = checkboxField(
                option("rch", "RCH"),
                option("mental_health", "Mental health"),
                option("none", "None")
        );
        VisitDetail detail = visitDetail("rch, mental_health", "RCH, Mental health");

        JSONArray values = KvpJsonFormUtils.getValue(field, Collections.singletonList(detail));

        Assert.assertEquals("[\"rch\",\"mental_health\"]", values.toString());
        Assert.assertTrue(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(0).getBoolean(JsonFormConstants.VALUE));
        Assert.assertTrue(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(1).getBoolean(JsonFormConstants.VALUE));
        Assert.assertFalse(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(2).optBoolean(JsonFormConstants.VALUE));
    }

    @Test
    public void getValueFallsBackToHumanReadableTextForHistoricalDetails() throws Exception {
        JSONObject field = checkboxField(option("mental_health", "Mental health"));
        VisitDetail detail = visitDetail("", "MENTAL HEALTH");

        JSONArray values = KvpJsonFormUtils.getValue(field, Collections.singletonList(detail));

        Assert.assertEquals("[\"mental_health\"]", values.toString());
        Assert.assertTrue(field.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME).getJSONObject(0).getBoolean(JsonFormConstants.VALUE));
    }

    private JSONObject checkboxField(JSONObject... options) throws Exception {
        return new JSONObject()
                .put(JsonFormConstants.TYPE, JsonFormConstants.CHECK_BOX)
                .put(JsonFormConstants.OPTIONS_FIELD_NAME, new JSONArray(options));
    }

    private JSONObject option(String key, String text) throws Exception {
        return new JSONObject()
                .put(JsonFormConstants.KEY, key)
                .put(JsonFormConstants.TEXT, text)
                .put(JsonFormConstants.VALUE, false);
    }

    private VisitDetail visitDetail(String details, String humanReadable) {
        VisitDetail visitDetail = new VisitDetail();
        visitDetail.setDetails(details);
        visitDetail.setHumanReadable(humanReadable);
        return visitDetail;
    }
}
