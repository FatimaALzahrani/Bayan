package com.bank.bayan.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bank.bayan.R;

import java.util.List;

public class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.TransferViewHolder> {

    private List<TransactionActivity.Contact> transferList;
    private OnTransferClickListener listener;

    public interface OnTransferClickListener {
        void onTransferClick(TransactionActivity.Contact contact);
    }

    public TransferAdapter(List<TransactionActivity.Contact> transferList, OnTransferClickListener listener) {
        this.transferList = transferList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transfer, parent, false);
        return new TransferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransferViewHolder holder, int position) {
        TransactionActivity.Contact contact = transferList.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return transferList.size();
    }

    class TransferViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImageView;
        private TextView nameTextView;
        private TextView accountNumberTextView;
        private ImageView arrowImageView;

        public TransferViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            accountNumberTextView = itemView.findViewById(R.id.accountNumberTextView);
            arrowImageView = itemView.findViewById(R.id.arrowImageView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransferClick(transferList.get(position));
                }
            });
        }

        public void bind(TransactionActivity.Contact contact) {
            nameTextView.setText(contact.getName());
            accountNumberTextView.setText(contact.getAccountNumber());

            String contentDesc = "جهة اتصال: " + contact.getName() +
                    ", رقم الحساب: " + contact.getAccountNumber() +
                    ". اضغط للتحويل";
            itemView.setContentDescription(contentDesc);
            itemView.setFocusable(true);
            itemView.setClickable(true);

            int position = getAdapterPosition();
            int[] colors = {R.color.orange_avatar, R.color.blue_avatar, R.color.teal_avatar};
            avatarImageView.setBackgroundResource(colors[position % colors.length]);
        }
    }
}