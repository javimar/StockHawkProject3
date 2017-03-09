package com.udacity.stockhawk.data;


public class HistoryEntries
{
    private String mDate;
    private float mPrice;

    public HistoryEntries(String mDate, float mPrice)
    {
        this.mDate = mDate;
        this.mPrice = mPrice;
    }

    public String getmDate() {
        return mDate;
    }

    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    public float getmPrice() {
        return mPrice;
    }

    public void setmPrice(float mPrice) {
        this.mPrice = mPrice;
    }
}
