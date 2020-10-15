package com.kok.msg;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kok.msg.gen.SmsEntityDao;
import com.kok.msg.utils.GreenDaoManager;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MainActivity extends Activity implements MsgAdapter.OnMsgClick {

    private SMSContentObserver smsContentObserver;
    private SmsHandler smsHandler = null;
    private RecyclerView recyclerView;
    private List<SmsEntity> smsEntityList = new ArrayList<>();
    private ListSharedPreference listSharedPreference;
    private RequestRetrofit requestRetrofit;
    private List<BankCardEntity> bankCardEntityList;
    private List<BankNumberEntity> bankNumberEntityList;
    private String sign;
    private IntentFilter filter;
    private LoadingDialog loadingDialog;

    public static final int MSG_OUT_MINUTES = 15;//短信过期时间
    private SmartRefreshLayout refreshView;

    //    private SmsReceiver receiver;
    @Override
    public void msgBtnClick(MsgAdapter.ViewHolder holder, int position) {
        try {
            SmsEntity smsEntity = smsEntityList.get(position);
            if (smsEntity != null && smsEntity.getState() == 0) {
                sign = Md5Tools.getMD5(smsEntity.getAddress() + listSharedPreference.getDataStr(Constants.SP_USER_NAME));
                sendMsg(smsEntity, sign);
            }
        } catch (Exception exeption) {
            exeption.printStackTrace();
        }
    }

    private void exitLogin() {
        listSharedPreference.saveDataStr(Constants.SP_LOGIN_STATE, "0");
        RequestRetrofit.cookie = null;
        //跳转到登陆页面
        finish();
        LoginActivity.toLogin(MainActivity.this);
    }

    private static class SmsHandler extends Handler {
        WeakReference<MainActivity> mainActivityReference = null;

        public SmsHandler(MainActivity mainActivity) {
            super();
            mainActivityReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == SMSContentObserver.MSG_INBOX) {
                MainActivity mainActivity = mainActivityReference.get();
                if (mainActivity != null) {
                    Toast.makeText(mainActivity, "短信接收箱发生变化", Toast.LENGTH_LONG).show();
                    mainActivity.smsBoxChange();
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestRetrofit = RequestRetrofit.getInstance();
        listSharedPreference = ListSharedPreference.getInstantce(this);
        smsHandler = new SmsHandler(this);
        smsContentObserver = new SMSContentObserver(this, smsHandler);
        getCardList();
        findViewById(R.id.btn_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCardList();
            }
        });
    }

    private int statusFailureCount = 0;
    private static final int RETRY_COUNT = 3;

    private void getCardPhoneList(String hot_line) {
        requestRetrofit.getBankPhoneList(hot_line, new Observer<ResponseData>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ResponseData responseData) {
                if (responseData != null) {
                    int status = responseData.getStatus();
                    String msg = responseData.getMsg();
                    JsonObject datJson = responseData.getData();
                    if (status == 200) {
                        hideLoading();
                        statusFailureCount = 0;
                        if (datJson != null) {
                            JsonArray jsonArray = datJson.getAsJsonArray("bank_list");
                            if (jsonArray != null && jsonArray.size() != 0) {
                                Gson gson = new Gson();
                                bankNumberEntityList = gson.fromJson(jsonArray, new TypeToken<List<BankNumberEntity>>() {
                                }.getType());
                            }
                        }
                        //所有拿到才去申请权限
                        if (Build.VERSION.SDK_INT > 21) {
                            if (PermissionsUtil.hasPermission(MainActivity.this, new String[]{Manifest.permission.READ_SMS})) {
                                Toast.makeText(MainActivity.this, "进入到21-1", Toast.LENGTH_SHORT);
                                if (bankCardEntityList == null || bankNumberEntityList == null) {
                                    return;
                                }
                                Toast.makeText(MainActivity.this, "进入到21", Toast.LENGTH_SHORT);
                                sms();
                            } else {
                                PermissionsUtil.requestPermission(MainActivity.this, new PermissionListener() {
                                    @Override
                                    public void permissionGranted(@NonNull String[] permissions) {
                                        if (bankCardEntityList == null || bankNumberEntityList == null) {
                                            return;
                                        }
                                        sms();
                                    }

                                    @Override
                                    public void permissionDenied(@NonNull String[] permissions) {
                                        Toast.makeText(MainActivity.this, "用户拒绝了读取消息权限", Toast.LENGTH_LONG).show();
                                        System.exit(0);
                                    }
                                }, new String[]{Manifest.permission.READ_SMS}, false, null);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "进入到无动态权限", Toast.LENGTH_SHORT);
                            if (bankCardEntityList == null || bankNumberEntityList == null) {
                                return;
                            }
                            sms();
                        }
                    } else if (status == 600) {
                        statusFailureCount++;
                        ListSharedPreference sharedPreference = ListSharedPreference.getInstantce(MainActivity.this);
                        final String phone = sharedPreference.getDataStr(Constants.SP_USER_NAME);
                        String card = sharedPreference.getDataStr(Constants.SP_USER_CARD);
                        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(card) || statusFailureCount > RETRY_COUNT) {
                            hideLoading();
                            exitLogin();
                            return;
                        }
                        //执行登陆操作
                        requestRetrofit.login(phone, card, new Observer<ResponseData>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(ResponseData responseData) {
                                if (responseData != null) {
                                    int status = responseData.getStatus();
                                    String msg = responseData.getMsg();
                                    if (status == 200) {
                                        getCardPhoneList("");
                                    } else {
                                        hideLoading();
                                        exitLogin();
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                hideLoading();
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                    } else {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "出现其他错误:" + msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "没有返回任何数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                hideLoading();
                Toast.makeText(MainActivity.this, "网络请求出现了错误", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 获取银行卡列表
     */
    private void getCardList() {
        requestRetrofit.getCardList(new Observer<ResponseData>() {
            @Override
            public void onSubscribe(Disposable d) {
                showLoading();
            }

            @Override
            public void onNext(ResponseData responseData) {
                if (responseData != null) {
                    int status = responseData.getStatus();
                    String msg = responseData.getMsg();
                    JsonObject datJson = responseData.getData();
                    if (status == 200) {
                        statusFailureCount = 0;
                        if (datJson != null) {
                            JsonArray jsonArray = datJson.getAsJsonArray("card_list");
                            if (jsonArray != null && jsonArray.size() != 0) {
                                Gson gson = new Gson();
                                bankCardEntityList = gson.fromJson(jsonArray, new TypeToken<List<BankCardEntity>>() {
                                }.getType());
                            }
                        }
                        getCardPhoneList("");
                    } else if (status == 600) {
                        statusFailureCount++;
                        ListSharedPreference sharedPreference = ListSharedPreference.getInstantce(MainActivity.this);
                        String phone = sharedPreference.getDataStr(Constants.SP_USER_NAME);
                        String card = sharedPreference.getDataStr(Constants.SP_USER_CARD);
                        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(card) || statusFailureCount > RETRY_COUNT) {
                            hideLoading();
                            exitLogin();
                            return;
                        }
                        //执行登陆操作
                        requestRetrofit.login(phone, card, new Observer<ResponseData>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(ResponseData responseData) {
                                if (responseData != null) {
                                    int status = responseData.getStatus();
                                    String msg = responseData.getMsg();
                                    hideLoading();
                                    if (status == 200) {
                                        getCardList();
                                    } else {
                                        exitLogin();
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                hideLoading();
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "出现其他错误:" + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                hideLoading();
                if (refreshView.isRefreshing()) {
                    refreshView.finishRefresh();
                }
            }

            @Override
            public void onComplete() {
                if (refreshView.isRefreshing()) {
                    refreshView.finishRefresh();
                }
            }
        });
    }


    /**
     * 上传短信到服务器
     *
     * @param sign
     */
    private void sendMsg(final SmsEntity entity, final String sign) {
        requestRetrofit.postMsg(entity.getBody(), entity.getAddress(), sign, new Observer<ResponseData>() {
            @Override
            public void onSubscribe(Disposable d) {
                showLoading();
            }

            @Override
            public void onNext(ResponseData responseData) {
                if (responseData != null) {
                    int status = responseData.getStatus();
                    String msg = responseData.getMsg();
                    if (status == 200) {
                        hideLoading();
                        statusFailureCount = 0;
                        //处理成功则保存该短信的处理状态
                        entity.setState(1);
                        GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().update(entity);

                        recyclerView.getAdapter().notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "已发送处理", Toast.LENGTH_SHORT).show();
                    } else if (status == 600) {
                        statusFailureCount++;
                        ListSharedPreference sharedPreference = ListSharedPreference.getInstantce(MainActivity.this);
                        final String phone = sharedPreference.getDataStr(Constants.SP_USER_NAME);
                        String card = sharedPreference.getDataStr(Constants.SP_USER_CARD);
                        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(card) || statusFailureCount > RETRY_COUNT) {
                            hideLoading();
                            exitLogin();
                            return;
                        }
                        //执行登陆操作
                        requestRetrofit.login(phone, card, new Observer<ResponseData>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(ResponseData responseData) {
                                hideLoading();
                                if (responseData != null) {
                                    int status = responseData.getStatus();
                                    String msg = responseData.getMsg();
                                    if (status == 200) {
                                        sendMsg(entity, sign);
                                    } else {
                                        exitLogin();
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                hideLoading();
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
                    } else {
                        hideLoading();
                        updateFaile(entity);

                        Toast.makeText(MainActivity.this, "出现其他错误:" + msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateFaile(entity);

                    hideLoading();
                    Toast.makeText(MainActivity.this, "没有返回任何数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                hideLoading();
                updateFaile(entity);

                Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void updateFaile(SmsEntity entity) {
        int failCount = entity.getFailCount();
        int count = failCount + 1;
        //设置状态为失败
        entity.setState(2);
        //设置失败的次数
        entity.setFailCount(count);
        SmsEntity smsEntityDb = GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().load(entity.get_id());
        smsEntityDb.setState(2);
        smsEntityDb.setFailCount(count);
        GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().update(smsEntityDb);

        getSmsData();
    }

    public static void toMain(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        context.startActivity(intent);
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        /*StaggeredGridLayoutManager layoutManager = new
                StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);*/
        recyclerView.setLayoutManager(layoutManager);
        MsgAdapter adapter = new MsgAdapter(smsEntityList);
        recyclerView.setAdapter(adapter);
        adapter.setOnMsgClick(this);
        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitLogin();
            }
        });
        refreshView = findViewById(R.id.refreshView);
        refreshView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                getCardList();
            }
        });
    }

    private void initData() {

    }

    public void smsBoxChange() {
        sms();
    }

    /**
     * 获取系统短信
     */
    public synchronized void sms() {

        //smsEntityList.clear();
        //获取内容解析器（ContentResolver）
        ContentResolver resolver = getContentResolver();
        //定义一个URI（Uniform Resource Identifier 统一资源标示符）
        //查全部短信
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = resolver.query(uri, null, null, null, "date desc");

        int index = 0;
        while (cursor.moveToNext()) {
            //解析短息内容
            String _id = cursor.getString(cursor.getColumnIndex("_id"));
            String thread_id = cursor.getString(cursor.getColumnIndex("thread_id"));
            String type = cursor.getString(cursor.getColumnIndex("type"));
            String address = cursor.getString(cursor.getColumnIndex("address"));
            String body = cursor.getString(cursor.getColumnIndex("body"));
            String dateString = cursor.getString(cursor.getColumnIndex("date"));

            Long dateLong = Long.parseLong(dateString);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(dateLong);
            String dateShow = sdf.format(date);
            SmsEntity smsEntity = new SmsEntity();
//            String idDate = (_id + dateLong).trim();
            smsEntity.set_id(dateString);
            smsEntity.setThread_id(thread_id);
            smsEntity.setAddress(address);
            smsEntity.setDate(dateShow);
            smsEntity.setDateLong(dateLong);
            smsEntity.setBody(body);
            smsEntity.setType(type);

            //过滤短信，只获取指定号码的
            if (filterSms(smsEntity, bankNumberEntityList)) {

                SmsEntity smsEntityDb = GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().load(smsEntity.get_id());

                //短信是否过期
                if (isBefore(new Date(dateLong))) {
                    //已经超时了
                    if (smsEntityDb == null) {
                        smsEntity.setState(3);
                        GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().insert(smsEntity);
                    }else{
                        if(smsEntityDb.getState()==0){
                            smsEntityDb.setState(3);
                            GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().update(smsEntityDb);
                        }
                    }
                } else {
                    if (smsEntityDb == null) {

                        //未上传过
                        smsEntity.setState(0);
                        //符合条件的短信，保存到数据库
                        GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().insert(smsEntity);

                    }
                }
            }
        }

        getSmsData();

//        Toast.makeText(MainActivity.this, "短信总数:" + smsEntityList.size(), Toast.LENGTH_LONG).show();
        cursor.close();

        for (int i = 0; i < smsEntityList.size(); i++) {
            SmsEntity entity = smsEntityList.get(i);

            if ((entity.state == 0 || entity.state == 2) && !isBefore(new Date(entity.dateLong))) {
                Toast.makeText(MainActivity.this, "处理短信：" + entity.getAddress(), Toast.LENGTH_SHORT).show();
                try {
                    sign = Md5Tools.getMD5(entity.getAddress() + listSharedPreference.getDataStr(Constants.SP_USER_NAME));
                    sendMsg(entity, sign);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isBefore(Date date) {
        //短信是否过期
        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.MINUTE, 0 - MSG_OUT_MINUTES);

        Calendar smsTime = Calendar.getInstance();
        smsTime.setTime(date);

        return smsTime.before(currentTime);
    }

    /**
     * 从数据库获取短信数据
     */
    private void getSmsData() {
        List<SmsEntity> smsEntities = GreenDaoManager.getInstance().getNewSession().getSmsEntityDao().queryBuilder().orderDesc(SmsEntityDao.Properties.DateLong).list();
        if (smsEntities != null && smsEntities.size() > 0) {
            smsEntityList.clear();
            smsEntityList.addAll(smsEntities);
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    /**
     * 短信过滤
     */
    private boolean filterSms(SmsEntity smsEntity, List<BankNumberEntity> bankNumberEntityList) {

        String address = smsEntity.getAddress();
        if (address.substring(0, 3).contains("+86")) {
            address = address.replace("+86", "").trim();
        } else if (address.substring(0, 3).contains("86")) {
            address = address.replace("86", "").trim();
        }
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        for (int i = 0; i < bankNumberEntityList.size(); i++) {
            String bankNumber = bankNumberEntityList.get(i).getHot_line().trim();
            if (address.equals(bankNumber)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (smsContentObserver != null) {
            getContentResolver().registerContentObserver(
                    Uri.parse("content://sms"), true, smsContentObserver);// 注册监听短信数据库的变化
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog.clear();
        }

        if (smsContentObserver != null) {
            getContentResolver().unregisterContentObserver(smsContentObserver);// 取消监听短信数据库的变化
        }
    }

    private void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(MainActivity.this);
            loadingDialog.setLoadingText("处理中");
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
            loadingDialog.startAnim();
        }
    }

    private void hideLoading() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(MainActivity.this);
            loadingDialog.setLoadingText("处理中");
        }
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog.endAnim();
        }
    }
}