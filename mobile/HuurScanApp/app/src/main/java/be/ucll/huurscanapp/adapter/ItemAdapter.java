package be.ucll.huurscanapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import be.ucll.huurscanapp.R;
import be.ucll.huurscanapp.dto.Item;
import be.ucll.huurscanapp.ui.ItemDetailActivity;

public class ItemAdapter extends ArrayAdapter<Item> {
    public ItemAdapter(Context context, List<Item> items) {
        super(context, 0, items);
    }

    @NotNull
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        // Get the data item for this position
        Item item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_layout, parent, false);
        }
        // Lookup view for data population
        TextView tvId = convertView.findViewById(R.id.tvId);
        TextView tvName = convertView.findViewById(R.id.tvName);
        Button btnDetails = convertView.findViewById(R.id.btnToDetails);
        // Populate the data into the template view using the data object

        if (item != null) {
            tvId.setText(String.valueOf(item.getId()));
            tvName.setText(item.getName());
        } else {
            // Handle the case where the item is null (e.g., log an error)
            Log.e("ItemAdapter", "Item at position " + position + " is null!");
        }

        // Attach the click event handler for the details button
        btnDetails.setOnClickListener(v -> {
            // TODO: Implement the click handler
            Intent detailIntent = new Intent(getContext(), ItemDetailActivity.class);
            detailIntent.putExtra("itemDetails", item); // Make sure Item class implements Parcelable
            getContext().startActivity(detailIntent);
        });

        // Return the completed view to render on screen
        return convertView;
    }
}