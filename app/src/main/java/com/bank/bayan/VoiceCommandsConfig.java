package com.bank.bayan;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceCommandsConfig {

    public enum CommandCategory {
        BALANCE,
        TRANSACTIONS,
        NAVIGATION,
        HELP,
        SETTINGS,
        TRANSFERS
    }

    private static final Map<CommandCategory, List<String>> COMMAND_PATTERNS = new HashMap<>();
    private static final Map<CommandCategory, String> COMMAND_DESCRIPTIONS = new HashMap<>();

    static {
        COMMAND_PATTERNS.put(CommandCategory.BALANCE, Arrays.asList(
                "رصيد", "رصيدي", "كم رصيدي", "بكم", "فلوسي", "أموالي", "المبلغ",
                "كم عندي", "كم معي", "المال", "النقود", "الحساب"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.BALANCE, "للاستماع لرصيدك الحالي");

        COMMAND_PATTERNS.put(CommandCategory.TRANSACTIONS, Arrays.asList(
                "معاملات", "آخر المعاملات", "المعاملات الأخيرة", "العمليات", "الحركات",
                "التحويلات", "المدفوعات", "الإيداعات", "السحوبات", "النشاط"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.TRANSACTIONS, "لسماع آخر المعاملات");

        COMMAND_PATTERNS.put(CommandCategory.NAVIGATION, Arrays.asList(
                "الرئيسية", "البيت", "الصفحة الرئيسية", "العودة", "رجوع", "إعدادات",
                "تحويل", "تحويلات", "الإعدادات", "القائمة", "الخيارات"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.NAVIGATION, "للتنقل بين صفحات التطبيق");

        COMMAND_PATTERNS.put(CommandCategory.HELP, Arrays.asList(
                "مساعدة", "مساعد", "ساعدني", "إرشادات", "تعليمات", "كيف", "ماذا أفعل",
                "الأوامر", "ما يمكنني قوله", "التعليمات", "الدليل"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.HELP, "للحصول على المساعدة والإرشادات");

        COMMAND_PATTERNS.put(CommandCategory.SETTINGS, Arrays.asList(
                "إعدادات الصوت", "سرعة الكلام", "درجة الصوت", "الإعدادات الصوتية",
                "تغيير الصوت", "تخصيص", "التفضيلات", "الخيارات المتقدمة"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.SETTINGS, "لتعديل إعدادات التطبيق والصوت");

        COMMAND_PATTERNS.put(CommandCategory.TRANSFERS, Arrays.asList(
                "تحويل", "تحويل أموال", "إرسال أموال", "تحويل فلوس", "إرسال",
                "دفع", "سداد", "دفع فاتورة", "تسديد"
        ));
        COMMAND_DESCRIPTIONS.put(CommandCategory.TRANSFERS, "للتحويلات ودفع الفواتير");
    }

    public static CommandCategory identifyCommand(String spokenText) {
        if (spokenText == null || spokenText.trim().isEmpty()) {
            return null;
        }

        String normalizedText = spokenText.toLowerCase().trim();

        // Check each category
        for (Map.Entry<CommandCategory, List<String>> entry : COMMAND_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (normalizedText.contains(pattern.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public static Map<CommandCategory, String> getAllCommands() {
        return new HashMap<>(COMMAND_DESCRIPTIONS);
    }

    public static List<String> getCommandPatterns(CommandCategory category) {
        return COMMAND_PATTERNS.get(category);
    }

    public static String getHelpText() {
        StringBuilder helpText = new StringBuilder("يمكنك استخدام الأوامر التالية:\n\n");

        for (Map.Entry<CommandCategory, String> entry : COMMAND_DESCRIPTIONS.entrySet()) {
            CommandCategory category = entry.getKey();
            String description = entry.getValue();
            List<String> examples = COMMAND_PATTERNS.get(category);

            helpText.append("• ").append(description).append("\n");
            helpText.append("  أمثلة: ");

            if (examples != null && !examples.isEmpty()) {
                for (int i = 0; i < Math.min(3, examples.size()); i++) {
                    helpText.append("\"").append(examples.get(i)).append("\"");
                    if (i < Math.min(2, examples.size() - 1)) {
                        helpText.append("، ");
                    }
                }
            }
            helpText.append("\n\n");
        }

        helpText.append("لاستخدام الأوامر الصوتية، اضغط طويلاً في أي مكان على الشاشة ثم قل أمرك.");

        return helpText.toString();
    }

    public static String getQuickHelp(CommandCategory category) {
        String description = COMMAND_DESCRIPTIONS.get(category);
        List<String> examples = COMMAND_PATTERNS.get(category);

        if (description == null || examples == null) {
            return "لا توجد معلومات متاحة لهذا الأمر";
        }

        StringBuilder quickHelp = new StringBuilder(description);
        quickHelp.append(". يمكنك قول: ");

        for (int i = 0; i < Math.min(3, examples.size()); i++) {
            quickHelp.append("\"").append(examples.get(i)).append("\"");
            if (i < Math.min(2, examples.size() - 1)) {
                quickHelp.append("، ");
            }
        }

        return quickHelp.toString();
    }

    public static class ResponseTemplates {
        public static final String BALANCE_RESPONSE = "رصيدك الحالي هو %s";
        public static final String INCOME_RESPONSE = "إيراداتك هذا الشهر %s";
        public static final String EXPENSES_RESPONSE = "مصروفاتك هذا الشهر %s";
        public static final String NO_TRANSACTIONS = "لا توجد معاملات لعرضها";
        public static final String TRANSACTIONS_SUMMARY = "آخر %d معاملات: %s";
        public static final String NAVIGATION_SUCCESS = "تم الانتقال إلى %s";
        public static final String COMMAND_NOT_UNDERSTOOD = "لم أفهم أمرك. قل \"مساعدة\" لسماع الأوامر المتاحة";
        public static final String LISTENING = "أستمع إليك الآن، قل أمرك";
        public static final String COMMAND_PROCESSING = "جاري تنفيذ الأمر...";
        public static final String ERROR_OCCURRED = "حدث خطأ أثناء تنفيذ الأمر";
        public static final String FEATURE_NOT_AVAILABLE = "هذه الميزة غير متاحة حالياً";
    }

    public static class FeedbackMessages {
        public static final String WELCOME = "مرحباً بك في البنك الرقمي الصوتي";
        public static final String GOODBYE = "شكراً لاستخدامك البنك الرقمي";
        public static final String PERMISSION_GRANTED = "تم منح الإذن بنجاح";
        public static final String PERMISSION_DENIED = "يتطلب إذن الميكروفون لاستخدام الأوامر الصوتية";
        public static final String LOADING = "جاري التحميل، يرجى الانتظار";
        public static final String SUCCESS = "تم بنجاح";
        public static final String CANCELLED = "تم الإلغاء";
        public static final String TIMEOUT = "انتهت مهلة الانتظار";
        public static final String NETWORK_ERROR = "خطأ في الاتصال، تحقق من الإنترنت";
        public static final String TRY_AGAIN = "يرجى المحاولة مرة أخرى";
    }

    public static class SpeechSettings {
        public static final float SPEED_VERY_SLOW = 0.5f;
        public static final float SPEED_SLOW = 0.7f;
        public static final float SPEED_NORMAL = 0.9f;
        public static final float SPEED_FAST = 1.2f;
        public static final float SPEED_VERY_FAST = 1.5f;

        public static final float PITCH_LOW = 0.8f;
        public static final float PITCH_NORMAL = 1.0f;
        public static final float PITCH_HIGH = 1.2f;

        public static String getSpeedDescription(float speed) {
            if (speed <= SPEED_VERY_SLOW) return "بطيء جداً";
            if (speed <= SPEED_SLOW) return "بطيء";
            if (speed <= SPEED_NORMAL) return "طبيعي";
            if (speed <= SPEED_FAST) return "سريع";
            return "سريع جداً";
        }

        public static String getPitchDescription(float pitch) {
            if (pitch < PITCH_NORMAL) return "منخفض";
            if (pitch > PITCH_NORMAL) return "مرتفع";
            return "طبيعي";
        }
    }

    public static class AudioCues {
        public static final String LISTENING_START = "بيب";
        public static final String COMMAND_RECOGNIZED = "نغمة قصيرة";
        public static final String ERROR_SOUND = "نغمة خطأ";
        public static final String SUCCESS_SOUND = "نغمة نجاح";
        public static final String NAVIGATION_SOUND = "نغمة تنقل";
    }


    public static String getContextualHelp(String currentScreen) {
        switch (currentScreen.toLowerCase()) {
            case "main":
            case "home":
                return "أنت في الصفحة الرئيسية. يمكنك قول: رصيدي، آخر المعاملات، تحويل، أو إعدادات";

            case "transactions":
                return "أنت في صفحة المعاملات. يمكنك قول: اقرأ المعاملة الأولى، التفاصيل، أو العودة للرئيسية";

            case "transfer":
                return "أنت في صفحة التحويلات. يمكنك قول: تحويل جديد، المستفيدين، أو إلغاء";

            case "settings":
                return "أنت في الإعدادات. يمكنك قول: إعدادات الصوت، سرعة الكلام، أو حفظ الإعدادات";

            default:
                return getHelpText();
        }
    }
}