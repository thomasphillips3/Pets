package com.thomasphillips3.pets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.amplify.generated.graphql.ListPetsQuery;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private List<ListPetsQuery.Item> data = new ArrayList<>();
    private LayoutInflater inflater;

    MyAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyAdapter.ViewHolder holder, int position) {
        holder.bindData(data.get(position));

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setItems(List<ListPetsQuery.Item> items) {
        data = items;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_name;
        TextView text_description;

        ViewHolder(View itemView) {
            super(itemView);
            text_name = itemView.findViewById(R.id.text_name);
            text_description = itemView.findViewById(R.id.text_description);
        }

        void bindData(ListPetsQuery.Item item) {
            text_name.setText(item.name());
            text_description.setText(item.description());
        }
    }
}
