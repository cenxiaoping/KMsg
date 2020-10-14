package com.kok.msg;

import android.app.Application;
import android.content.Context;

import com.kok.msg.utils.GreenDaoManager;

import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        SQLiteStudioService.instance().start(this);
        //GreenDao的初始化
        GreenDaoManager.getInstance();
    }

    public static Context getContext() {
        return context;
    }
}
