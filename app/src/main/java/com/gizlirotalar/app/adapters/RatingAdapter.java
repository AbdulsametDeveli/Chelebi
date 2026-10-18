package com.gizlirotalar.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.models.Rating;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.RatingViewHolder> {
    private List<Rating> ratingList;

    public RatingAdapter(List<Rating> ratingList) {
        this.ratingList = ratingList;
    }

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rating, parent, false);
        return new RatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
        Rating rating = ratingList.get(position);
        holder.tvUserName.setText(rating.getUserName());
        holder.ratingBar.setRating(rating.getRating());
        
        if (rating.getComment() != null && !rating.getComment().isEmpty()) {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(rating.getComment());
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("tr", "TR"));
        holder.tvDate.setText(sdf.format(new Date(rating.getCreatedAt())));
    }

    @Override
    public int getItemCount() { return ratingList.size(); }

    public static class RatingViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvComment, tvDate;
        RatingBar ratingBar;

        public RatingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvRatingUserName);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
            tvComment = itemView.findViewById(R.id.tvRatingComment);
            tvDate = itemView.findViewById(R.id.tvRatingDate);
        }
    }
}
