package com.bank.bayan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactions;
    private DecimalFormat decimalFormat;
    private DateFormat dateFormat;
    private VoiceHelper voiceHelper;
    private AccessibilityHelper accessibilityHelper;

    public TransactionAdapter(Context context, List<Transaction> transactions, VoiceHelper voiceHelper) {
        this.context = context;
        this.transactions = transactions;
        this.voiceHelper = voiceHelper;
        this.accessibilityHelper = new AccessibilityHelper(context);
        this.decimalFormat = new DecimalFormat("#,##0.00");
        this.dateFormat = new SimpleDateFormat("dd MMMM hh:mm a", new Locale("ar", "SA"));
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.tvDescription.setText(transaction.getDescription());
        holder.tvCategory.setText(transaction.getCategory());

        String dateString = transaction.getDate();
        holder.tvDate.setText(dateString);

        String amountText = " " + decimalFormat.format(transaction.getAmount())+" ";
        holder.tvAmount.setText(amountText);

        if (transaction.isIncome()) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green_light));
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.tvAmount.setCompoundDrawableTintList(ContextCompat.getColorStateList(context, R.color.red));
        }

        setTransactionIcon(holder.ivIcon, transaction);

        View iconBackground = holder.itemView.findViewById(R.id.icon_background);
        if (iconBackground != null) {
            setIconBackground(iconBackground, transaction);
        }

        setupTransactionAccessibility(holder, transaction, position);
    }

    private void setupTransactionAccessibility(TransactionViewHolder holder, Transaction transaction, int position) {
        String transactionType = transaction.isIncome() ? "إيراد" : "مصروف";
        String amountDescription = formatAmountForSpeech(transaction.getAmount());
//        Date date = new Date(transaction.getDate());
//        String dateDescription = formatDateForSpeech(date);

        String fullDescription = String.format(
                "المعاملة رقم %d. %s. %s. المبلغ %s. التاريخ %s. الفئة %s",
                position + 1,
                transactionType,
                transaction.getDescription(),
                amountDescription,
                transaction.getDate(),
                transaction.getCategory()
        );

        accessibilityHelper.setContentDescriptionWithAction(
                holder.itemView,
                fullDescription,
                "انقر للاستماع للتفاصيل أو انقر مرتين للمزيد من الخيارات"
        );

        holder.itemView.setOnClickListener(v -> {
            accessibilityHelper.provideHapticFeedback(AccessibilityHelper.HapticFeedbackType.SELECTION);
            readTransactionDetails(transaction, position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            accessibilityHelper.provideHapticFeedback(AccessibilityHelper.HapticFeedbackType.SELECTION);
            showTransactionOptions(transaction, position);
            return true;
        });

        setupComponentAccessibility(holder, transaction);

        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);

//        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
//            if (hasFocus) {
//                accessibilityHelper.provideHapticFeedback(AccessibilityHelper.HapticFeedbackType.NAVIGATION);
//                // Optionally announce when focused
//                if (accessibilityHelper.isScreenReaderEnabled()) {
//                    // Screen reader will handle this automatically
//                } else {
//                    // For voice-only mode, announce focus
//                    voiceHelper.queue("معاملة " + (position + 1) + ": " + transaction.getDescription());
//                }
//            }
//        });
    }

    private void setupComponentAccessibility(TransactionViewHolder holder, Transaction transaction) {
        String iconDescription = getIconDescription(transaction.getCategory());
        accessibilityHelper.setContentDescription(holder.ivIcon, iconDescription);

        accessibilityHelper.setContentDescription(holder.tvDescription,
                "وصف المعاملة: " + transaction.getDescription());

        String amountDescription = formatAmountForSpeech(transaction.getAmount());
        String transactionType = transaction.isIncome() ? "إيراد" : "مصروف";
//        accessibilityHelper.setupCurrencyAccessibility(holder.tvAmount,
//                transaction.getAmount(), transactionType);

//        Date date = new Date(transaction.getDate());
//        String dateDescription = formatDateForSpeech(date);
        accessibilityHelper.setContentDescription(holder.tvDate, "تاريخ المعاملة: " + transaction.getDate());
    }

    private void readTransactionDetails(Transaction transaction, int position) {
        String transactionType = transaction.isIncome() ? "إيراد" : "مصروف";
        String amountDescription = formatAmountForSpeech(transaction.getAmount());
//        Date date = new Date(transaction.getDate());
//        String dateDescription = formatDateForSpeech(date);

        StringBuilder details = new StringBuilder();
        details.append("المعاملة رقم ").append(position + 1).append(". ");
        details.append(transactionType).append(". ");
        details.append(transaction.getDescription()).append(". ");
        details.append("المبلغ ").append(amountDescription).append(". ");
        details.append("تاريخ المعاملة ").append(transaction.getDate()).append(". ");
        details.append("الفئة ").append(transaction.getCategory()).append(". ");

        if (transaction.isIncome()) {
            details.append("هذه معاملة إيراد.");
        } else {
            details.append("هذه معاملة مصروف.");
        }

        voiceHelper.speak(details.toString());
    }

    private void showTransactionOptions(Transaction transaction, int position) {
        voiceHelper.speak("خيارات المعاملة. يمكنك قول: التفاصيل، التكرار، المشاركة، أو الحذف");

        StringBuilder options = new StringBuilder();
        options.append("الخيارات المتاحة للمعاملة ").append(position + 1).append(": ");
        options.append("قول التفاصيل لسماع التفاصيل الكاملة، ");
        options.append("قول التكرار لإنشاء معاملة مشابهة، ");
        options.append("قول المشاركة لمشاركة المعاملة، ");
        options.append("أو قول الحذف لحذف المعاملة");

        voiceHelper.queue(options.toString());
    }

    private String formatAmountForSpeech(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0");
        String formattedAmount = formatter.format(Math.abs(amount));

        if (amount < 0) {
            return "ناقص " + formattedAmount + " ريال سعودي";
        } else {
            return formattedAmount + " ريال سعودي";
        }
    }

    private String formatDateForSpeech(Date date) {
        SimpleDateFormat speechFormat = new SimpleDateFormat("dd MMMM yyyy 'الساعة' hh:mm a", new Locale("ar", "SA"));
        return speechFormat.format(date);
    }

    private String getIconDescription(String category) {
        category = category.toLowerCase();

        if (category.contains("راتب") || category.contains("دخل")) {
            return "أيقونة راتب";
        } else if (category.contains("تحويل")) {
            return "أيقونة تحويل";
        } else if (category.contains("فاتورة") || category.contains("كهرباء")) {
            return "أيقونة فاتورة";
        } else if (category.contains("مشتريات") || category.contains("سوبر ماركت")) {
            return "أيقونة مشتريات";
        } else if (category.contains("وقود") || category.contains("بنزين")) {
            return "أيقونة وقود";
        } else {
            return "أيقونة معاملة مالية";
        }
    }

    private void setTransactionIcon(ImageView iconView, Transaction transaction) {
        int iconResId = R.drawable.ic_transactions; // default icon

        String category = transaction.getCategory().toLowerCase();

        if (category.contains("salary") || category.contains("راتب")) {
            iconResId = R.drawable.cash;
        } else if (category.contains("transfer") || category.contains("تحويل")) {
            iconResId = R.drawable.ic_transfer_new;
        } else if (category.contains("bill") || category.contains("فاتورة")) {
            iconResId = R.drawable.ic_bill_new;
        } else if (category.contains("shopping") || category.contains("مشتريات")) {
            iconResId = R.drawable.ic_shopping;
        }

        iconView.setImageResource(iconResId);
    }

    private void setIconBackground(View backgroundView, Transaction transaction) {
        int backgroundColorResId;

        String category = transaction.getCategory().toLowerCase();

        if (category.contains("salary") || category.contains("راتب")) {
            backgroundColorResId = R.color.green_light;
        } else if (category.contains("transfer") || category.contains("تحويل")) {
            backgroundColorResId = R.color.red;
        } else if (category.contains("bill") || category.contains("فاتورة")) {
            backgroundColorResId = R.color.orange;
        } else if (category.contains("shopping") || category.contains("مشتريات")) {
            backgroundColorResId = R.color.primary_blue;
        } else {
            backgroundColorResId = R.color.gray_text;
        }

        backgroundView.setBackgroundTintList(ContextCompat.getColorStateList(context, backgroundColorResId));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
        notifyDataSetChanged();

        if (voiceHelper != null) {
            String updateMessage = "تم تحديث قائمة المعاملات. عدد المعاملات: " + newTransactions.size();
            voiceHelper.queue(updateMessage);
        }
    }

    public void announceListSummary() {
        if (transactions.isEmpty()) {
            voiceHelper.speak("لا توجد معاملات لعرضها");
            return;
        }

        int incomeCount = 0;
        int expenseCount = 0;

        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                incomeCount++;
            } else {
                expenseCount++;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("قائمة المعاملات تحتوي على ").append(transactions.size()).append(" معاملة. ");
        summary.append(incomeCount).append(" إيراد، ");
        summary.append(expenseCount).append(" مصروف. ");
        summary.append("انقر على أي معاملة للاستماع للتفاصيل");

        voiceHelper.speak(summary.toString());
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvDescription, tvCategory, tvDate, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_transaction_icon);
            tvDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvCategory = itemView.findViewById(R.id.tv_transaction_category);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
        }
    }
}