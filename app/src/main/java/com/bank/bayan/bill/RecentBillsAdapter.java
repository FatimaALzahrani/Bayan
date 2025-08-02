package com.bank.bayan.bill;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bank.bayan.R;

import java.text.DecimalFormat;
import java.util.List;

public class RecentBillsAdapter extends RecyclerView.Adapter<RecentBillsAdapter.RecentBillViewHolder> {

    private List<Bill> recentBillsList;
    private OnRecentBillClickListener listener;
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public interface OnRecentBillClickListener {
        void onRecentBillClick(Bill bill);
    }

    public RecentBillsAdapter(List<Bill> recentBillsList, OnRecentBillClickListener listener) {
        this.recentBillsList = recentBillsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentBillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_bill, parent, false);
        return new RecentBillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentBillViewHolder holder, int position) {
        Bill bill = recentBillsList.get(position);
        holder.bind(bill);
    }

    @Override
    public int getItemCount() {
        return recentBillsList.size();
    }

    class RecentBillViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView serviceProviderTextView;
        private TextView accountNumberTextView;
        private TextView amountTextView;
        private TextView dueDateTextView;
        private TextView statusTextView;
        private ImageView arrowImageView;

        public RecentBillViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            serviceProviderTextView = itemView.findViewById(R.id.serviceProviderTextView);
            accountNumberTextView = itemView.findViewById(R.id.accountNumberTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            arrowImageView = itemView.findViewById(R.id.arrowImageView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRecentBillClick(recentBillsList.get(position));
                }
            });
        }

        public void bind(Bill bill) {
            serviceProviderTextView.setText(bill.getServiceProvider());
            accountNumberTextView.setText(bill.getAccountNumber());
            amountTextView.setText(decimalFormat.format(bill.getAmount()) + " ريال");
            dueDateTextView.setText("تاريخ الاستحقاق: " + bill.getDueDate());
            
            statusTextView.setText(bill.getStatus());
            if ("مستحقة".equals(bill.getStatus())) {
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.red));
            } else if ("مدفوعة".equals(bill.getStatus())) {
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.success_color));
            } else {
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.orange_yellow));
            }

            String contentDesc = "فاتورة " + bill.getServiceProvider() +
                    ", رقم الحساب: " + bill.getAccountNumber() +
                    ", المبلغ: " + decimalFormat.format(bill.getAmount()) + " ريال" +
                    ", تاريخ الاستحقاق: " + bill.getDueDate() +
                    ", الحالة: " + bill.getStatus() +
                    ". اضغط للسداد";
            itemView.setContentDescription(contentDesc);
            itemView.setFocusable(true);
            itemView.setClickable(true);

            int position = getAdapterPosition();
            int[] colors = {R.color.orange_avatar, R.color.blue_avatar, R.color.teal_avatar, 
                           R.color.green_avatar, R.color.purple_avatar};
            
            setServiceIcon(bill.getServiceProvider());
            iconImageView.setBackgroundResource(R.drawable.circle_background);
            iconImageView.setBackgroundTintList(itemView.getContext().getColorStateList(colors[position % colors.length]));
        }

        private void setServiceIcon(String serviceProvider) {
            String provider = serviceProvider.toLowerCase();
            
            if (provider.contains("كهرباء") || provider.contains("سعودية للكهرباء")) {
                iconImageView.setImageResource(R.drawable.ic_electricity);
            } else if (provider.contains("مياه") || provider.contains("المياه الوطنية")) {
                iconImageView.setImageResource(R.drawable.ic_water);
            } else if (provider.contains("اتصالات") || provider.contains("هاتف") || provider.contains("موبايلي") || provider.contains("زين")) {
                iconImageView.setImageResource(R.drawable.ic_phone);
            } else if (provider.contains("إنترنت") || provider.contains("انترنت") || provider.contains("فايبر")) {
                iconImageView.setImageResource(R.drawable.ic_internet);
            } else if (provider.contains("غاز") || provider.contains("الغاز والتصنيع")) {
                iconImageView.setImageResource(R.drawable.ic_gas);
            } else if (provider.contains("تأمين")) {
                iconImageView.setImageResource(R.drawable.baseline_shield_24);
            } else {
                iconImageView.setImageResource(R.drawable.ic_transfer);
            }
        }
    }
}