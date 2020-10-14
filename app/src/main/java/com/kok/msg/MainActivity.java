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
import com.kok.msg.utils.GreenDaoManager;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
                sendMsg(smsEntity.get_id(), smsEntity.getAddress(), smsEntity.getBody(), smsEntity.getAddress(), sign);
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


    private void sendMsg(final String _id, final String date, final String content, final String sendPhone, final String sign) {
        requestRetrofit.postMsg(content, sendPhone, sign, new Observer<ResponseData>() {
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
                        List<String> dataDidList = listSharedPreference.getDataList(Constants.SP_MSG);
                        dataDidList.add(_id);
                        listSharedPreference.saveDataList(Constants.SP_MSG, dataDidList);
                        //更新对应列表
                        for (int i = 0; i < smsEntityList.size(); i++) {
                            String id = smsEntityList.get(i).get_id();
                            if (id.equals(_id)) {
                                smsEntityList.get(i).setState(1);
                                recyclerView.getAdapter().notifyDataSetChanged();
                                break;
                            }
                        }
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
                                        sendMsg(_id, date, content, sendPhone, sign);
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
                        List<String> dataDidList = listSharedPreference.getDataList(Constants.SP_ERROR);
                        dataDidList.add(_id);
                        listSharedPreference.saveDataList(Constants.SP_ERROR, dataDidList);

                        Toast.makeText(MainActivity.this, "出现其他错误:" + msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    List<String> dataDidList = listSharedPreference.getDataList(Constants.SP_ERROR);
                    dataDidList.add(_id);
                    listSharedPreference.saveDataList(Constants.SP_ERROR, dataDidList);

                    hideLoading();
                    Toast.makeText(MainActivity.this, "没有返回任何数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                hideLoading();
                List<String> dataDidList = listSharedPreference.getDataList(Constants.SP_ERROR);
                dataDidList.add(_id);
                listSharedPreference.saveDataList(Constants.SP_ERROR, dataDidList);

                Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        });
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
            String idDate = (_id + dateLong).trim();
            smsEntity.set_id(idDate);
            smsEntity.setThread_id(thread_id);
            smsEntity.setAddress(address);
            smsEntity.setDate(dateShow);
            smsEntity.setBody(body);
            smsEntity.setType(type);

            //短信是否过期
            Calendar currentTime = Calendar.getInstance();
            currentTime.add(Calendar.MINUTE, -15);

            Calendar smsTime = Calendar.getInstance();
            smsTime.setTime(new Date(dateLong));

            if (smsTime.before(currentTime)) {
                //已经超时了
                smsEntity.setState(3);
            } else {
                SmsEntity smsEntityDb = GreenDaoManager.getInstance().getmDaoSession().getSmsEntityDao().load(smsEntity.get_id());
                if (smsEntityDb != null) {
                } else {
                    //未上传过
                    smsEntity.setState(0);
                }
            }

            //暂时注释掉
//            if (!filterSms(smsEntity, bankCardEntityList, bankNumberEntityList)) {
//                index++;
//                continue;
//            } else {
//                index++;
//            }


//            int status = getState(smsEntity.get_id());
//
//            //判断状态
//            smsEntity.setState(status);
            //上传状态
//            smsEntity.setUploadError(getUploadState(smsEntity.get_id()));
            int length = smsEntityList.size();
            if (index < length) {
                smsEntityList.add(index - 1, smsEntity);
            } else {
                smsEntityList.add(smsEntity);
            }
            //未提交 且没有过期的会提交
//            if (smsEntity != null && status == 0 && !smsEntity.isOutDate) {
//                try {
//                    Toast.makeText(MainActivity.this, "处理短信：" + address, Toast.LENGTH_SHORT).show();
//                    sign = Md5Tools.getMD5(smsEntity.getAddress() + listSharedPreference.getDataStr(Constants.SP_USER_NAME));
//                    sendMsg(smsEntity.get_id(), smsEntity.getAddress(), smsEntity.getBody(), smsEntity.getAddress(), sign);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
        Toast.makeText(MainActivity.this, "短信总数:" + smsEntityList.size(), Toast.LENGTH_LONG).show();
        cursor.close();
    }


//    private SmsEntity getState(String id) {
//        return GreenDaoManager.getInstance().getmDaoSession().getSmsEntityDao().load(id);

//        List list = listSharedPreference.getDataList(Constants.SP_MSG);
//        if (list == null || list.size() == 0) {
//            return 0;
//        }
//        for (int i = 0; i < list.size(); i++) {
//            String pId = (String) list.get(i);
//            if (pId.equals(idDate)) {
//                return 1;
//            }
//        }
//        return 0;
//    }

    private boolean getUploadState(String idDate) {
        List list = listSharedPreference.getDataList(Constants.SP_ERROR);
        if (list == null || list.size() == 0) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            String pId = (String) list.get(i);
            if (pId.equals(idDate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 短信过滤
     */
    private boolean filterSms(SmsEntity smsEntity, List<BankCardEntity> bankCardEntityList, List<BankNumberEntity> bankNumberEntityList) {
//        Log.d("", "短信过滤中:" + address);
//        Toast.makeText(MainActivity.this,"短信过滤中:"+address,Toast.LENGTH_SHORT).show();
//        Toast.makeText(MainActivity.this,"短信过滤中:"+body,Toast.LENGTH_SHORT).show();
        if (TextUtils.isEmpty(smsEntity.getAddress()) || TextUtils.isEmpty(smsEntity.getBody()) || smsEntity.getAddress().length() < 4) {
            return false;
        }
        //如果短信已经存在列表中
        for (int i = 0; i < smsEntityList.size(); i++) {
            if (smsEntity.get_id().equals(smsEntityList.get(i).get_id())) {
                //则只更新对应时间
                //短信是否过期
                SmsEntity smsEntityTemp = smsEntityList.get(i);
                int msgDate = (int) (DateUtils.strToDateLong(smsEntity.getDate()).getTime() / 60000);
                int currentDate = (int) (System.currentTimeMillis() / 60000);//分钟
//                smsEntityTemp.setOutDate((currentDate - msgDate) > MSG_OUT_MINUTES ? true : false);
                return false;
            }
        }

//        if (address.substring(0, 3).contains("+86")) {
//            address = address.replace("+86", "").trim();
//        } else if (address.substring(0, 3).contains("86")) {
//            address = address.replace("86", "").trim();
//        }
//        if (TextUtils.isEmpty(address)) {
//            return false;
//        }
//        for (int i = 0; i < bankNumberEntityList.size(); i++) {
//            String bankNumber = bankNumberEntityList.get(i).getHot_line().trim();
//            if (address.equals(bankNumber)) {
//                return true;
//            }
//        }
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
        if (smsContentObserver != null) {
            getContentResolver().unregisterContentObserver(smsContentObserver);// 取消监听短信数据库的变化
        }

    }

//    public class SmsReceiver extends BroadcastReceiver {
//
//        @RequiresApi(api = Build.VERSION_CODES.M)
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            StringBuilder content = new StringBuilder();//用于存储短信内容
//            String sender = null;//存储短信发送方手机号
//            Bundle bundle = intent.getExtras();//通过getExtras()方法获取短信内容
//            String format = intent.getStringExtra("format");
//            if (bundle != null) {
//                Object[] pdus = (Object[]) bundle.get("pdus");//根据pdus关键字获取短信字节数组，数组内的每个元素都是一条短信
//                for (Object object : pdus) {
//                    SmsMessage message = SmsMessage.createFromPdu((byte[]) object, format);//将字节数组转化为Message对象
//                    sender = message.getOriginatingAddress();//获取短信手机号
//                    content.append(message.getMessageBody());//获取短信内容
//                }
//            }
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog.clear();
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