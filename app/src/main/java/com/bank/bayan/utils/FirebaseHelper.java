package com.bank.bayan.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.bank.bayan.models.User;
import com.bank.bayan.models.Account;
import com.bank.bayan.models.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirebaseHelper {
    private static final String USERS_NODE = "users";
    private static final String TRANSACTIONS_NODE = "transactions";
    private DatabaseReference databaseReference;

    public FirebaseHelper() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public interface UserDataCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public interface TransactionsCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(String error);
    }

    public interface BalanceUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public void getUserData(String userId, UserDataCallback callback) {
        databaseReference.child(USERS_NODE).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                if (user.getAccount() == null) {
                                    createDefaultAccount(userId, user, callback);
                                } else {
                                    callback.onSuccess(user);
                                }
                            } else {
                                callback.onError("خطأ في قراءة بيانات المستخدم");
                            }
                        } else {
                            callback.onError("المستخدم غير موجود");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError("خطأ في الاتصال بقاعدة البيانات: " + error.getMessage());
                    }
                });
    }

    public void getUserTransactions(String userId, TransactionsCallback callback) {
        databaseReference.child(TRANSACTIONS_NODE).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Transaction> transactions = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                                Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                                if (transaction != null) {
                                    transactions.add(transaction);
                                }
                            }
                        }
                        callback.onSuccess(transactions);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError("خطأ في قراءة المعاملات: " + error.getMessage());
                    }
                });
    }

    private void createDefaultAccount(String userId, User user, UserDataCallback callback) {
        String accountNumber = generateAccountNumber();

        Account defaultAccount = new Account();
        defaultAccount.setAccountNumber(accountNumber);
        defaultAccount.setBalance(0.0);
        defaultAccount.setAccountType("حساب جاري");
        defaultAccount.setActive(true);
        defaultAccount.setLastUpdated(System.currentTimeMillis());

        user.setAccount(defaultAccount);

        databaseReference.child(USERS_NODE).child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onError("فشل في إنشاء الحساب: " + e.getMessage()));
    }

    public void updateBalance(String userId, double newBalance, BalanceUpdateCallback callback) {
        databaseReference.child(USERS_NODE).child(userId).child("account").child("balance")
                .setValue(newBalance)
                .addOnSuccessListener(aVoid -> {
                    databaseReference.child(USERS_NODE).child(userId).child("account").child("lastUpdated")
                            .setValue(System.currentTimeMillis());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("فشل في تحديث الرصيد: " + e.getMessage()));
    }

    public void addTransaction(String userId, Transaction transaction, BalanceUpdateCallback callback) {
        String transactionKey = databaseReference.child(TRANSACTIONS_NODE).child(userId).push().getKey();

        if (transactionKey != null) {
            databaseReference.child(TRANSACTIONS_NODE).child(userId)
                    .child(transactionKey)
                    .setValue(transaction)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError("فشل في حفظ المعاملة: " + e.getMessage()));
        } else {
            callback.onError("خطأ في إنشاء المعاملة");
        }
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();

        // Generate 16 digit account number in format: XXXX XXXX XXXX XXXX
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) {
                accountNumber.append(" ");
            }
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    public void performTransfer(String fromUserId, String toAccountNumber,
                                double amount, String purpose, String recipientName,
                                BalanceUpdateCallback callback) {
        getUserData(fromUserId, new UserDataCallback() {
            @Override
            public void onSuccess(User user) {
                double currentBalance = user.getAccount().getBalance();
                if (currentBalance >= amount) {
                    double newBalance = currentBalance - amount;

                    Transaction transaction = new Transaction();
                    transaction.setId("txn" + System.currentTimeMillis());
                    transaction.setType("expense");
                    transaction.setAmount(amount);
                    transaction.setCategory("تحويل");
                    transaction.setDescription("تحويل إلى " + recipientName);
                    transaction.setDate(getCurrentDateString());
                    transaction.setIcon("transfer");
                    transaction.setUserId(fromUserId);

                    updateBalance(fromUserId, newBalance, new BalanceUpdateCallback() {
                        @Override
                        public void onSuccess() {
                            addTransaction(fromUserId, transaction, callback);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } else {
                    callback.onError("الرصيد غير كافي");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private String getCurrentDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy hh:mm a", new java.util.Locale("ar"));
        return sdf.format(new java.util.Date());
    }
}