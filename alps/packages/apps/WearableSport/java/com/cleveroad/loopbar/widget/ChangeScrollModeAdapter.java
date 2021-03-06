package com.cleveroad.loopbar.widget;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablesport.R;
import com.cleveroad.loopbar.adapter.IOperationItem;

import java.util.Collection;


final class ChangeScrollModeAdapter extends RecyclerView.Adapter<BaseRecyclerViewHolder<IOperationItem>>
        implements IChangeSizeCallback {

    static final int VIEW_TYPE_CHANGE_SCROLL_MODE = 2;
    @Orientation
    private int mOrientation = Orientation.ORIENTATION_VERTICAL;
    private CategoriesAdapter mInputAdapter;
    private boolean mIsIndeterminate = true;

    private int mHeaderSize;

    ChangeScrollModeAdapter(@NonNull RecyclerView.Adapter<? extends RecyclerView.ViewHolder> inputAdapter) {
        mInputAdapter = new CategoriesAdapter(inputAdapter);
    }

    @Override
    public BaseRecyclerViewHolder<IOperationItem> onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CHANGE_SCROLL_MODE) {
            return new ChangeScrollModeHolder((CategoriesAdapter.CategoriesHolder) mInputAdapter.onCreateViewHolder(parent, CategoriesAdapter.VIEW_TYPE_OTHER));
        }
        return mInputAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(BaseRecyclerViewHolder<IOperationItem> holder, int position) {
        mInputAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mIsIndeterminate ? mInputAdapter.getItemCount() : mInputAdapter.getItemCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
           return mInputAdapter.getItemViewType(position);
    }

    @Override
    public int getHeaderSize() {
        return mHeaderSize;
    }

    void setHeaderSize(int size) {
        mHeaderSize = size;
    }

    @Override
    @Orientation
    public int getOrientation() {
        return mOrientation;
    }

    void addOnItemClickListener(OnItemClickListener onItemClickListener) {
        mInputAdapter.addOnItemClickListener(onItemClickListener);
    }

    void removeOnItemClickListener(OnItemClickListener onItemClickListener) {
        mInputAdapter.removeOnItemClickListener(onItemClickListener);
    }

    Collection<IOperationItem> getWrappedItems() {
        return mInputAdapter.getWrappedItems();
    }


    void setListener(OnItemClickListener listener) {
        mInputAdapter.setListener(listener);
    }

    /**
     * Set mode for scrolling (Infinite or finite)
     *
     * @param isIndeterminate true for infinite
     */
    void setIsIndeterminate(boolean isIndeterminate) {
        if (mIsIndeterminate != isIndeterminate) {
            mIsIndeterminate = isIndeterminate;
            mInputAdapter.setIndeterminate(isIndeterminate);
            notifyDataSetChanged();
        }
    }

    IOperationItem getItem(int position) {
        return mInputAdapter.getItem(position);
    }


    void setOrientation(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            mInputAdapter.setOrientation(orientation);
        }
    }

    int normalizePosition(int position) {
        return mInputAdapter.normalizePosition(position);
    }


    private View createHeaderView(ViewGroup parent) {
        @LayoutRes int layoutId = mOrientation == Orientation.ORIENTATION_VERTICAL
                ? R.layout.enls_empty_header_vertical
                : R.layout.enls_empty_header_horizontal;
        return LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
    }

    @SuppressWarnings("WeakerAccess")
    class HeaderHolder extends BaseRecyclerViewHolder<IOperationItem> {

        private IChangeSizeCallback mChangeSizeCallback;

        private HeaderHolder(@NonNull View itemView, IChangeSizeCallback changeSizeCallback) {
            super(itemView);
            mChangeSizeCallback = changeSizeCallback;
        }

        @Override
        protected void onBindItem(IOperationItem item, int position) {
            int size = mChangeSizeCallback.getHeaderSize();
            if (size > 0) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                if (mChangeSizeCallback.getOrientation() == Orientation.ORIENTATION_HORIZONTAL) {
                    layoutParams.width = size;
                } else {
                    layoutParams.height = size;
                }
            }
        }
    }

    // this class is only for encapsulation CategoriesHolder, actually now it doesn't change any logic
    class ChangeScrollModeHolder extends BaseRecyclerViewHolder<IOperationItem> {

        private CategoriesAdapter.CategoriesHolder categoriesHolder;

        ChangeScrollModeHolder(CategoriesAdapter.CategoriesHolder categoriesHolder) {
            super(categoriesHolder.itemView);
            this.categoriesHolder = categoriesHolder;
        }

        @Override
        protected void onBindItem(IOperationItem item, int position) {
            categoriesHolder.onBindItem(item, position);
        }

        <T extends RecyclerView.ViewHolder> void bindItemWildcardHelper(RecyclerView.Adapter<T> adapter, int position) {
            categoriesHolder.bindItemWildcardHelper(adapter, position);
        }
    }
}
