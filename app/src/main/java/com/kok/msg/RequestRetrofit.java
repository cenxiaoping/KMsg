package com.kok.msg;

import android.app.Application;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求Retrofit
 */
public class RequestRetrofit {

    private Retrofit retrofit;
    private OkHttpClient okHttpClient;
    private static RequestRetrofit requestRetrofit;
    private static final  int READ_TIMEOUT =10;
    private static final int WRITE_TIMEOUT=10;
    private static final int CONNECT_TIMEOUT=10;
    private RequestInterface requestInterface;
    public static String cookie=null;
    private RequestRetrofit (){
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Request.Builder builder =  request.newBuilder();
                        if(cookie!=null){
                            builder.addHeader("cookie", cookie);
//                            if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) {
//                                builder.addHeader("Connection", "close");
//                            }
                        }
                        Response response =chain.proceed(builder.build());
                        if(cookie==null){
                            if(response.header("set-cookie")!=null){
                                cookie=response.header("set-cookie");
                                log("tag1","set-cookie:"+cookie);
                                if(!TextUtils.isEmpty(cookie)){
                                    cookie = cookie.substring(0, cookie.indexOf(";"));
                                    log("tag1","cookie:"+cookie);
                                }
                            }
                        }

                        return response;
                    }
                })
//                .cookieJar(new CookiesManager())
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

    }


    public static void log(String tag, String msg) {  //信息太长,分段打印
        //因为String的length是字符数量不是字节数量所以为了防止中文字符过多，
        //  把4*1024的MAX字节打印长度改为2001字符数
        int max_str_length = 2001 - tag.length();
        //大于4000时
        while (msg.length() > max_str_length) {
            Log.i(tag, msg.substring(0, max_str_length));
            msg = msg.substring(max_str_length);
        }
        //剩余部分
        Log.i(tag, msg);
    }

    public static RequestRetrofit getInstance() {
        if(requestRetrofit==null){
            requestRetrofit = new RequestRetrofit();
        }
        return requestRetrofit;
    }

    private RequestInterface getRequestInterface(){
        if(requestInterface==null){
            requestInterface = retrofit.create(RequestInterface.class);
        }
        return requestInterface;
    }

    public void login(String phone, String card, Observer<ResponseData> observer){

        RequestInterface requestInterface =getRequestInterface();
        Observable<ResponseData> reponseDataObservable =  requestInterface.login(RequestBody.create(MediaType.parse("multipart/form-data"),phone),
                RequestBody.create(MediaType.parse("multipart/form-data"),card));
        reponseDataObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }


    public void getCardList(Observer<ResponseData> observer){
        RequestInterface requestInterface =getRequestInterface();
        Observable<ResponseData> reponseDataObservable =  requestInterface.getCardList(RequestBody.create(MediaType.parse("multipart/form-data"),"123"));
        reponseDataObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void postMsg(String content, String phone,String sign,Observer<ResponseData> observer){
        RequestInterface requestInterface =getRequestInterface();
        Observable<ResponseData> reponseDataObservable =  requestInterface.sendBoxMsg(RequestBody.create(MediaType.parse("multipart/form-data"),content),
                RequestBody.create(MediaType.parse("multipart/form-data"),phone),
                RequestBody.create(MediaType.parse("multipart/form-data"),sign));
        reponseDataObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void getBankPhoneList(String hotline,Observer<ResponseData> observer){
        RequestInterface requestInterface =getRequestInterface();
        Observable<ResponseData> reponseDataObservable =  requestInterface.getBankNumberList(RequestBody.create(MediaType.parse("multipart/form-data"),hotline));
        reponseDataObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

}
