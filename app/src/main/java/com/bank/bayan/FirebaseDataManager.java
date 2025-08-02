package com.bank.bayan;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseDataManager {
    
    private static final String TAG = "FirebaseDataManager";
    private Context context;
    private DatabaseReference databaseRef;
    private String userId;
    
    public interface UserDataCallback {
        void onUserDataLoaded(UserData userData);
        void onError(String error);
    }
    
    public interface TransactionsCallback {
        void onTransactionsLoaded(List<Transaction> transactions);
        void onError(String error);
    }
    
    public interface BalanceCallback {
        void onBalanceLoaded(double balance);
        void onBalanceUpdated(double newBalance);
        void onError(String error);
    }
    
    public interface MonthlyStatsCallback {
        void onStatsLoaded(MonthlyStats stats);
        void onError(String error);
    }
    
    public interface OperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public FirebaseDataManager(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    public void loadUserData(UserDataCallback callback) {
        DatabaseReference userRef = databaseRef.child("users").child(userId);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserData userData = snapshot.getValue(UserData.class);
                    if (userData != null) {
                        userData.setUserId(userId);
                        callback.onUserDataLoaded(userData);
                    } else {
                        callback.onError("خطأ في تحليل بيانات المستخدم");
                    }
                } else {
                    // Create default user data
                    createDefaultUserData(callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل بيانات المستخدم: " + error.getMessage());
            }
        });
    }

    public void updateUserData(UserData userData, OperationCallback callback) {
        DatabaseReference userRef = databaseRef.child("users").child(userId);
        
        userRef.setValue(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess("تم تحديث بيانات المستخدم بنجاح"))
                .addOnFailureListener(e -> callback.onError("خطأ في تحديث البيانات: " + e.getMessage()));
    }

    private void createDefaultUserData(UserDataCallback callback) {
        UserData defaultUser = new UserData();
        defaultUser.setUserId(userId);
        defaultUser.setName("المستخدم");
        defaultUser.setBalance(3340.55);
        defaultUser.setAccountNumber("1234567890");
        defaultUser.setAccountType("savings");
        defaultUser.setActive(true);
        
        UserPreferences preferences = new UserPreferences();
        preferences.setVoiceEnabled(true);
        preferences.setLanguage("ar");
        preferences.setNotifications(true);
        defaultUser.setPreferences(preferences);

        DatabaseReference userRef = databaseRef.child("users").child(userId);
        userRef.setValue(defaultUser)
                .addOnSuccessListener(aVoid -> callback.onUserDataLoaded(defaultUser))
                .addOnFailureListener(e -> callback.onError("خطأ في إنشاء بيانات المستخدم: " + e.getMessage()));
    }

    public void loadBalance(BalanceCallback callback) {
        DatabaseReference balanceRef = databaseRef.child("users").child(userId).child("balance");
        
        balanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double balance = snapshot.getValue(Double.class);
                if (balance != null) {
                    callback.onBalanceLoaded(balance);
                } else {
                    callback.onError("لم يتم العثور على بيانات الرصيد");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل الرصيد: " + error.getMessage());
            }
        });
    }

    public void updateBalance(double newBalance, OperationCallback callback) {
        DatabaseReference balanceRef = databaseRef.child("users").child(userId).child("balance");
        
        balanceRef.setValue(newBalance)
                .addOnSuccessListener(aVoid -> callback.onSuccess("تم تحديث الرصيد بنجاح"))
                .addOnFailureListener(e -> callback.onError("خطأ في تحديث الرصيد: " + e.getMessage()));
    }

    public void loadTransactions(int limit, TransactionsCallback callback) {
        DatabaseReference transactionsRef = databaseRef.child("transactions").child(userId);
        
        transactionsRef.orderByChild("date").limitToLast(limit).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> transactions = new ArrayList<>();
                
                for (DataSnapshot txnSnapshot : snapshot.getChildren()) {
                    Transaction transaction = txnSnapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.setId(txnSnapshot.getKey());
                        transactions.add(0, transaction);
                    }
                }
                
                callback.onTransactionsLoaded(transactions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل المعاملات: " + error.getMessage());
            }
        });
    }

    public void addTransaction(Transaction transaction, OperationCallback callback) {
        DatabaseReference transactionsRef = databaseRef.child("transactions").child(userId);
        String transactionId = transactionsRef.push().getKey();
        
        if (transactionId != null) {
            transaction.setId(transactionId);
            transaction.setUserId(userId);
            
            transactionsRef.child(transactionId).setValue(transaction)
                    .addOnSuccessListener(aVoid -> {
                        // Update balance after adding transaction
                        updateBalanceAfterTransaction(transaction, callback);
                    })
                    .addOnFailureListener(e -> callback.onError("خطأ في إضافة المعاملة: " + e.getMessage()));
        } else {
            callback.onError("خطأ في إنشاء معرف المعاملة");
        }
    }

    private void updateBalanceAfterTransaction(Transaction transaction, OperationCallback callback) {
        DatabaseReference balanceRef = databaseRef.child("users").child(userId).child("balance");
        
        balanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double currentBalance = snapshot.getValue(Double.class);
                if (currentBalance != null) {
                    double newBalance = currentBalance;
                    
                    if ("income".equals(transaction.getType())) {
                        newBalance += transaction.getAmount();
                    } else if ("expense".equals(transaction.getType())) {
                        newBalance -= Math.abs(transaction.getAmount());
                    }
                    
                    balanceRef.setValue(newBalance)
                            .addOnSuccessListener(aVoid -> {
                                updateMonthlyStats(transaction);
                                callback.onSuccess("تمت المعاملة بنجاح");
                            })
                            .addOnFailureListener(e -> callback.onError("خطأ في تحديث الرصيد: " + e.getMessage()));
                } else {
                    callback.onError("خطأ في قراءة الرصيد الحالي");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في قراءة الرصيد: " + error.getMessage());
            }
        });
    }

    public void loadMonthlyStats(String month, MonthlyStatsCallback callback) {
        DatabaseReference statsRef = databaseRef.child("monthlyStatistics").child(userId).child(month);
        
        statsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    MonthlyStats stats = snapshot.getValue(MonthlyStats.class);
                    if (stats != null) {
                        callback.onStatsLoaded(stats);
                    } else {
                        callback.onError("خطأ في تحليل الإحصائيات الشهرية");
                    }
                } else {
                    calculateMonthlyStats(month, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل الإحصائيات: " + error.getMessage());
            }
        });
    }

    private void calculateMonthlyStats(String month, MonthlyStatsCallback callback) {
        DatabaseReference transactionsRef = databaseRef.child("transactions").child(userId);
        
        transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MonthlyStats stats = new MonthlyStats();
                stats.setMonth(month);
                
                double totalIncome = 0.0;
                double totalExpenses = 0.0;
                int transactionCount = 0;
                Map<String, Double> categories = new HashMap<>();
                
                for (DataSnapshot txnSnapshot : snapshot.getChildren()) {
                    Transaction transaction = txnSnapshot.getValue(Transaction.class);
                    if (transaction != null && isTransactionInMonth(transaction.getDate(), month)) {
                        transactionCount++;
                        
                        if ("income".equals(transaction.getType())) {
                            totalIncome += transaction.getAmount();
                        } else if ("expense".equals(transaction.getType())) {
                            totalExpenses += Math.abs(transaction.getAmount());
                        }
                        
                        String category = transaction.getCategory();
                        if (category != null) {
                            double currentAmount = categories.getOrDefault(category, 0.0);
                            if ("income".equals(transaction.getType())) {
                                categories.put(category, currentAmount + transaction.getAmount());
                            } else {
                                categories.put(category, currentAmount - Math.abs(transaction.getAmount()));
                            }
                        }
                    }
                }
                
                stats.setTotalIncome(totalIncome);
                stats.setTotalExpenses(-totalExpenses);
                stats.setNetIncome(totalIncome - totalExpenses);
                stats.setTransactionCount(transactionCount);
                stats.setCategories(categories);
                
                DatabaseReference statsRef = databaseRef.child("monthlyStatistics").child(userId).child(month);
                statsRef.setValue(stats);
                
                callback.onStatsLoaded(stats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في حساب الإحصائيات: " + error.getMessage());
            }
        });
    }

    private void updateMonthlyStats(Transaction transaction) {
        String currentMonth = getCurrentMonth();
        DatabaseReference statsRef = databaseRef.child("monthlyStatistics").child(userId).child(currentMonth);
        
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MonthlyStats stats;
                if (snapshot.exists()) {
                    stats = snapshot.getValue(MonthlyStats.class);
                    if (stats == null) {
                        stats = new MonthlyStats();
                        stats.setMonth(currentMonth);
                    }
                } else {
                    stats = new MonthlyStats();
                    stats.setMonth(currentMonth);
                }
                
                if ("income".equals(transaction.getType())) {
                    stats.setTotalIncome(stats.getTotalIncome() + transaction.getAmount());
                } else if ("expense".equals(transaction.getType())) {
                    stats.setTotalExpenses(stats.getTotalExpenses() - Math.abs(transaction.getAmount()));
                }
                
                stats.setNetIncome(stats.getTotalIncome() + stats.getTotalExpenses());
                stats.setTransactionCount(stats.getTransactionCount() + 1);
                
                Map<String, Double> categories = stats.getCategories();
                if (categories == null) {
                    categories = new HashMap<>();
                }
                
                String category = transaction.getCategory();
                if (category != null) {
                    double currentAmount = categories.getOrDefault(category, 0.0);
                    if ("income".equals(transaction.getType())) {
                        categories.put(category, currentAmount + transaction.getAmount());
                    } else {
                        categories.put(category, currentAmount - Math.abs(transaction.getAmount()));
                    }
                }
                stats.setCategories(categories);
                
                statsRef.setValue(stats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error updating monthly stats: " + error.getMessage());
            }
        });
    }

    public void loadAuthorizedPersons(AuthorizedPersonsCallback callback) {
        DatabaseReference authRef = databaseRef.child("authorizedPersons").child(userId);
        
        authRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AuthorizedPerson> persons = new ArrayList<>();
                
                for (DataSnapshot personSnapshot : snapshot.getChildren()) {
                    AuthorizedPerson person = personSnapshot.getValue(AuthorizedPerson.class);
                    if (person != null) {
                        person.setId(personSnapshot.getKey());
                        persons.add(person);
                    }
                }
                
                callback.onAuthorizedPersonsLoaded(persons);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل المفوضين: " + error.getMessage());
            }
        });
    }

    public void addAuthorizedPerson(AuthorizedPerson person, OperationCallback callback) {
        DatabaseReference authRef = databaseRef.child("authorizedPersons").child(userId);
        String personId = authRef.push().getKey();
        
        if (personId != null) {
            person.setId(personId);
            person.setAddedAt(System.currentTimeMillis());
            
            authRef.child(personId).setValue(person)
                    .addOnSuccessListener(aVoid -> callback.onSuccess("تم إضافة المفوض بنجاح"))
                    .addOnFailureListener(e -> callback.onError("خطأ في إضافة المفوض: " + e.getMessage()));
        } else {
            callback.onError("خطأ في إنشاء معرف المفوض");
        }
    }

    public void loadBankingServices(ServicesCallback callback) {
        DatabaseReference servicesRef = databaseRef.child("services");
        
        servicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    BankingServices services = snapshot.getValue(BankingServices.class);
                    if (services != null) {
                        callback.onServicesLoaded(services);
                    } else {
                        callback.onError("خطأ في تحليل بيانات الخدمات");
                    }
                } else {
                    // Create default services
                    createDefaultServices(callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("خطأ في تحميل الخدمات: " + error.getMessage());
            }
        });
    }

    private void createDefaultServices(ServicesCallback callback) {
        BankingServices defaultServices = new BankingServices();

        DatabaseReference servicesRef = databaseRef.child("services");
        servicesRef.setValue(defaultServices)
                .addOnSuccessListener(aVoid -> callback.onServicesLoaded(defaultServices))
                .addOnFailureListener(e -> callback.onError("خطأ في إنشاء الخدمات: " + e.getMessage()));
    }

    private boolean isTransactionInMonth(String transactionDate, String targetMonth) {
        return true;
    }

    private String getCurrentMonth() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return monthFormat.format(new Date());
    }

    public interface AuthorizedPersonsCallback {
        void onAuthorizedPersonsLoaded(List<AuthorizedPerson> persons);
        void onError(String error);
    }

    public interface ServicesCallback {
        void onServicesLoaded(BankingServices services);
        void onError(String error);
    }

    public static class UserData {
        private String userId;
        private String name;
        private String email;
        private String phone;
        private String accountNumber;
        private double balance;
        private String accountType;
        private boolean isActive;
        private long createdAt;
        private long lastLogin;
        private UserPreferences preferences;

        public UserData() {}

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

        public long getLastLogin() { return lastLogin; }
        public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

        public UserPreferences getPreferences() { return preferences; }
        public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }
    }

    public static class UserPreferences {
        private boolean voiceEnabled;
        private String language;
        private boolean notifications;
        private AccessibilitySettings accessibility;

        public UserPreferences() {}

        public boolean isVoiceEnabled() { return voiceEnabled; }
        public void setVoiceEnabled(boolean voiceEnabled) { this.voiceEnabled = voiceEnabled; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public boolean isNotifications() { return notifications; }
        public void setNotifications(boolean notifications) { this.notifications = notifications; }

        public AccessibilitySettings getAccessibility() { return accessibility; }
        public void setAccessibility(AccessibilitySettings accessibility) { this.accessibility = accessibility; }
    }

    public static class AccessibilitySettings {
        private boolean screenReader;
        private boolean voiceCommands;
        private boolean hapticFeedback;

        public AccessibilitySettings() {}

        public boolean isScreenReader() { return screenReader; }
        public void setScreenReader(boolean screenReader) { this.screenReader = screenReader; }

        public boolean isVoiceCommands() { return voiceCommands; }
        public void setVoiceCommands(boolean voiceCommands) { this.voiceCommands = voiceCommands; }

        public boolean isHapticFeedback() { return hapticFeedback; }
        public void setHapticFeedback(boolean hapticFeedback) { this.hapticFeedback = hapticFeedback; }
    }

    public static class MonthlyStats {
        private String month;
        private double totalIncome;
        private double totalExpenses;
        private double netIncome;
        private int transactionCount;
        private Map<String, Double> categories;

        public MonthlyStats() {
            this.categories = new HashMap<>();
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }

        public double getTotalIncome() { return totalIncome; }
        public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

        public double getTotalExpenses() { return totalExpenses; }
        public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }

        public double getNetIncome() { return netIncome; }
        public void setNetIncome(double netIncome) { this.netIncome = netIncome; }

        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

        public Map<String, Double> getCategories() { return categories; }
        public void setCategories(Map<String, Double> categories) { this.categories = categories; }
    }

    public static class AuthorizedPerson {
        private String id;
        private String name;
        private String relationship;
        private String phone;
        private String nationalId;
        private PersonPermissions permissions;
        private boolean isActive;
        private long addedAt;

        // Constructors
        public AuthorizedPerson() {}

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }

        public PersonPermissions getPermissions() { return permissions; }
        public void setPermissions(PersonPermissions permissions) { this.permissions = permissions; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public long getAddedAt() { return addedAt; }
        public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
    }

    public static class PersonPermissions {
        private boolean viewBalance;
        private boolean makeTransfers;
        private boolean payBills;
        private boolean viewStatements;

        // Constructors
        public PersonPermissions() {}

        // Getters and Setters
        public boolean isViewBalance() { return viewBalance; }
        public void setViewBalance(boolean viewBalance) { this.viewBalance = viewBalance; }

        public boolean isMakeTransfers() { return makeTransfers; }
        public void setMakeTransfers(boolean makeTransfers) { this.makeTransfers = makeTransfers; }

        public boolean isPayBills() { return payBills; }
        public void setPayBills(boolean payBills) { this.payBills = payBills; }

        public boolean isViewStatements() { return viewStatements; }
        public void setViewStatements(boolean viewStatements) { this.viewStatements = viewStatements; }
    }

    public static class BankingServices {
        private TransferService transfers;
        private BillPaymentService billPayments;
        private VoiceCommandService voiceCommands;

        // Constructors
        public BankingServices() {}

        // Getters and Setters
        public TransferService getTransfers() { return transfers; }
        public void setTransfers(TransferService transfers) { this.transfers = transfers; }

        public BillPaymentService getBillPayments() { return billPayments; }
        public void setBillPayments(BillPaymentService billPayments) { this.billPayments = billPayments; }

        public VoiceCommandService getVoiceCommands() { return voiceCommands; }
        public void setVoiceCommands(VoiceCommandService voiceCommands) { this.voiceCommands = voiceCommands; }
    }

    public static class TransferService {
        private boolean enabled;
        private double dailyLimit;
        private double monthlyLimit;
        private double fee;

        // Constructors
        public TransferService() {}

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(double dailyLimit) { this.dailyLimit = dailyLimit; }

        public double getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
    }

    public static class BillPaymentService {
        private boolean enabled;
        private Map<String, BillProvider> providers;

        // Constructors
        public BillPaymentService() {
            this.providers = new HashMap<>();
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Map<String, BillProvider> getProviders() { return providers; }
        public void setProviders(Map<String, BillProvider> providers) { this.providers = providers; }
    }

    public static class BillProvider {
        private String name;
        private String code;
        private boolean enabled;

        // Constructors
        public BillProvider() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class VoiceCommandService {
        private boolean enabled;
        private List<String> supportedCommands;

        public VoiceCommandService() {
            this.supportedCommands = new ArrayList<>();
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<String> getSupportedCommands() { return supportedCommands; }
        public void setSupportedCommands(List<String> supportedCommands) { this.supportedCommands = supportedCommands; }
    }
}

