package com.mstarc.music.wearablemusic.data;

/**
 * Created by liuqing
 * 18-1-3.
 * Email: 1239604859@qq.com
 */

public class MusicBus {
    private boolean isStart;
    private boolean isPause;
    private boolean startOrPause;

    private boolean isNext;
    private boolean isPre;

    public boolean isStart() {
        return isStart;
    }

    public MusicBus setStart(boolean start) {
        isStart = start;
        return this;
    }

    public boolean isPause() {
        return isPause;
    }

    public MusicBus setPause(boolean pause) {
        isPause = pause;
        return this;
    }

    public boolean isStartOrPause() {
        return startOrPause;
    }

    public MusicBus setStartOrPause(boolean startOrPause) {
        this.startOrPause = startOrPause;
        return this;
    }

    public boolean isNext() {
        return isNext;
    }

    public MusicBus setNext(boolean next) {
        isNext = next;
        return this;
    }

    public boolean isPre() {
        return isPre;
    }

    public MusicBus setPre(boolean pre) {
        isPre = pre;
        return this;
    }

    @Override
    public String toString() {
        return "MusicBus{" +
                "isStart=" + isStart +
                ", isPause=" + isPause +
                ", startOrPause=" + startOrPause +
                ", isNext=" + isNext +
                ", isPre=" + isPre +
                '}';
    }
}
