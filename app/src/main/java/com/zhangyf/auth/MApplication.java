package com.zhangyf.auth;

import android.app.Application;

import com.zhangyf.library.utils.SPUtils;
import com.zhangyf.library.utils.TotpUtil;

/**
 * Created by zhangyf on 2017/3/8.
 */

public class MApplication extends Application{

    private static MApplication instance = null;

    public static MApplication getInstance() {
        if (null == instance) {
            instance = new MApplication();
        }
        return instance;
    }

    public MApplication() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SPUtils.init(this);
        TotpUtil.init("FZ6S5VB64HVSYLJN");
    }
}
