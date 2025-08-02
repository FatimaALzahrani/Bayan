package com.bank.bayan.bill;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bank.bayan.R;

import java.util.List;

public class BillTypesAdapter extends RecyclerView.Adapter<BillTypesAdapter.BillTypeViewHolder> {

    private List<BillPaymentActivity.BillType> billTypesList;
    private OnBillTypeClickListener listener;

    public interface OnBillTypeClickListener {
        void onBillTypeClick(BillPaymentActivity.BillType billType);
    }

    public BillTypesAdapter(List<BillPaymentActivity.BillType> billTypesList, OnBillTypeClickListener listener) {
        this.billTypesList = billTypesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BillTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill_type, parent, false);
        return new BillTypeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillTypeViewHolder holder, int position) {
        BillPaymentActivity.BillType billType = billTypesList.get(position);
        holder.bind(billType);
    }

    @Override
    public int getItemCount() {
        return billTypesList.size();
    }

    class BillTypeViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView nameTextView;
        private TextView companyTextView;

        public BillTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            companyTextView = itemView.findViewById(R.id.companyTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBillTypeClick(billTypesList.get(position));
                }
            });
        }

        public void bind(BillPaymentActivity.BillType billType) {
            nameTextView.setText(billType.getName());
            companyTextView.setText(billType.getCompany());

            if (billType.getIconResource() != 0) {
                iconImageView.setImageResource(billType.getIconResource());
            } else {
                iconImageView.setImageResource(R.drawable.ic_transfer);
            }

            itemView.setContentDescription("نوع فاتورة: " + billType.getName() +
                    " من " + billType.getCompany() + ". اضغط للسداد");
            itemView.setFocusable(true);
            itemView.setClickable(true);

            int position = getAdapterPosition();
            int[] colors = {R.color.blue_avatar, R.color.teal_avatar, R.color.orange_avatar, 
                           R.color.green_avatar, R.color.purple_avatar, R.color.red_avatar};
            iconImageView.setBackgroundResource(R.drawable.circle_background);
            iconImageView.setBackgroundTintList(itemView.getContext().getColorStateList(colors[position % colors.length]));
        }
    }
}