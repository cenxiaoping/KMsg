package com.kok.msg;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

//1,创建LoadingDialog继承Dialog并实现构造方法
public class LoadingDialog extends Dialog {

       private AVLoadingIndicatorView loadingIndicatorView;
       private TextView loadingTv;
        public LoadingDialog(Context context) {
            super(context);
            /**设置对话框背景透明*/
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setContentView(R.layout.dialog_loading);
            setCanceledOnTouchOutside(false);
            loadingIndicatorView = findViewById(R.id.avloading);
            loadingTv = findViewById(R.id.tv_loading);
        }

        public void startAnim(){
            if(loadingIndicatorView!=null){
                loadingIndicatorView.show();
            }
        }

    public void endAnim(){
        if(loadingIndicatorView!=null){
            loadingIndicatorView.hide();
        }
    }

    public void clear(){
         if(loadingIndicatorView!=null){
               loadingIndicatorView.clearAnimation();
               loadingIndicatorView=null;
         }
    }

    public void setLoadingText(String loadingText){
            if(loadingTv!=null){
                loadingTv.setText(loadingText+"......");
            }
    }
}