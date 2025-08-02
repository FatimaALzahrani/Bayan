package com.bank.bayan;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class AccessibilityHelper {
    
    private Context context;
    private AccessibilityManager accessibilityManager;
    private Vibrator vibrator;
    
    public enum HapticFeedbackType {
        SELECTION,
        NAVIGATION,
        SUCCESS,
        ERROR,
        WARNING
    }

    public AccessibilityHelper(Context context) {
        this.context = context;
        this.accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public boolean isAccessibilityEnabled() {
        return accessibilityManager != null && accessibilityManager.isEnabled();
    }

    public boolean isScreenReaderEnabled() {
        if (accessibilityManager == null) return false;
        
        List<AccessibilityServiceInfo> serviceInfoList = 
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        
        return !serviceInfoList.isEmpty();
    }

    public boolean isTouchExplorationEnabled() {
        return accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled();
    }

    public void setContentDescription(View view, String description) {
        if (view != null && description != null) {
            view.setContentDescription(description);
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    public void setContentDescriptionWithAction(View view, String description, String actionHint) {
        if (view != null && description != null) {
            String fullDescription = description;
            if (actionHint != null && !actionHint.isEmpty()) {
                fullDescription += ". " + actionHint;
            }
            view.setContentDescription(fullDescription);
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    public void setupCurrencyAccessibility(View view, double amount, String context) {
        if (view == null) return;
        
        String amountDescription = formatCurrencyForAccessibility(amount);
        String fullDescription = context + ": " + amountDescription;
        
        setContentDescription(view, fullDescription);
        view.setFocusable(true);
        view.setClickable(true);
    }

    public void setupTransactionAccessibility(View view, String description, double amount, 
                                            String type, String date, int position) {
        if (view == null) return;
        
        String transactionType = "income".equals(type) ? "إيراد" : "مصروف";
        String amountDescription = formatCurrencyForAccessibility(amount);
        
        String fullDescription = String.format(
            "المعاملة رقم %d. %s. %s. المبلغ %s. التاريخ %s. انقر للتفاصيل",
            position + 1, transactionType, description, amountDescription, date
        );
        
        setContentDescriptionWithAction(view, fullDescription, "انقر مرتين للخيارات");
        view.setFocusable(true);
        view.setClickable(true);
    }

    public void setupButtonAccessibility(View button, String buttonText, String action) {
        if (button == null) return;
        
        String description = buttonText;
        if (action != null && !action.isEmpty()) {
            description += ". " + action;
        }
        
        setContentDescription(button, description);
        button.setFocusable(true);
        button.setClickable(true);
    }

    public void setupNavigationAccessibility(View navItem, String itemName, boolean isSelected) {
        if (navItem == null) return;
        
        String description = itemName;
        if (isSelected) {
            description += ". محدد حالياً";
        }
        description += ". انقر للانتقال";
        
        setContentDescription(navItem, description);
        navItem.setSelected(isSelected);
    }

    public void announceForAccessibility(String message) {
        if (accessibilityManager != null && isAccessibilityEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(message);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    public void announceBalanceChange(double oldBalance, double newBalance) {
        if (oldBalance != newBalance) {
            String message = "تم تحديث الرصيد من " + 
                formatCurrencyForAccessibility(oldBalance) + " إلى " + 
                formatCurrencyForAccessibility(newBalance);
            announceForAccessibility(message);
        }
    }

    public void announceTransactionComplete(String transactionType, double amount) {
        String amountText = formatCurrencyForAccessibility(amount);
        String message = "تمت العملية بنجاح. " + transactionType + " بمبلغ " + amountText;
        announceForAccessibility(message);
    }

    public void announceError(String errorMessage, String context) {
        String message = "خطأ";
        if (context != null && !context.isEmpty()) {
            message += " في " + context;
        }
        message += ": " + errorMessage;
        announceForAccessibility(message);
    }

    public void provideHapticFeedback(HapticFeedbackType type) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        long[] pattern;
        int amplitude = VibrationEffect.DEFAULT_AMPLITUDE;
        
        switch (type) {
            case SELECTION:
                pattern = new long[]{0, 50};
                break;
            case NAVIGATION:
                pattern = new long[]{0, 30, 50, 30};
                break;
            case SUCCESS:
                pattern = new long[]{0, 100, 50, 100};
                break;
            case ERROR:
                pattern = new long[]{0, 200, 100, 200, 100, 200};
                break;
            case WARNING:
                pattern = new long[]{0, 150};
                break;
            default:
                pattern = new long[]{0, 50};
                break;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    public void setupFocusHandling(View view, String focusDescription) {
        if (view == null) return;
        
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                provideHapticFeedback(HapticFeedbackType.NAVIGATION);
                if (focusDescription != null && isScreenReaderEnabled()) {
                    announceForAccessibility("التركيز على " + focusDescription);
                }
            }
        });
    }

    public void setupFormFieldAccessibility(View field, String label, String hint, boolean isRequired) {
        if (field == null) return;
        
        String description = label;
        if (isRequired) {
            description += " مطلوب";
        }
        if (hint != null && !hint.isEmpty()) {
            description += ". " + hint;
        }
        
        setContentDescription(field, description);
        field.setFocusable(true);
        field.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setupProgressAccessibility(View progressView, String operation, int progress) {
        if (progressView == null) return;
        
        String description = operation + ". التقدم " + progress + " بالمائة";
        setContentDescription(progressView, description);
        
        if (progress % 25 == 0) {
            announceForAccessibility(description);
        }
    }

    private String formatCurrencyForAccessibility(double amount) {
        if (amount == 0) {
            return "صفر ريال";
        }
        
        boolean isNegative = amount < 0;
        amount = Math.abs(amount);
        
        String formattedAmount = String.format("%,.2f", amount);
        
        formattedAmount = formattedAmount.replace(".", " فاصلة ");
        
        String result = formattedAmount + " ريال سعودي";
        
        if (isNegative) {
            result = "ناقص " + result;
        }
        
        return result;
    }

    public void setupListAccessibility(View listView, int itemCount, String listDescription) {
        if (listView == null) return;
        
        String description = listDescription + ". يحتوي على " + itemCount + " عنصر";
        setContentDescription(listView, description);
        
        announceForAccessibility(description);
    }

    public void setupTabAccessibility(View tab, String tabName, int position, int totalTabs, boolean isSelected) {
        if (tab == null) return;
        
        String description = "تبويب " + tabName + ". " + (position + 1) + " من " + totalTabs;
        if (isSelected) {
            description += ". محدد حالياً";
        }
        
        setContentDescription(tab, description);
        tab.setSelected(isSelected);
    }

    public void announceLoadingState(String operation, boolean isLoading) {
        if (isLoading) {
            announceForAccessibility("جاري " + operation + ". يرجى الانتظار");
        } else {
            announceForAccessibility("تم " + operation + " بنجاح");
        }
    }

    public void setupDialogAccessibility(View dialog, String title, String message, boolean isError) {
        if (dialog == null) return;
        
        String description = title;
        if (message != null && !message.isEmpty()) {
            description += ". " + message;
        }
        
        setContentDescription(dialog, description);
        
        announceForAccessibility(description);
        
        if (isError) {
            provideHapticFeedback(HapticFeedbackType.ERROR);
        } else {
            provideHapticFeedback(HapticFeedbackType.WARNING);
        }
    }

    public boolean isHighContrastEnabled() {

        return false;
    }

    public float getTextSizeMultiplier() {

        return 1.0f;
    }

    public void setupTimeSensitiveAccessibility(View view, String content, long timeoutMillis) {
        if (view == null) return;
        
        setContentDescription(view, content);
        announceForAccessibility(content);
        
        if (isAccessibilityEnabled()) {

        }
    }


    public void cleanup() {
        context = null;
        accessibilityManager = null;
        vibrator = null;
    }
}

