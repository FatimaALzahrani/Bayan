package com.bank.bayan.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("مرحباً بك في تطبيق بيان البنكي");
    }

    public LiveData<String> getText() {
        return mText;
    }
}

