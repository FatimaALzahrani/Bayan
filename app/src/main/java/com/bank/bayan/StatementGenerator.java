package com.bank.bayan;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatementGenerator {
    
    private static final String TAG = "StatementGenerator";
    private Context context;
    private String userId;
    private DatabaseReference databaseRef;
    
    public interface StatementCallback {
        void onStatementGenerated(String filePath);
        void onError(String error);
    }

    public StatementGenerator(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    public void generateStatement(StatementCallback callback) {
        generateMonthlyStatement(getCurrentMonth(), callback);
    }

    public void generateStatement(String month, StatementCallback callback) {
        generateMonthlyStatement(month, callback);
    }

    private void generateMonthlyStatement(String month, StatementCallback callback) {
        databaseRef.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists()) {
                    callback.onError("بيانات المستخدم غير موجودة");
                    return;
                }

                String userName = userSnapshot.child("name").getValue(String.class);
                String accountNumber = userSnapshot.child("accountNumber").getValue(String.class);
                Double balance = userSnapshot.child("balance").getValue(Double.class);

                getTransactionsForMonth(month, userName, accountNumber, balance != null ? balance : 0.0, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل بيانات المستخدم: " + error.getMessage());
            }
        });
    }

    private void getTransactionsForMonth(String month, String userName, String accountNumber, 
                                       double currentBalance, StatementCallback callback) {
        
        databaseRef.child("transactions").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot transactionsSnapshot) {
                List<Transaction> transactions = new ArrayList<>();
                double totalIncome = 0.0;
                double totalExpenses = 0.0;

                for (DataSnapshot txnSnapshot : transactionsSnapshot.getChildren()) {
                    Transaction transaction = txnSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        if (isTransactionInMonth(transaction.getDate(), month)) {
                            transaction.setId(txnSnapshot.getKey());
                            transactions.add(transaction);

                            if ("income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else if ("expense".equals(transaction.getType())) {
                                totalExpenses += Math.abs(transaction.getAmount());
                            }
                        }
                    }
                }

                generateStatementFile(userName, accountNumber, currentBalance, transactions,
                                    totalIncome, totalExpenses, month, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل المعاملات: " + error.getMessage());
            }
        });
    }

    private void generateStatementFile(String userName, String accountNumber, double currentBalance,
                                     List<Transaction> transactions, double totalIncome, 
                                     double totalExpenses, String month, StatementCallback callback) {
        
        try {
            File statementDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "statements");
            if (!statementDir.exists()) {
                statementDir.mkdirs();
            }

            String fileName = "statement_" + month + "_" + System.currentTimeMillis() + ".txt";
            File statementFile = new File(statementDir, fileName);

            FileWriter writer = new FileWriter(statementFile);
            
            writeStatementHeader(writer, userName, accountNumber, month);
            
            writeStatementSummary(writer, currentBalance, totalIncome, totalExpenses, transactions.size());
            
            writeTransactionsList(writer, transactions);
            
            writeStatementFooter(writer);
            
            writer.close();

            saveStatementToFirebase(month, statementFile.getAbsolutePath());

            callback.onStatementGenerated(statementFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Error generating statement file", e);
            callback.onError("خطأ في إنشاء ملف كشف الحساب: " + e.getMessage());
        }
    }

    private void writeStatementHeader(FileWriter writer, String userName, String accountNumber, String month) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar", "SA"));
        String currentDate = dateFormat.format(new Date());

        writer.write("=====================================\n");
        writer.write("           كشف حساب بنكي\n");
        writer.write("=====================================\n\n");
        writer.write("اسم العميل: " + (userName != null ? userName : "غير محدد") + "\n");
        writer.write("رقم الحساب: " + (accountNumber != null ? accountNumber : "غير محدد") + "\n");
        writer.write("فترة الكشف: " + getMonthNameInArabic(month) + "\n");
        writer.write("تاريخ الإصدار: " + currentDate + "\n");
        writer.write("=====================================\n\n");
    }

    private void writeStatementSummary(FileWriter writer, double currentBalance, double totalIncome, 
                                     double totalExpenses, int transactionCount) throws IOException {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        
        writer.write("ملخص الحساب:\n");
        writer.write("-------------\n");
        writer.write("الرصيد الحالي: " + df.format(currentBalance) + " ريال سعودي\n");
        writer.write("إجمالي الإيرادات: " + df.format(totalIncome) + " ريال سعودي\n");
        writer.write("إجمالي المصروفات: " + df.format(totalExpenses) + " ريال سعودي\n");
        writer.write("صافي التغيير: " + df.format(totalIncome - totalExpenses) + " ريال سعودي\n");
        writer.write("عدد المعاملات: " + transactionCount + "\n\n");
    }

    private void writeTransactionsList(FileWriter writer, List<Transaction> transactions) throws IOException {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        
        writer.write("تفاصيل المعاملات:\n");
        writer.write("==================\n\n");

        if (transactions.isEmpty()) {
            writer.write("لا توجد معاملات في هذه الفترة.\n\n");
            return;
        }

        transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

        int counter = 1;
        for (Transaction transaction : transactions) {
            writer.write(counter + ". ");
            writer.write("التاريخ: " + transaction.getDate() + "\n");
            writer.write("   الوصف: " + transaction.getDescription() + "\n");
            writer.write("   النوع: " + getTransactionTypeInArabic(transaction.getType()) + "\n");
            writer.write("   الفئة: " + getCategoryInArabic(transaction.getCategory()) + "\n");
            
            String amountText;
            if ("income".equals(transaction.getType())) {
                amountText = "+" + df.format(transaction.getAmount());
            } else {
                amountText = "-" + df.format(Math.abs(transaction.getAmount()));
            }
            writer.write("   المبلغ: " + amountText + " ريال سعودي\n");
            
            if (transaction.getId() != null) {
                writer.write("   المرجع: " + transaction.getId() + "\n");
            }
            
            writer.write("\n");
            counter++;
        }
    }

    private void writeStatementFooter(FileWriter writer) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar", "SA"));
        String currentDateTime = dateFormat.format(new Date());

        writer.write("=====================================\n");
        writer.write("ملاحظات مهمة:\n");
        writer.write("- هذا كشف حساب إلكتروني معتمد\n");
        writer.write("- جميع المبالغ بالريال السعودي\n");
        writer.write("- للاستفسارات اتصل بخدمة العملاء\n");
        writer.write("- احتفظ بهذا الكشف لسجلاتك\n\n");
        writer.write("تم إنشاء هذا الكشف في: " + currentDateTime + "\n");
        writer.write("=====================================\n");
    }

    private void saveStatementToFirebase(String month, String filePath) {
        String statementId = "stmt_" + month + "_" + System.currentTimeMillis();
        
        DatabaseReference statementRef = databaseRef.child("accountStatements").child(userId).child(statementId);
        
        statementRef.child("id").setValue(statementId);
        statementRef.child("userId").setValue(userId);
        statementRef.child("month").setValue(month);
        statementRef.child("generatedAt").setValue(System.currentTimeMillis());
        statementRef.child("localPath").setValue(filePath);
        statementRef.child("isReady").setValue(true);
    }

    private boolean isTransactionInMonth(String transactionDate, String targetMonth) {

        return true;
    }

    private String getCurrentMonth() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return monthFormat.format(new Date());
    }

    private String getMonthNameInArabic(String month) {
        try {
            String[] parts = month.split("-");
            if (parts.length >= 2) {
                int monthNum = Integer.parseInt(parts[1]);
                String[] arabicMonths = {
                    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
                };
                if (monthNum >= 1 && monthNum <= 12) {
                    return arabicMonths[monthNum - 1] + " " + parts[0];
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing month", e);
        }
        return month;
    }

    private String getTransactionTypeInArabic(String type) {
        switch (type) {
            case "income":
                return "إيراد";
            case "expense":
                return "مصروف";
            default:
                return type;
        }
    }

    private String getCategoryInArabic(String category) {
        if (category == null) return "غير محدد";
        
        switch (category.toLowerCase()) {
            case "salary":
                return "راتب";
            case "transfer":
                return "تحويل";
            case "bill":
                return "فاتورة";
            case "shopping":
                return "مشتريات";
            case "food":
                return "طعام";
            case "transport":
                return "مواصلات";
            case "entertainment":
                return "ترفيه";
            case "health":
                return "صحة";
            case "education":
                return "تعليم";
            default:
                return category;
        }
    }

    public void generatePDFStatement(StatementCallback callback) {
        generateStatement(new StatementCallback() {
            @Override
            public void onStatementGenerated(String filePath) {
                callback.onStatementGenerated(filePath);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void emailStatement(String email, StatementCallback callback) {
        generateStatement(new StatementCallback() {
            @Override
            public void onStatementGenerated(String filePath) {
                callback.onStatementGenerated("تم إنشاء كشف الحساب وهو جاهز للإرسال عبر البريد الإلكتروني");
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getStatementHistory(StatementHistoryCallback callback) {
        databaseRef.child("accountStatements").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<StatementInfo> statements = new ArrayList<>();
                
                for (DataSnapshot statementSnapshot : snapshot.getChildren()) {
                    String id = statementSnapshot.child("id").getValue(String.class);
                    String month = statementSnapshot.child("month").getValue(String.class);
                    Long generatedAt = statementSnapshot.child("generatedAt").getValue(Long.class);
                    Boolean isReady = statementSnapshot.child("isReady").getValue(Boolean.class);
                    
                    if (id != null && month != null) {
                        StatementInfo info = new StatementInfo(id, month, 
                            generatedAt != null ? generatedAt : 0, 
                            isReady != null ? isReady : false);
                        statements.add(info);
                    }
                }
                
                callback.onStatementHistoryLoaded(statements);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل تاريخ الكشوف: " + error.getMessage());
            }
        });
    }

    public interface StatementHistoryCallback {
        void onStatementHistoryLoaded(List<StatementInfo> statements);
        void onError(String error);
    }

    public static class StatementInfo {
        private String id;
        private String month;
        private long generatedAt;
        private boolean isReady;

        public StatementInfo(String id, String month, long generatedAt, boolean isReady) {
            this.id = id;
            this.month = month;
            this.generatedAt = generatedAt;
            this.isReady = isReady;
        }

        public String getId() { return id; }
        public String getMonth() { return month; }
        public long getGeneratedAt() { return generatedAt; }
        public boolean isReady() { return isReady; }
    }
}

