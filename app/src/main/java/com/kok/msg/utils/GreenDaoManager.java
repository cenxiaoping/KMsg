package com.kok.msg.utils;

import com.kok.msg.App;
import com.kok.msg.gen.DaoMaster;
import com.kok.msg.gen.DaoSession;

public class GreenDaoManager {
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private static GreenDaoManager INSTANCE;

    private GreenDaoManager() {
        init();
    }

    /**
     * 对外唯一实例的接口
     *
     * @return
     */
    public static synchronized GreenDaoManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GreenDaoManager();
        }
        return INSTANCE;
    }

    /**
     * 初始化数据
     */
    private void init() {
        DaoMaster.DevOpenHelper devOpenHelper = new DaoMaster.DevOpenHelper(App.getContext(),
                "ksmsDb");
        mDaoMaster = new DaoMaster(devOpenHelper.getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();
    }

    public DaoMaster getmDaoMaster() {
        return mDaoMaster;
    }

    public DaoSession getmDaoSession() {
        return mDaoSession;
    }

    public DaoSession getNewSession() {
        mDaoSession = mDaoMaster.newSession();
        return mDaoSession;
    }

    public void close() {
        mDaoSession.clear();
    }
}
