package org.smartregister.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.kvp.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class TestRegisterActivity extends AppCompatActivity {
    public final int REQUEST_CODE_GET_JSON = 1234;
    public final String TAG = TestRegisterActivity.class.getCanonicalName();
    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String formName = "test_form";
        //Toolbar toolbar = findViewById(R.id.toolbar)
        try {
            startForm(formName);
        } catch (Exception e) {
            Log.e(TAG,e.getMessage(),e);
        }

    }

    private void startForm(String formName) throws Exception {

        JSONObject jsonForm = getFormFromAssets(formName);
        String currentLocationId = "Tanzania";
        if (jsonForm != null) {
            jsonForm.getJSONObject("metadata").put("encounter_location", currentLocationId);
            Intent intent = new Intent(this, JsonWizardFormActivity.class);
            intent.putExtra("json", jsonForm.toString());

            Form form = new Form();
            form.setName("Test Form");
            form.setWizard(true);
            form.setNextLabel("Next");
            form.setPreviousLabel("Previous");
            form.setSaveLabel("Save");
            form.setHideSaveLabel(true);

            intent.putExtra("form", form);
            startActivityForResult(intent, REQUEST_CODE_GET_JSON);


        }

    }

    private JSONObject getFormFromAssets(String formName) throws Exception {
        InputStream inputStream = getAssets().open("json.form/" + formName + ".json");
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new JSONObject(outputStream.toString("UTF-8"));
        } finally {
            inputStream.close();
        }
    }
}
