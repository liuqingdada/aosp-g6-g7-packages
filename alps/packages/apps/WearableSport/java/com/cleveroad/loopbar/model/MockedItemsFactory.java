package com.cleveroad.loopbar.model;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.mstarc.wearablesport.R;
import com.cleveroad.loopbar.adapter.ICategoryItem;

import java.util.ArrayList;
import java.util.List;

public class MockedItemsFactory {

    private MockedItemsFactory() {}

    public static List<ICategoryItem> getCategoryItems(Context context) {
        List<ICategoryItem> items = new ArrayList<>();
        items.add(new CategoryItem(context.getString(R.string.sport_progress_item_bushu),"3548",null,true));
        items.add(new CategoryItem(context.getString(R.string.sport_progress_item_licheng),"2.35",context.getString(R.string.sport_progress_item_qianmi),true));
        items.add(new CategoryItem(context.getString(R.string.sport_progress_item_reliang),"35.4",context.getString(R.string.sport_progress_item_qianka),true));
        items.add(new CategoryItem(context.getString(R.string.sport_progress_item_sudu),"7'30''",null,true));
        return items;
    }
}
