package com.kok.msg;

import com.google.gson.JsonObject;

public class ResponseData {

    /**
     * Status : 200
     * Msg : success
     * Data : {}
     */
    private int Status;
    private String Msg;
    private JsonObject Data;

    public void setStatus(int Status) {
        this.Status = Status;
    }

    public void setMsg(String Msg) {
        this.Msg = Msg;
    }

    public void setData(JsonObject Data) {
        this.Data = Data;
    }

    public int getStatus() {
        return Status;
    }

    public String getMsg() {
        return Msg;
    }

    public JsonObject getData() {
        return Data;
    }

}
