package com.kok.msg;

/**
 * @date 20200709
 * 短信实体类--用于保存过滤的短信收件箱
 */
public class SmsEntity {

    public String _id;//pduid _id+data
    public String thread_id;//回话id
    public String address;//对方号码
    public String type;//短信类型
    public String date;//短信时间
    public String body;//短信内容
    public int state =0;//短信状态，0表示未发送到服务端 1表示已经发送到服务端
    public boolean isOutDate =false;//短信是否过期
    public boolean isUploadError =false;//true：上传失败过，false：没有上传失败过。是否上传失败过

    public boolean isUploadError() {
        return isUploadError;
    }

    public void setUploadError(boolean uploadError) {
        isUploadError = uploadError;
    }

    public int getState() {
        return state;
    }

    public boolean isOutDate() {
        return isOutDate;
    }

    public void setOutDate(boolean outDate) {
        isOutDate = outDate;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getThread_id() {
        return thread_id;
    }

    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
