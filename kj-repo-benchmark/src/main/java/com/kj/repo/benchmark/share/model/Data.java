package com.kj.repo.benchmark.share.model;

/**
 * @author kj
 * Created on 2020-04-01
 */
public class Data extends Model {
    private long globalId;
    private int sourceKey;
    private String sourceId;
    private int status;
    private int version;
    private String data;
    private String extra;

    public long getGlobalId() {
        return globalId;
    }

    public void setGlobalId(long globalId) {
        this.globalId = globalId;
    }

    public int getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(int sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
