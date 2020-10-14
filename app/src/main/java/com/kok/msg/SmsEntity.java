package com.kok.msg;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * @date 20200709
 * 短信实体类--用于保存过滤的短信收件箱
 */
@Entity
public class SmsEntity {
    @Id
    public String _id;//pduid _id+data
    public String thread_id;//回话id
    public String address;//对方号码
    public String type;//短信类型
    public String date;//短信时间
    public String body;//短信内容
    public int state =0;//短信状态，0表示未发送到服务端,1表示已经发送到服务端,2表示上传失败，3表示短信已过期
    public int uploadCount;//上传次数
    @Generated(hash = 96682159)
    public SmsEntity(String _id, String thread_id, String address, String type,
            String date, String body, int state, int uploadCount) {
        this._id = _id;
        this.thread_id = thread_id;
        this.address = address;
        this.type = type;
        this.date = date;
        this.body = body;
        this.state = state;
        this.uploadCount = uploadCount;
    }
    @Generated(hash = 1127714058)
    public SmsEntity() {
    }
    public String get_id() {
        return this._id;
    }
    public void set_id(String _id) {
        this._id = _id;
    }
    public String getThread_id() {
        return this.thread_id;
    }
    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getDate() {
        return this.date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getBody() {
        return this.body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public int getState() {
        return this.state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public int getUploadCount() {
        return this.uploadCount;
    }
    public void setUploadCount(int uploadCount) {
        this.uploadCount = uploadCount;
    }

}
