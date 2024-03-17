package be.ucll.huurscanapp.ui;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import be.ucll.huurscanapp.R;
import be.ucll.huurscanapp.dto.Item;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemDetailActivity extends AppCompatActivity {

    private Item currentItem; // Assume this is the item passed with intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Get references to UI elements
        TextView tvDetailId = findViewById(R.id.tvDetailId);
        TextView tvDetailName = findViewById(R.id.tvDetailName);
        TextView tvDetailRented = findViewById(R.id.tvDetailRented);
        TextView tvDetailRentDate = findViewById(R.id.tvDetailRentDate);

        Button btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        Button btnEditItem = findViewById(R.id.btnEditItem);
        Button btnDeleteItem = findViewById(R.id.btnDeleteItem);


        // Inside onCreate() method of ItemDetailActivity
        if (getIntent().hasExtra("itemDetails")) {
            currentItem = getIntent().getParcelableExtra("itemDetails");

            // Now populate the views with the item details
            tvDetailId.setText(String.format(Locale.getDefault(), "ID: %d", currentItem.getId()));
            tvDetailName.setText(String.format(Locale.getDefault(), "Name: %s", currentItem.getName()));
            tvDetailRented.setText(String.format(Locale.getDefault(), "Rented: %s", currentItem.isRented() ? "Yes" : "No"));
            tvDetailRentDate.setText(String.format(Locale.getDefault(), "Rented date: %s",currentItem.getRentDate()));
        } else {
            // Handle the case where currentItem is null
            Toast.makeText(this, "Item details not available.", Toast.LENGTH_SHORT).show();
        }

        // Set click listeners for buttons
        btnGenerateQRCode.setOnClickListener(v -> {
            // Generate QR code for the current item ID
            String qrContent = String.valueOf(currentItem.getId());
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400);

                showQRCodeDialog(bitmap); // Show QR code in a dialog
            } catch(Exception e) {
                e.printStackTrace();
            }
        });


        btnEditItem.setOnClickListener(v -> {
            // Navigate to EditItemActivity and pass the current item details
            Intent editIntent = new Intent(ItemDetailActivity.this, EditItemActivity.class);
            editIntent.putExtra("itemDetails", currentItem);
            editIntent.putExtra("itemId", currentItem.getId());
            startActivity(editIntent);
        });

        btnDeleteItem.setOnClickListener(v -> {
            // Show confirmation dialog before deleting the item
            if (currentItem != null) {
                new AlertDialog.Builder(ItemDetailActivity.this)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        // Call deleteItem method if user confirms
                        deleteItem(currentItem.getId());
                        // Navigate back to InfoActivity after deletion
                        Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();// Close the current activity
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            } else {
                Toast.makeText(ItemDetailActivity.this, "Error: Item is not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to delete an item based on its ID
    private void deleteItem(int itemId) {
        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
        String url = "https://10.0.2.2:7046/api/Rentals/" + itemId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "Failed to delete item: " + response.code(), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ItemDetailActivity.this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    // Method to display QR code in a dialog
    private void showQRCodeDialog(Bitmap qrCodeBitmap) {
        final Dialog qrDialog = new Dialog(this);
        qrDialog.setContentView(R.layout.dialog_qr_code);
        qrDialog.setTitle("QR Code");

        ImageView qrImageView = qrDialog.findViewById(R.id.qrCodeImageView);
        Button btnSave = qrDialog.findViewById(R.id.btnSaveQRCode);

        qrImageView.setImageBitmap(qrCodeBitmap);

        btnSave.setOnClickListener(v -> {
            saveQRCodeToGallery(qrCodeBitmap, "QRCode" + System.currentTimeMillis());
            qrDialog.dismiss();
        });

        qrDialog.show();
    }

    // Method to save QR code bitmap to gallery
    private void saveQRCodeToGallery(Bitmap bitmap, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "saved_qr_codes");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            assert uri != null;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                assert outputStream != null;
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                Toast.makeText(this, "QR Code saved to Gallery", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving QR Code to Gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
