package com.boycottpro.causecompanystats.model;

public class CauseListItem {

    private String cause_id;
    private String cause_desc;
    private int boycott_count;

    public CauseListItem() {
    }

    public CauseListItem(String cause_id, String cause_desc, int boycott_count) {
        this.cause_id = cause_id;
        this.cause_desc = cause_desc;
        this.boycott_count = boycott_count;
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

    public int getBoycott_count() {
        return boycott_count;
    }

    public void setBoycott_count(int boycott_count) {
        this.boycott_count = boycott_count;
    }
}
