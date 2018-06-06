package com.mstarc.record.wearablerecorder.bean;

/**
 * Created by liuqing
 * 18-1-3.
 * Email: 1239604859@qq.com
 */

public class RecordBus {
    private boolean startRecord;
    private boolean pauseRecord;
    private boolean startOrPauseRecord;
    private boolean stopRecord;

    private boolean startPlay;
    private boolean pausePlay;
    private boolean startOrPausePlay;

    public boolean isStartRecord() {
        return startRecord;
    }

    public RecordBus setStartRecord(boolean startRecord) {
        this.startRecord = startRecord;
        return this;
    }

    public boolean isPauseRecord() {
        return pauseRecord;
    }

    public RecordBus setPauseRecord(boolean pauseRecord) {
        this.pauseRecord = pauseRecord;
        return this;
    }

    public boolean isStartOrPauseRecord() {
        return startOrPauseRecord;
    }

    public RecordBus setStartOrPauseRecord(boolean startOrPauseRecord) {
        this.startOrPauseRecord = startOrPauseRecord;
        return this;
    }

    public boolean isStopRecord() {
        return stopRecord;
    }

    public RecordBus setStopRecord(boolean stopRecord) {
        this.stopRecord = stopRecord;
        return this;
    }

    public boolean isStartPlay() {
        return startPlay;
    }

    public RecordBus setStartPlay(boolean startPlay) {
        this.startPlay = startPlay;
        return this;
    }

    public boolean isPausePlay() {
        return pausePlay;
    }

    public RecordBus setPausePlay(boolean pausePlay) {
        this.pausePlay = pausePlay;
        return this;
    }

    public boolean isStartOrPausePlay() {
        return startOrPausePlay;
    }

    public RecordBus setStartOrPausePlay(boolean startOrPausePlay) {
        this.startOrPausePlay = startOrPausePlay;
        return this;
    }

    @Override
    public String toString() {
        return "RecordBus{" +
                "startRecord=" + startRecord +
                ", pauseRecord=" + pauseRecord +
                ", startOrPauseRecord=" + startOrPauseRecord +
                ", stopRecord=" + stopRecord +
                ", startPlay=" + startPlay +
                ", pausePlay=" + pausePlay +
                ", startOrPausePlay=" + startOrPausePlay +
                '}';
    }
}
