package be.ucll.huurscanapp.ui;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import be.ucll.huurscanapp.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddItemsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_items);

        // Get references to UI elements
        EditText editTextItemName = findViewById(R.id.editTextItemName);
        Spinner spinnerIsRented = findViewById(R.id.spinnerIsRented);
        Button buttonSubmitItem = findViewById(R.id.buttonSubmitItem);

        // Set on click listener for submit button
        buttonSubmitItem.setOnClickListener(v -> {
            String itemName = editTextItemName.getText().toString();
            boolean isRented = Boolean.parseBoolean(spinnerIsRented.getSelectedItem().toString());
            submitNewItem(itemName, isRented);
        });
    }

    // Function to submit a new item to the server
    private void submitNewItem(String itemName, boolean isRented) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", itemName);
            jsonObject.put("isRented", isRented);

            // Create request body with JSON data
            RequestBody requestBody = RequestBody.Companion.create(
                    jsonObject.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // Use an unsafe OkHttpClient (replace with a safe one in production) TODO: change it
            OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
            Request request = new Request.Builder()
                    .url("https://10.0.2.2:7046/api/Rentals") // URL for submitting rentals
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AddItemsActivity.this, "Error adding item: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AddItemsActivity.this, "Item added successfully", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(getApplicationContext(), InfoActivity.class));
                            finish();
                        });
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}