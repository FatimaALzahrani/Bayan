package com.bank.bayan;

import android.content.Context;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SmartVoiceAssistant {
    
    private Context context;
    private VoiceHelper voiceHelper;
    private AccessibilityHelper accessibilityHelper;
    
    private static final List<String> BALANCE_COMMANDS = Arrays.asList(
        "رصيد", "بكم", "كم رصيدي", "أربعة", "4", "عرض الرصيد", "الرصيد"
    );
    
    private static final List<String> INCOME_COMMANDS = Arrays.asList(
        "إيرادات", "دخل", "إيداعات", "راتب"
    );
    
    private static final List<String> EXPENSES_COMMANDS = Arrays.asList(
        "مصروفات", "مصاريف", "مدفوعات"
    );
    
    private static final List<String> TRANSACTIONS_COMMANDS = Arrays.asList(
        "معاملات", "آخر المعاملات", "العمليات", "التحويلات السابقة"
    );
    
    private static final List<String> TRANSFER_COMMANDS = Arrays.asList(
        "تحويل", "واحد", "1", "تحويل أموال", "إرسال أموال", "تحويل مبلغ"
    );
    
    private static final List<String> BILL_PAYMENT_COMMANDS = Arrays.asList(
        "مدفوعات", "اثنين", "2", "دفع فاتورة", "سداد فاتورة", "فواتير", "دفع"
    );
    
    private static final List<String> SERVICES_COMMANDS = Arrays.asList(
        "خدمات", "ثلاثة", "3", "الخدمات", "خدمات البنك", "الخدمات المصرفية"
    );
    
    private static final List<String> ADD_AUTHORIZED_COMMANDS = Arrays.asList(
        "مفوض", "خمسة", "5", "إضافة مفوض", "مفوض جديد", "إضافة مفوض جديد"
    );
    
    private static final List<String> STATEMENT_COMMANDS = Arrays.asList(
        "كشف الحساب", "تحميل", "كشف", "بيان الحساب", "تحميل كشف"
    );
    
    private static final List<String> HELP_COMMANDS = Arrays.asList(
        "مساعدة", "مساعد", "ساعدني", "ماذا يمكنني أن أقول", "الأوامر"
    );
    
    private static final List<String> REPEAT_COMMANDS = Arrays.asList(
        "إعادة", "مرة أخرى", "كرر", "أعد"
    );
    
    private static final List<String> STOP_COMMANDS = Arrays.asList(
        "توقف", "اسكت", "كفى", "خلاص", "إيقاف"
    );

    public interface CommandCallback {
        void onBalanceRequested();
        void onIncomeRequested();
        void onExpensesRequested();
        void onTransactionsRequested();
        void onStatementRequested();
        void onTransferRequested();
        void onBillPaymentRequested();
        void onServicesRequested();
        void onAddAuthorizedPersonRequested();
        void onHelpRequested();
        void onRepeatRequested();
        void onUnknownCommand();
    }

    public SmartVoiceAssistant(Context context, VoiceHelper voiceHelper, AccessibilityHelper accessibilityHelper) {
        this.context = context;
        this.voiceHelper = voiceHelper;
        this.accessibilityHelper = accessibilityHelper;
    }

    public boolean processCommand(String command, CommandCallback callback) {
        if (command == null || command.trim().isEmpty()) {
            callback.onUnknownCommand();
            return false;
        }

        command = command.toLowerCase().trim();
        
        command = cleanCommand(command);

        if (containsAny(command, STOP_COMMANDS)) {
//            voiceHelper.stop();
            voiceHelper.speak("تم التوقف. كيف يمكنني مساعدتك؟");
            return true;
        }

        if (containsAny(command, BALANCE_COMMANDS)) {
            callback.onBalanceRequested();
            return true;
        }
        
        if (containsAny(command, INCOME_COMMANDS)) {
            callback.onIncomeRequested();
            return true;
        }
        
        if (containsAny(command, EXPENSES_COMMANDS)) {
            callback.onExpensesRequested();
            return true;
        }
        
        if (containsAny(command, TRANSACTIONS_COMMANDS)) {
            callback.onTransactionsRequested();
            return true;
        }
        
        if (containsAny(command, TRANSFER_COMMANDS)) {
            callback.onTransferRequested();
            return true;
        }
        
        if (containsAny(command, BILL_PAYMENT_COMMANDS)) {
            callback.onBillPaymentRequested();
            return true;
        }
        
        if (containsAny(command, SERVICES_COMMANDS)) {
            callback.onServicesRequested();
            return true;
        }
        
        if (containsAny(command, ADD_AUTHORIZED_COMMANDS)) {
            callback.onAddAuthorizedPersonRequested();
            return true;
        }
        
        if (containsAny(command, STATEMENT_COMMANDS)) {
            callback.onStatementRequested();
            return true;
        }
        
        if (containsAny(command, HELP_COMMANDS)) {
            callback.onHelpRequested();
            return true;
        }
        
        if (containsAny(command, REPEAT_COMMANDS)) {
            callback.onRepeatRequested();
            return true;
        }

        // Check for number-based commands
        if (containsNumber(command, "1") || containsNumber(command, "واحد")) {
            callback.onTransferRequested();
            return true;
        }
        
        if (containsNumber(command, "2") || containsNumber(command, "اثنين")) {
            callback.onBillPaymentRequested();
            return true;
        }
        
        if (containsNumber(command, "3") || containsNumber(command, "ثلاثة")) {
            callback.onServicesRequested();
            return true;
        }
        
        if (containsNumber(command, "4") || containsNumber(command, "أربعة")) {
            callback.onBalanceRequested();
            return true;
        }
        
        if (containsNumber(command, "5") || containsNumber(command, "خمسة")) {
            callback.onAddAuthorizedPersonRequested();
            return true;
        }

        callback.onUnknownCommand();
        return false;
    }

    private String cleanCommand(String command) {
        String[] fillerWords = {"أريد", "أبغى", "ممكن", "لو سمحت", "من فضلك", "يا", "هاي", "السلام عليكم"};
        
        for (String filler : fillerWords) {
            command = command.replace(filler, " ");
        }
        
        command = command.replaceAll("\\s+", " ").trim();
        
        return command;
    }

    private boolean containsAny(String command, List<String> keywords) {
        for (String keyword : keywords) {
            if (command.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNumber(String command, String number) {
        return command.contains(number);
    }

    public void handleTransferCommand() {
        String transferText = "خدمة التحويل. يمكنك تحويل الأموال إلى حسابات أخرى. " +
                "قل اسم المستفيد أو رقم الحساب للمتابعة";
        voiceHelper.speak(transferText);

    }

    public void handleBillPaymentCommand() {
        String billText = "خدمة دفع الفواتير. يمكنك دفع فواتير الكهرباء والماء والاتصالات. " +
                "قل نوع الفاتورة التي تريد دفعها";
        voiceHelper.speak(billText);
        
    }

    public void handleServicesCommand() {
        String servicesText = "الخدمات المصرفية المتاحة: " +
                "التحويلات المصرفية، " +
                "دفع الفواتير، " +
                "كشوف الحساب، " +
                "إدارة المفوضين، " +
                "الاستعلامات، " +
                "والإعدادات. " +
                "قل اسم الخدمة التي تريدها";
        voiceHelper.speak(servicesText);
    }

    public void handleAddAuthorizedPersonCommand() {
        String authorizedText = "إضافة مفوض جديد. يمكنك إضافة شخص مفوض للوصول إلى حسابك. " +
                "ستحتاج إلى اسم المفوض ورقم هويته ورقم هاتفه. " +
                "هل تريد المتابعة؟";
        voiceHelper.speak(authorizedText);
    }

    public void provideDetailedHelp() {
        String detailedHelp = "مرحباً بك في مساعد البنك الذكي. " +
                "يمكنك استخدام الأوامر الصوتية للتحكم في التطبيق. " +
                "الأوامر المتاحة: " +
                "للرصيد قل: رصيدي أو أربعة. " +
                "للتحويل قل: تحويل أو واحد. " +
                "للمدفوعات قل: مدفوعات أو اثنين. " +
                "للخدمات قل: خدمات أو ثلاثة. " +
                "لإضافة مفوض قل: مفوض أو خمسة. " +
                "لكشف الحساب قل: كشف الحساب. " +
                "يمكنك مقاطعتي في أي وقت بقول توقف أو اسكت";
        
        voiceHelper.speak(detailedHelp);
    }

    public boolean isWakeWord(String command) {
        String[] wakeWords = {"مساعد البنك", "بنك", "مساعد", "هاي بنك"};
        command = command.toLowerCase();
        
        for (String wakeWord : wakeWords) {
            if (command.contains(wakeWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void handleInterruption() {
//        voiceHelper.stop();
        voiceHelper.speak("نعم، كيف يمكنني مساعدتك؟");
    }

    public String getSmartResponse(String context, String userInput) {
        userInput = userInput.toLowerCase().trim();
        
        if (userInput.contains("شكرا") || userInput.contains("شكراً")) {
            return "العفو، سعيد لمساعدتك. هل تحتاج شيء آخر؟";
        }
        
        if (userInput.contains("لا") && context.contains("هل تريد")) {
            return "حسناً، يمكنك طلب المساعدة في أي وقت";
        }
        
        if (userInput.contains("نعم") && context.contains("هل تريد")) {
            return "ممتاز، دعنا نتابع";
        }
        
        if (userInput.contains("كيف")) {
            return "يمكنني مساعدتك في العمليات المصرفية. قل ما تريد فعله";
        }
        
        return "لم أفهم طلبك. يمكنك قول مساعدة للحصول على قائمة الأوامر";
    }

    public void announceMenuOptions() {
        String menuText = "القائمة الرئيسية. اختر من الخيارات التالية: " +
                "واحد للتحويل، " +
                "اثنين للمدفوعات، " +
                "ثلاثة للخدمات، " +
                "أربعة لعرض الرصيد، " +
                "خمسة لإضافة مفوض جديد. " +
                "قل رقم العملية أو اسمها";
        
        voiceHelper.speak(menuText);
    }

    public void handleContextualCommand(String command, String currentContext) {
        command = command.toLowerCase().trim();
        
        if (currentContext.equals("transfer")) {
            handleTransferContext(command);
        } else if (currentContext.equals("bill_payment")) {
            handleBillPaymentContext(command);
        } else if (currentContext.equals("services")) {
            handleServicesContext(command);
        } else {

        }
    }

    private void handleTransferContext(String command) {
        if (command.contains("أحمد") || command.contains("احمد")) {
            voiceHelper.speak("تحويل إلى أحمد. كم المبلغ المراد تحويله؟");
        } else if (command.contains("إلغاء") || command.contains("رجوع")) {
            voiceHelper.speak("تم إلغاء عملية التحويل. عودة للقائمة الرئيسية");
        } else if (isNumeric(command)) {
            voiceHelper.speak("المبلغ " + command + " ريال. هل تريد تأكيد التحويل؟");
        }
    }

    private void handleBillPaymentContext(String command) {
        if (command.contains("كهرباء")) {
            voiceHelper.speak("دفع فاتورة الكهرباء. أدخل رقم الحساب أو قل رقم العداد");
        } else if (command.contains("ماء")) {
            voiceHelper.speak("دفع فاتورة الماء. أدخل رقم الحساب");
        } else if (command.contains("اتصالات")) {
            voiceHelper.speak("دفع فاتورة الاتصالات. أدخل رقم الهاتف");
        }
    }

    private void handleServicesContext(String command) {
        if (command.contains("استعلام")) {
            voiceHelper.speak("خدمة الاستعلامات. يمكنك الاستعلام عن الرصيد أو المعاملات");
        } else if (command.contains("إعدادات")) {
            voiceHelper.speak("فتح الإعدادات");
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str.replaceAll("[^0-9.]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

