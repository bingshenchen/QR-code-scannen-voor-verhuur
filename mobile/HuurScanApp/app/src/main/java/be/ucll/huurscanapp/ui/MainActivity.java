package be.ucll.huurscanapp.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import be.ucll.huurscanapp.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> mActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonScanQr = findViewById(R.id.button_scan_qr);
        Button buttonUploadPhoto = findViewById(R.id.button_upload_photo);
        Button buttonConfirm = findViewById(R.id.button_confirm_manual_entry);
        ImageButton btnTurnHome = findViewById(R.id.btn_home_to_home);
        ImageButton btnTurnInfo = findViewById(R.id.btn_home_to_info);
        TextView textViewEntryId = findViewById(R.id.edittext_manual_entry);

        buttonScanQr.setOnClickListener(v -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.setPrompt("Scan a QR Code");
            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            intentIntegrator.initiateScan();
        });

        // Register a callback to handle the result from picking an image from the gallery
        mActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // Convert the selected image URI to a Bitmap, then to a BinaryBitmap to decode the QR code
                        InputStream imageStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                        if (bitmap != null) {
                            int width = bitmap.getWidth(), height = bitmap.getHeight();
                            int[] pixels = new int[width * height];
                            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                            try {
                                // Try to decode the QR code from the bitmap
                                Result result = new MultiFormatReader().decode(binaryBitmap);
                                handleScanResult(result.getText());
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Failed to decode QR Code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        buttonUploadPhoto.setOnClickListener(v -> mActivityResultLauncher.launch("image/*"));

        buttonConfirm.setOnClickListener(v -> {
            try {
                int itemId = Integer.parseInt(textViewEntryId.getText().toString());
                fetchRentalItemDetails(itemId);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid ID entered", Toast.LENGTH_SHORT).show();
            }
        });

        btnTurnHome.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        btnTurnInfo.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), InfoActivity.class)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                assert selectedImage != null;
                InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                int width = bitmap.getWidth(), height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                bitmap.recycle();
                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                Reader reader = new MultiFormatReader();
                Result result = reader.decode(binaryBitmap);
                handleScanResult(result.getText());
            } catch (Exception e) {
                Toast.makeText(this, "Failed to decode QR Code", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    // Handle the result of QR code scanning by fetching rental item details
    private void handleScanResult(String scanResult) {
        int itemId = Integer.parseInt(scanResult);
        fetchRentalItemDetails(itemId);
    }

    // Fetch rental item details from the server using the item ID
    private void fetchRentalItemDetails(int itemId) {
        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
        String url = "https://10.0.2.2:7046/api/Rentals/" + itemId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch item details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    assert response.body() != null;
                    final String responseData = response.body().string();
                    runOnUiThread(() -> processRentalItemResponse(responseData, itemId));
                }
            }
        });
    }


    private void processRentalItemResponse(String responseData, int itemId) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            boolean isRented = jsonObject.getBoolean("isRented");

            if (!isRented) {
                // If the item is not rented, ask the user if they want to rent it
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Rent Item")
                        .setMessage("This item is not rented. Do you want to rent it?")
                        .setPositiveButton("Yes", (dialog, which) -> rentItem(itemId))
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                // If the item has been rented, ask the user if they want to return it
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Item Rented")
                        .setMessage("This item is already rented. Do you want to return?")
                        .setPositiveButton("Yes", (dialog, which) -> returnItem(itemId))
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to parse response", Toast.LENGTH_SHORT).show();
        }
    }

    // Send a request to the server to rent the item
    private void rentItem(int itemId) {
        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
        String url = "https://10.0.2.2:7046/api/Rentals/" + itemId;
        Log.d("NetworkRequest", "Sending rentItem request to URL: " + url);

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", itemId);
            payload.put("isRented", true);
        } catch (JSONException e)
        {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error creating JSON payload: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to rent item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                Log.d("NetworkResponse", "Response body rentItem: " + responseBody);
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to rent item: " + response.code(), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item rented successfully", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Send a request to the server to return the item
    private void returnItem(int itemId) {
        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
        String url = "https://10.0.2.2:7046/api/Rentals/" + itemId;
        Log.d("NetworkRequest", "Sending returnItem request to URL: " + url);

        JSONObject payload = new JSONObject();
        try {
            payload.put("id", itemId);
            payload.put("isRented", false);
        } catch (JSONException e)
        {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error creating JSON payload: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to return item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to return item: " + response.code(), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item returned successfully", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}