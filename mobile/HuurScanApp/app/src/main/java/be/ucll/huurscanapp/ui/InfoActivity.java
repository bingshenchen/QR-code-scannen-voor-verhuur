package be.ucll.huurscanapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import be.ucll.huurscanapp.dto.Item;
import be.ucll.huurscanapp.adapter.ItemAdapter;
import be.ucll.huurscanapp.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class InfoActivity extends AppCompatActivity {
    private ArrayList<Item> items;
    private ItemAdapter itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Get references to UI elements
        ListView lvItems = findViewById(R.id.lvItems);
        items = new ArrayList<>();
        itemsAdapter = new ItemAdapter(this, items);
        lvItems.setAdapter(itemsAdapter);

        ImageButton btnTurnHome = findViewById(R.id.btn_info_to_home);
        ImageButton btnTurnInfo = findViewById(R.id.btn_info_to_info);

        Button btnAddItem = findViewById(R.id.btnAddItem);
        // Set click listener for "Add Item" button
        btnAddItem.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), AddItemsActivity.class)));

        // Set click listener for item clicks in the ListView
        lvItems.setOnItemClickListener((adapterView, view, position, l) -> {
            Item item = items.get(position);
            Intent detailIntent = new Intent(getApplicationContext(), ItemDetailActivity.class);
            detailIntent.putExtra("itemDetails", item);
            startActivity(detailIntent);
        });

        btnTurnHome.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        btnTurnInfo.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), InfoActivity.class)));

        fetchAllItems();
    }

    // Function to fetch all items data from the server
    public void fetchAllItems() {
        OkHttpClient client = HttpClientUtils.getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("https://10.0.2.2:7046/api/Rentals")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(InfoActivity.this, e.getMessage() + "Error fetching data: ", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    final ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String responseData = responseBody.string();
                        Log.d("HTTP GET", "Response data: " + responseData);
                        try {
                            List<Item> itemList = parseItemsFromJson(responseData);
                            runOnUiThread(() -> {
                                items.clear();
                                items.addAll(itemList);
                                itemsAdapter.notifyDataSetChanged();
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("Error","Error parsing JSON data: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(InfoActivity.this, "Error parsing JSON data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    } else {
                        Log.e("Error","Response body is null");
                        runOnUiThread(() -> Toast.makeText(InfoActivity.this, "Response body is null", Toast.LENGTH_LONG).show());
                    }
                }
            }


        });
    }

    // Get references to UI elements
    private List<Item> parseItemsFromJson(String json) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);
        List<Item> itemList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject itemJson = jsonArray.getJSONObject(i);
            Item item = new Item();
            item.setId(itemJson.getInt("id"));
            item.setName(itemJson.getString("name"));
            item.setRented(itemJson.getBoolean("isRented"));

            if (!itemJson.isNull("rentedDate")) {
                String rentedDateStr = itemJson.getString("rentedDate");
                item.setRentDate(rentedDateStr);
            } else {
                item.setRentDate("N/A");
            }

            itemList.add(item);
        }
        return itemList;
    }

}