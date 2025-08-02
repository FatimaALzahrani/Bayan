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

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<TransactionActivity.Contact> favoritesList;
    private OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(TransactionActivity.Contact contact);
    }

    public FavoritesAdapter(List<TransactionActivity.Contact> favoritesList, OnFavoriteClickListener listener) {
        this.favoritesList = favoritesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        TransactionActivity.Contact contact = favoritesList.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImageView;
        private TextView nameTextView;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFavoriteClick(favoritesList.get(position));
                }
            });
        }

        public void bind(TransactionActivity.Contact contact) {
            nameTextView.setText(contact.getName());

            itemView.setContentDescription("جهة اتصال مفضلة: " + contact.getName() + ". اضغط للتحويل");
            itemView.setFocusable(true);
            itemView.setClickable(true);

            int position = getAdapterPosition();
            int[] colors = {R.color.blue_avatar, R.color.teal_avatar, R.color.orange_avatar, R.color.green_avatar};
            avatarImageView.setBackgroundResource(colors[position % colors.length]);
        }
    }
}