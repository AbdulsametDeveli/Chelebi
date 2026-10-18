package com.gizlirotalar.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.models.Spot;

import java.util.List;

public class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.SpotViewHolder> {

    private List<Spot> spotList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(Spot spot, int position);
        void onItemClick(Spot spot);
        default void onEditClick(Spot spot) {} // yeni: düzenle
    }

    public SpotAdapter(List<Spot> spotList, OnItemClickListener listener) {
        this.spotList = spotList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spot, parent, false);
        return new SpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
        Spot spot = spotList.get(position);
        holder.tvName.setText(spot.getName());
        holder.tvCategory.setText(spot.getCategory());

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(spot, position));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(spot));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(spot));
    }

    @Override
    public int getItemCount() {
        return spotList.size();
    }

    public static class SpotViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory;
        ImageButton btnDelete, btnEdit;

        public SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
