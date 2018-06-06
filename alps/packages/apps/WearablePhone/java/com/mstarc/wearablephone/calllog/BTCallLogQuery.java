/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mstarc.wearablephone.calllog;

import android.provider.CallLog.Calls;

import com.mstarc.wearablephone.bluetooth.DataProviderContract;

/**
 * The query for the call log table.
 */
public final class BTCallLogQuery {
    public static final String[] _PROJECTION = new String[]{
            DataProviderContract._ID,                          // 0
            DataProviderContract.TYPE,                         // 1
            DataProviderContract.NUMBER,                       // 2
            DataProviderContract.NAME,                       // 3
            DataProviderContract.DATE,                         // 4
    };

    public static final int ID = 0;
    public static final int CALL_TYPE = 1;
    public static final int NUMBER = 2;
    public static final int NAME = 3;
    public static final int DATE = 4;
}
