package com.kok.msg;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于存储和读取列表数据
 */
public class ListSharedPreference {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private ListSharedPreference(Context mContext) {
        preferences = mContext.getSharedPreferences(Constants.SP_LEVEL, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    private static ListSharedPreference listSharedPreference;

    public static ListSharedPreference getInstantce(Context context) {
        if (listSharedPreference == null) {
            listSharedPreference = new ListSharedPreference(context.getApplicationContext());
        }
        return listSharedPreference;
    }

    public void saveDataStr(String tag, String str) {
        editor.putString(tag, str);
        editor.commit();
    }

    public String getDataStr(String tag) {
        return preferences.getString(tag, null);
    }

    /**
     * 保存List
     *
     * @param tag
     * @param datalist
     */
    public <T> void saveDataList(String tag, List<T> datalist) {
        if (null == datalist || datalist.size() <= 0)
            return;
        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(datalist);
        editor.putString(tag, strJson);
        editor.commit();
    }

    /**
     * 获取List
     *
     * @param tag
     * @return
     */
    public <T> List<T> getDataList(String tag) {
        List<T> datalist = new ArrayList<T>();
        String strJson = preferences.getString(tag, null);
        if (null == strJson) {
            return datalist;
        }
        Gson gson = new Gson();
        List<T> tempList = gson.fromJson(strJson, new TypeToken<List<T>>() {
        }.getType());
        if (tempList == null) {
            return datalist;
        }
        datalist.addAll(tempList);
        return datalist;
    }

}
