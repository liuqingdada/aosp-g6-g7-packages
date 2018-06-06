package com.mstarc.wechat.wearwechat.model;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SyncKey {
    public int Count;
    public List<KeyVal> List;

    public String toString() {
        ArrayList localArrayList = new ArrayList();
        Iterator localIterator = this.List.iterator();
        while (localIterator.hasNext())
            localArrayList.add((localIterator.next()).toString());
        return StringUtil.join(localArrayList, "|");
    }

    public static class KeyVal {
        public int Key;
        public long Val;

        public String toString() {
            return this.Key + "_" + this.Val;
        }
    }
}

