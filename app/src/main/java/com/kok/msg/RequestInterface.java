package com.kok.msg;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RequestInterface {

    @Multipart
    @POST("pay/login.do")
    Observable<ResponseData> login(
            @Part("phone") RequestBody phone,
            @Part("card_number") RequestBody cardNumber
    );

    @Multipart
    @POST("app/card_list.do")
    Observable<ResponseData> getCardList(@Part("no") RequestBody no);

    @Multipart
    @POST("app/notify_pay.do")
    Observable<ResponseData> sendBoxMsg(
            @Part("content") RequestBody content,
            @Part("msg_number") RequestBody phone, @Part("sign") RequestBody sign);

    //获取银行号码
    @Multipart
    @POST("app/bank_list.do")
    Observable<ResponseData> getBankNumberList(@Part("hot_line") RequestBody hot_line);

//    @POST
//    @FormUrlEncoded
//    Call<Object> login3(@Url String url,
//                        @Field("usename") String usename,
//                        @Field("password") String password);
//
//    @Streaming
//    @GET
//    Observable<ResponseBody> download(@Header("RANGE") String start, @Url String url);
}
