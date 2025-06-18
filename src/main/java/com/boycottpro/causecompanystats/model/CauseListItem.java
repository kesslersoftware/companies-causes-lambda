package com.boycottpro.causecompanystats.model;

public class CauseListItem {

    private String cause_id;
    private String cause_desc;
    private int boycottCount;

    public CauseListItem() {
    }

    public CauseListItem(String cause_id, String cause_desc, int boycottCount) {
        this.cause_id = cause_id;
        this.cause_desc = cause_desc;
        this.boycottCount = boycottCount;
    }

    public String getCause_id() {
        return cause_id;
    }

    public void setCause_id(String cause_id) {
        this.cause_id = cause_id;
    }

    public String getCause_desc() {
        return cause_desc;
    }

    public void setCause_desc(String cause_desc) {
        this.cause_desc = cause_desc;
    }

    public int getBoycottCount() {
        return boycottCount;
    }

    public void setBoycottCount(int boycottCount) {
        this.boycottCount = boycottCount;
    }
}
