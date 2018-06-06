package com.cleveroad.loopbar.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.cleveroad.loopbar.util.GravitySnapHelper;
import com.mstarc.wearablesport.R;
import com.cleveroad.loopbar.adapter.SimpleCategoriesAdapter;
import com.cleveroad.loopbar.model.MockedItemsFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoopBarView extends FrameLayout implements OnItemClickListener {

    /**
     * Scroll mode constant for LoopBar
     * Representing automatic (adapting) scrolling state of LoopBar
     * If amount of items in LoopBar won't be enough to get out of bounds of LoopBar
     * (i.e. all items fit on screen) it will have finite behavior {@link #SCROLL_MODE_FINITE}.
     * In another case there will be infinite behavior {@link #SCROLL_MODE_INFINITE}
     * (there will be displayed only one appearance of each added item in LoopBar)
     *
     * @see ScrollAttr
     * @see #setScrollMode(int)
     * @see #getScrollMode()
     * @see #SCROLL_MODE_FINITE
     * @see #SCROLL_MODE_INFINITE
     */
    public static final int SCROLL_MODE_AUTO = 2;

    /**
     * Scroll mode constant for LoopBar
     * Representing infinite scrolling state of LoopBar
     * (items will repeatedly show in the LoopBar while you scroll it)
     *
     * @see ScrollAttr
     * @see #setScrollMode(int)
     * @see #getScrollMode()
     */
    public static final int SCROLL_MODE_INFINITE = 3;

    /**
     * Scroll mode constant for LoopBar
     * Representing finite scrolling state of LoopBar
     * (there will be displayed only one appearance of each added item in LoopBar)
     *
     * @see ScrollAttr
     * @see #setScrollMode(int)
     * @see #getScrollMode()
     */
    public static final int SCROLL_MODE_FINITE = 4;

    private static final String TAG = LoopBarView.class.getSimpleName();

    //outside params
    private RecyclerView.Adapter<? extends RecyclerView.ViewHolder> mInputAdapter;
    private List<OnItemClickListener> mClickListeners = new ArrayList<>();
    private int mColorCodeSelectionView;

    //view settings
    private IOrientationState mOrientationState;
    //state settings below
    private int mCurrentItemPosition;

    //views
    private RecyclerView mRvCategories;
    @Nullable

    private ChangeScrollModeAdapter mOuterAdapter;

    private LinearLayoutManager mLinearLayoutManager;
    private boolean mSkipNextOnLayout;
    private boolean mIndeterminateInitialized;
    private boolean mInfinite;
    GravitySnapHelper mGravitySnapHelper;

    @ScrollAttr
    private int mScrollMode;

    private IndeterminateOnScrollListener mIndeterminateOnScrollListener = new IndeterminateOnScrollListener(this);

    public LoopBarView(Context context) {
        super(context);
        init(context, null);
    }

    public LoopBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoopBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoopBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void inflate(IOrientationState orientationState, int backgroundResource) {
        inflate(getContext(), orientationState.getLayoutId(), this);
        mRvCategories = (RecyclerView) findViewById(R.id.rvCategories);
        View vRvContainer = findViewById(R.id.vRvContainer);
        /* background color must be set to container of recyclerView.
         * If you set it to main view, there will be any transparent part
         * when selector has overlay */
        vRvContainer.setBackgroundResource(backgroundResource);
        mGravitySnapHelper.attachToRecyclerView(mRvCategories);

    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        //read customization attributes
        mGravitySnapHelper = new GravitySnapHelper(Gravity.TOP);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoopBarView);
        mColorCodeSelectionView = typedArray.getColor(R.styleable.LoopBarView_enls_selectionBackground,
                ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark));
        int orientation = typedArray
                .getInteger(R.styleable.LoopBarView_enls_orientation, Orientation.ORIENTATION_HORIZONTAL);
        mInfinite = typedArray.getBoolean(R.styleable.LoopBarView_enls_infiniteScrolling, true);

        @ScrollAttr int scrollMode = typedArray.getInt(R.styleable.LoopBarView_enls_scrollMode,
                mInfinite ? SCROLL_MODE_INFINITE : SCROLL_MODE_FINITE);
        mScrollMode = scrollMode;
        int focusedIndex = typedArray
                .getInteger(R.styleable.LoopBarView_enls_forcus_index, 1);
        typedArray.recycle();

        //check attributes you need, for example all paddings
        int[] attributes = new int[]{android.R.attr.background};
        //then obtain typed array
        typedArray = context.obtainStyledAttributes(attrs, attributes);
        int backgroundResource = typedArray.getResourceId(0, R.color.enls_default_list_background);

        //current view has two state : horizontal & vertical. State design pattern
        mOrientationState = getOrientationStateFromParam(orientation);
        inflate(mOrientationState, backgroundResource);

        mLinearLayoutManager = mOrientationState.getLayoutManager(getContext(), focusedIndex);
        mRvCategories.setLayoutManager(mLinearLayoutManager);

        if (isInEditMode()) {
            setCategoriesAdapter(new SimpleCategoriesAdapter(MockedItemsFactory.getCategoryItems(getContext())));
        }

        typedArray.recycle();
    }


    /**
     * Sets scroll mode to infinite or finite
     *
     * @param isInfinite value presents is scroll mode need to be infinite
     * @deprecated use {@link #setScrollMode(int)} instead
     */
    @Deprecated
    public void setIsInfinite(boolean isInfinite) {
        setScrollMode(isInfinite ? SCROLL_MODE_INFINITE : SCROLL_MODE_FINITE);
    }

    private void changeScrolling(boolean isInfinite) {
        if (mInfinite != isInfinite) {
            mInfinite = isInfinite;
            if (mOuterAdapter != null) {
                mOuterAdapter.setIsIndeterminate(isInfinite);
            }
            checkAndScroll();
        }
    }

    /**
     * Returns constant representing current scroll mode
     *
     * @return one of {@link ScrollAttr}
     */
    @ScrollAttr
    public final int getScrollMode() {
        return mScrollMode;
    }

    /**
     * Sets new Scroll mode for LoopBar
     *
     * @param scrollMode must be one of {@link ScrollAttr}
     */
    public final void setScrollMode(@ScrollAttr int scrollMode) {
        if (scrollMode != mScrollMode) {
            mScrollMode = scrollMode;
            validateScrollMode();
        }
    }

    /**
     * Returns boolean value representing if LoopBar is in infinite mode or not
     *
     * @return true if LoopBar is in infinite mode or false if is in finite
     */
    public boolean isInfinite() {
        return mInfinite;
    }

    private void validateScrollMode() {
        if (mScrollMode == SCROLL_MODE_AUTO) {
            if (mOrientationState != null
                    && mRvCategories != null
                    && mOuterAdapter != null) {
                boolean isFitOnScreen = mOrientationState.isItemsFitOnScreen(mRvCategories,
                        mOuterAdapter.getWrappedItems().size());
                changeScrolling(!isFitOnScreen);
            }
        } else if (mScrollMode == SCROLL_MODE_INFINITE) {
            changeScrolling(true);
        } else if (mScrollMode == SCROLL_MODE_FINITE) {
            changeScrolling(false);
        }
    }

    /**
     * Initiate LoopBar with RecyclerView adapter
     *
     * @param inputAdapter Instance of {@link RecyclerView.Adapter}
     */
    public void setCategoriesAdapter(@NonNull RecyclerView.Adapter<? extends RecyclerView.ViewHolder> inputAdapter) {
        mInputAdapter = inputAdapter;
        mOuterAdapter = new ChangeScrollModeAdapter(inputAdapter);
        validateScrollMode();
        mOuterAdapter.setIsIndeterminate(mInfinite);
        mOuterAdapter.setListener(this);
        mOuterAdapter.setOrientation(mOrientationState.getOrientation());
        mRvCategories.setAdapter(mOuterAdapter);
    }

   /**
     * Add item click listener to this view
     *
     * @param itemClickListener Instance of {@link OnItemClickListener}
     * @return always true.
     */
    @SuppressWarnings("unused")
    public boolean addOnItemClickListener(OnItemClickListener itemClickListener) {
        return mClickListeners.add(itemClickListener);
    }

    /**
     * Remove item click listener from this view
     *
     * @param itemClickListener Instance of {@link OnItemClickListener}
     * @return true if this {@code List} was modified by this operation, false
     * otherwise.
     */
    @SuppressWarnings("unused")
    public boolean removeOnItemClickListener(OnItemClickListener itemClickListener) {
        return mClickListeners.remove(itemClickListener);
    }

    private void notifyItemClickListeners(int normalizedPosition) {
        for (OnItemClickListener itemClickListener : mClickListeners) {
            itemClickListener.onItemClicked(normalizedPosition);
        }
    }

    /**
     * Returns RecyclerView wrapped inside of view for control animations
     * Don't use it for changing adapter inside.
     * Use {@link #setCategoriesAdapter(RecyclerView.Adapter)} instead
     *
     * @return instance of {@link RecyclerView}
     * @deprecated use {@link #setItemAnimator(RecyclerView.ItemAnimator)},
     * {@link #isAnimating()},
     * {@link #addItemDecoration(RecyclerView.ItemDecoration)},
     * {@link #addItemDecoration(RecyclerView.ItemDecoration, int)},
     * {@link #removeItemDecoration(RecyclerView.ItemDecoration)},
     * {@link #invalidateItemDecorations()},
     * {@link #addOnScrollListener(RecyclerView.OnScrollListener)},
     * {@link #removeOnScrollListener(RecyclerView.OnScrollListener)}
     * {@link #clearOnScrollListeners()} instead
     */
    @Deprecated
    public RecyclerView getWrappedRecyclerView() {
        return mRvCategories;
    }

    private RecyclerView getRvCategories() {
        return mRvCategories;
    }

    /**
     * Sets the {@link RecyclerView.ItemAnimator} that will handle animations involving changes
     * to the items in wrapped RecyclerView. By default, RecyclerView instantiates and
     * uses an instance of {@link DefaultItemAnimator}. Whether item animations are
     * enabled for the RecyclerView depends on the ItemAnimator and whether
     * the LayoutManager {@link RecyclerView.LayoutManager#supportsPredictiveItemAnimations()
     * supports item animations}.
     *
     * @param animator The ItemAnimator being set. If null, no animations will occur
     *                 when changes occur to the items in this RecyclerView.
     */
    @SuppressWarnings("unused")
    public final void setItemAnimator(RecyclerView.ItemAnimator animator) {
        getRvCategories().setItemAnimator(animator);
    }

    /**
     * Returns true if wrapped RecyclerView is currently running some animations.
     * <p>
     * If you want to be notified when animations are finished, use
     * {@link RecyclerView.ItemAnimator#isRunning(RecyclerView.ItemAnimator.ItemAnimatorFinishedListener)}.
     *
     * @return True if there are some item animations currently running or waiting to be started.
     */
    @SuppressWarnings("unused")
    public final boolean isAnimating() {
        return getRvCategories().isAnimating();
    }

    /**
     * Add an {@link RecyclerView.ItemDecoration} to wrapped RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     * <p>
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     */
    @SuppressWarnings("unused")
    public final void addItemDecoration(RecyclerView.ItemDecoration decor) {
        getRvCategories().addItemDecoration(decor);
    }

    /**
     * Add an {@link RecyclerView.ItemDecoration} to wrapped RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     * <p>
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     * @param index Position in the decoration chain to insert this decoration at. If this value
     *              is negative the decoration will be added at the end.
     */
    @SuppressWarnings("unused")
    public final void addItemDecoration(RecyclerView.ItemDecoration decor, int index) {
        getRvCategories().addItemDecoration(decor, index);
    }

    /**
     * Remove an {@link RecyclerView.ItemDecoration} from wrapped RecyclerView.
     * <p>
     * <p>The given decoration will no longer impact the measurement and drawing of
     * item views.</p>
     *
     * @param decor Decoration to remove
     * @see #addItemDecoration(RecyclerView.ItemDecoration)
     */
    @SuppressWarnings("unused")
    public final void removeItemDecoration(RecyclerView.ItemDecoration decor) {
        getRvCategories().removeItemDecoration(decor);
    }

    /**
     * Invalidates all ItemDecorations in wrapped RecyclerView. If RecyclerView has item decorations, calling this method
     * will trigger a {@link #requestLayout()} call.
     */
    @SuppressWarnings("unused")
    public final void invalidateItemDecorations() {
        getRvCategories().invalidateItemDecorations();
    }

    /**
     * Add a listener to wrapped RecyclerView that will be notified of any changes in scroll state or position.
     * <p>
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnScrollListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to set or null to clear
     */
    @SuppressWarnings("unused")
    public final void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        getRvCategories().addOnScrollListener(listener);
    }

    /**
     * Remove a listener from wrapped RecyclerView that was notified of any changes in scroll state or position.
     *
     * @param listener listener to set or null to clear
     */
    @SuppressWarnings("unused")
    public final void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        getRvCategories().removeOnScrollListener(listener);
    }

    /**
     * Remove all secondary listener from wrapped RecyclerView that were notified of any changes in scroll state or position.
     */
    @SuppressWarnings("unused")
    public final void clearOnScrollListeners() {
        getRvCategories().clearOnScrollListeners();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        int adapterSize = ss.mAdapterSize;
        if (adapterSize > 0) {
            setCurrentItem(ss.mCurrentItemPosition);
        }
        mScrollMode = ss.mScrollMode;
        changeScrolling(ss.mIsInfinite);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!mSkipNextOnLayout) {

            if (!mIndeterminateInitialized) {
                mIndeterminateInitialized = true;
                //scroll to middle of indeterminate recycler view on initialization and if user somehow scrolled to start or end
                mRvCategories.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                boolean isFitOnScreen = mOrientationState
                                        .isItemsFitOnScreen(mRvCategories, mOuterAdapter.getWrappedItems().size());
                                if (mScrollMode == SCROLL_MODE_AUTO) {
                                    changeScrolling(!isFitOnScreen);
                                }
                                checkAndScroll();
                                mRvCategories.addOnScrollListener(mIndeterminateOnScrollListener);
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                    mRvCategories.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                } else {
                                    mRvCategories.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }

                            }
                        });
            }
            mSkipNextOnLayout = true;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        int adapterSize = mInputAdapter != null ? mInputAdapter.getItemCount() : 0;
        return new SavedState(superState,
                mCurrentItemPosition, mInfinite, mScrollMode,
                adapterSize);
    }

    /**
     * Set selected item in endless view. OnItemSelected listeners won't be invoked
     *
     * @param currentItemPosition selected position
     */
    @SuppressWarnings("unused")
    public void setCurrentItem(int currentItemPosition) {
        selectItem(currentItemPosition, false);
    }

    /**
     * Set selected item in endless view.
     * OnItemSelected listeners won't be invoked
     *
     * @param currentItemPosition selected position
     * @param isInvokeListeners   should view notify OnItemSelected listeners about this selection
     */
    @SuppressWarnings("unused")
    public void setCurrentItem(int currentItemPosition, boolean isInvokeListeners) {
        selectItem(currentItemPosition, isInvokeListeners);
    }

    /**
     * Select item by its' position
     *
     * @param position        int value of item position to select
     * @param invokeListeners boolean value for invoking listeners
     */
    @SuppressWarnings("unused")
    public void selectItem(int position, boolean invokeListeners) {

        int realPosition = mOuterAdapter.normalizePosition(position);
        //do nothing if position not changed
        if (realPosition == mCurrentItemPosition) {
            return;
        }

        mCurrentItemPosition = realPosition;

        if (invokeListeners) {
            notifyItemClickListeners(realPosition);
        }

        Log.i(TAG, "clicked on position =" + position);

    }

    /**
     * Select item by its' position. Listeners will be invoked
     *
     * @param position int value of item position to select
     */
    @Override
    public void onItemClicked(int position) {
        selectItem(position, true);
    }

    //orientation state factory method
    public IOrientationState getOrientationStateFromParam(int orientation) {
        return orientation == Orientation.ORIENTATION_VERTICAL
                ? new OrientationStateVertical()
                : new OrientationStateHorizontal();
    }

    private void checkAndScroll() {
        if (isInfinite() && (mLinearLayoutManager.findFirstVisibleItemPosition() == 0
                || mLinearLayoutManager.findFirstVisibleItemPosition() == 1
                || mLinearLayoutManager.findLastVisibleItemPosition() == Integer.MAX_VALUE)) {
            mLinearLayoutManager.scrollToPositionWithOffset(getPositionForScroll(), 0);
        }
    }

    private int getPositionForScroll() {
        if (mOuterAdapter == null || mOuterAdapter.getWrappedItems().isEmpty()) {
            return Integer.MAX_VALUE / 2;
        }
        int size = mOuterAdapter.getWrappedItems().size();
        int count = (Integer.MAX_VALUE / 2) / size;

        return count * size;
    }


    /**
     * Interface with pre-defined limit of constants for scroll mode
     *
     * @see #SCROLL_MODE_AUTO
     * @see #SCROLL_MODE_INFINITE
     * @see #SCROLL_MODE_FINITE
     */
    @IntDef({SCROLL_MODE_AUTO, SCROLL_MODE_INFINITE, SCROLL_MODE_FINITE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollAttr {
    }

    /**
     * Encapsulate logic for LoopBar saving and restore state
     *
     * @see #onSaveInstanceState()
     * @see #onRestoreInstanceState(Parcelable)
     */
    public static class SavedState extends BaseSavedState implements Parcelable {

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private int mCurrentItemPosition;
        @ScrollAttr
        private int mScrollMode;
        private boolean mIsInfinite;
        private int mAdapterSize;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        private SavedState(Parcel parcel) {
            super(parcel);
            mCurrentItemPosition = parcel.readInt();
            @ScrollAttr
            int scrollMode = parcel.readInt();
            mScrollMode = scrollMode;
            boolean[] booleanValues = new boolean[1];
            parcel.readBooleanArray(booleanValues);
            mIsInfinite = booleanValues[0];
            mAdapterSize = parcel.readInt();
        }

        SavedState(Parcelable superState, int currentItemPosition, boolean isInfinite, int scrollMode, int adapterSize) {
            super(superState);
            mCurrentItemPosition = currentItemPosition;
            mIsInfinite = isInfinite;
            mScrollMode = scrollMode;
            mAdapterSize = adapterSize;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(mCurrentItemPosition);
            parcel.writeInt(mScrollMode);
            parcel.writeBooleanArray(new boolean[]{mIsInfinite});
            parcel.writeInt(mAdapterSize);
        }
    }

    private static class IndeterminateOnScrollListener extends RecyclerView.OnScrollListener {

        private final WeakReference<LoopBarView> loopBarViewWeakReference;

        private IndeterminateOnScrollListener(LoopBarView loopBarView) {
            loopBarViewWeakReference = new WeakReference<>(loopBarView);

        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LoopBarView loopBarView = loopBarViewWeakReference.get();
            if (loopBarView != null) {
                loopBarView.checkAndScroll();
            }

        }
    }
}
