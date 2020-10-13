package com.kok.msg;

/**
 * @data 20200709
 * 银行卡信息
 */
public class BankCardEntity {

    /**
     * bank_title : 银行名称
     * card_number : 银行卡卡号
     * card_name : 持卡人姓名
     */
    private String bank_title;
    private String card_number;
    private String card_name;

    public void setBank_title(String bank_title) {
        this.bank_title = bank_title;
    }

    public void setCard_number(String card_number) {
        this.card_number = card_number;
    }

    public void setCard_name(String card_name) {
        this.card_name = card_name;
    }

    public String getBank_title() {
        return bank_title;
    }

    public String getCard_number() {
        return card_number;
    }

    public String getCard_name() {
        return card_name;
    }

}
