package be.ucll.huurscanapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import be.ucll.huurscanapp.R;
import be.ucll.huurscanapp.dto.Item;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditItemActivity extends AppCompatActivity {

    private EditText editItemName;
    private CheckBox checkBoxIsRented;
    private int itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        // Get references to UI elements
        editItemName = findViewById(R.id.editItemName);
        checkBoxIsRented = findViewById(R.id.checkBoxIsRented);
        Button buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        // Retrieve item details from the intent
        Item currentItem = getIntent().getParcelableExtra("itemDetails");
        itemId = getIntent().getIntExtra("itemId", -1);

        if (currentItem != null) {
            // Pre-populate edit fields with existing item data
            editItemName.setText(currentItem.getName());
            checkBoxIsRented.setChecked(currentItem.isRented());
        }

        buttonSaveChanges.setOnClickListener(v -> saveChanges());


    }
    private void saveChanges() {
        // Collect data from the EditText fields
        String name = editItemName.getText().toString();
        boolean isRented = checkBoxIsRented.isChecked();

        // Create JSON payload for the update request
        JSONObject payload = new JSONObject();
        try {
            payload.put("id", itemId);
            payload.put("name", name);
            payload.put("isRented", isRented);
        } catch (JSONException e)
        {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(EditItemActivity.this, "Error creating JSON payload: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }


        MediaType JSON = MediaType.get("application/json; charset=utf-8");


        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();        // TODO: Change it
        String url = "https://10.0.2.2:7046/api/Rentals/" + itemId;

        Log.d("NetworkRequest", "Sending request to URL: " + url);
        Log.d("NetworkRequest", "Request payload: " + payload);

        // Build PUT request to update the rental item
        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EditItemActivity", "Error saving changes: ", e);
                runOnUiThread(() -> Toast.makeText(EditItemActivity.this, "Error saving changes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                Log.d("EditItemActivity", "Response: " + responseBody);
                Log.d("NetworkResponse", "Received response with status code: " + response.code());
                Log.d("NetworkResponse", "Response body: " + responseBody);
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response + ", body: " + responseBody);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EditItemActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(new Intent(getApplicationContext(), InfoActivity.class));
                    });
                }
            }
        });
    }

}