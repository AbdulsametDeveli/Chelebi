package com.gizlirotalar.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gizlirotalar.app.R;
import com.gizlirotalar.app.models.Report;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<Report> reportList;
    private OnReportActionListener listener;

    public interface OnReportActionListener {
        void onApprove(Report report, int position);
        void onReject(Report report, int position);
    }

    public ReportAdapter(List<Report> reportList, OnReportActionListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.tvSpotName.setText(report.getSpotName());
        holder.tvReason.setText("Sebep: " + report.getReason());
        holder.tvReporter.setText("Bildiren: " + report.getUserName());
        
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(report.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }
        
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(report, position));
        holder.btnReject.setOnClickListener(v -> listener.onReject(report, position));
    }

    @Override
    public int getItemCount() { return reportList.size(); }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpotName, tvReason, tvReporter, tvDescription;
        Button btnApprove, btnReject;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpotName = itemView.findViewById(R.id.tvReportSpotName);
            tvReason = itemView.findViewById(R.id.tvReportReason);
            tvReporter = itemView.findViewById(R.id.tvReportReporter);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
            btnApprove = itemView.findViewById(R.id.btnReportApprove);
            btnReject = itemView.findViewById(R.id.btnReportReject);
        }
    }
}
