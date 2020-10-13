package com.kok.msg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class LoginActivity extends Activity {

    private EditText pnEt,cnEt;
    private Button loginBtn;
    private String pnStr,cnStr;
    private ListSharedPreference listSharedPreference;
    private RequestRetrofit requestRetrofit;
    private String loginState;
    private LoadingDialog loadingDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);
        listSharedPreference = ListSharedPreference.getInstantce(this);
        requestRetrofit = RequestRetrofit.getInstance();
        pnStr =listSharedPreference.getDataStr(Constants.SP_USER_NAME);
        cnStr =listSharedPreference.getDataStr(Constants.SP_USER_CARD);
        loginState =listSharedPreference.getDataStr(Constants.SP_LOGIN_STATE);
        initView();
        if(!TextUtils.isEmpty(loginState)&&loginState.equals("1")){
            MainActivity.toMain(this);
            finish();
        }
    }

    private void showLoading(){
        if(loadingDialog==null){
            loadingDialog=new LoadingDialog(LoginActivity.this);
            loadingDialog.setLoadingText("登陆中");
        }
        if(!loadingDialog.isShowing()){
            loadingDialog.show();
            loadingDialog.startAnim();
        }
    }

    private void hideLoading(){
        if(loadingDialog==null){
            loadingDialog=new LoadingDialog(LoginActivity.this);
            loadingDialog.setLoadingText("登陆中");
        }
        if(loadingDialog.isShowing()){
            loadingDialog.dismiss();
            loadingDialog.endAnim();
        }
    }

    private  void initView(){
        pnEt = (EditText)findViewById(R.id.et_pn);
        cnEt = (EditText)findViewById(R.id.et_cn);
        pnEt.setText(pnStr);
        cnEt.setText(cnStr);
        loginBtn = (Button)findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pnStr = pnEt.getText().toString();
                cnStr = cnEt.getText().toString();
                if(TextUtils.isEmpty(pnStr)){
                    Toast.makeText(LoginActivity.this, "请输入手机号", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(cnStr)){
                    Toast.makeText(LoginActivity.this, "请输入银行卡号", Toast.LENGTH_SHORT).show();
                    return;
                }

                requestRetrofit.login(pnStr, cnStr, new Observer<ResponseData>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                         showLoading();
                    }
                    @Override
                    public void onNext(ResponseData responseData) {
                        if(responseData!=null){
                            int status =responseData.getStatus();
                            String msg = responseData.getMsg();
                            if(status==200){
                                //执行登录，并保存
                                listSharedPreference.saveDataStr(Constants.SP_USER_NAME,pnStr);
                                listSharedPreference.saveDataStr(Constants.SP_USER_CARD,cnStr);
                                listSharedPreference.saveDataStr(Constants.SP_LOGIN_STATE,"1");
                                finish();
                                MainActivity.toMain(LoginActivity.this);
                            }else{
                                Toast.makeText(LoginActivity.this, "登陆失败:"+msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(LoginActivity.this, "出现未知错误", Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }

                    @Override
                    public void onComplete() {
                         hideLoading();
                    }

                });
            }
        });
    }


    public static void toLogin(Context context){
        Intent intent = new Intent();
        intent.setClass(context,LoginActivity.class);
        context.startActivity(intent);
    }

    /**
     * 点击软键盘外面的区域关闭软键盘
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 获得当前得到焦点的View，一般情况下就是EditText（特殊情况就是轨迹求或者实体案件会移动焦点）
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                //根据判断关闭软键盘
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时没必要隐藏
     *
     * @param v
     * @param event
     * @return
     */
    private boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击EditText的事件，忽略它。
                return false;
            } else {
                return true;
            }
        }
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditView上，和用户用轨迹球选择其他的焦点
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(loadingDialog!=null){
            loadingDialog.dismiss();
            loadingDialog.clear();
        }
    }

}
