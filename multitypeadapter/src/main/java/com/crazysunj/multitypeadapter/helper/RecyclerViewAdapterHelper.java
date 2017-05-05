/**
 * Copyright 2017 Sun Jian
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crazysunj.multitypeadapter.helper;

import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.crazysunj.multitypeadapter.entity.LevelData;
import com.crazysunj.multitypeadapter.entity.MultiHeaderEntity;
import com.crazysunj.multitypeadapter.sticky.StickyHeaderDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * description
 * 使用粘性头部，请调用bindToRecyclerView,否则自己添加decoration
 * type 取值范围
 * 数据类型 [0,1000)
 * 头类型 [-1000,0)
 * shimmer数据类型 [-2000,-1000)
 * shimmer头类型 [-3000,-2000)
 * Created by sunjian on 2017/5/4.
 */

public abstract class RecyclerViewAdapterHelper<T extends MultiHeaderEntity> {

    private static final int DEFAULT_VIEW_TYPE = -0xff;
    //默认头的level，处理数据只跟type有关系
    private static final int DEFAULT_HEADER_LEVEL = -1;
    //只刷新头
    private static final int REFRESH_HEADER = 0;
    //只刷新数据
    private static final int REFRESH_DATA = 1;
    //同时刷新数据和头
    private static final int REFRESH_HEADER_DATA = 2;

    //控制麒麟臂用户
    private boolean isCanRefresh = true;
    //随机id的默认最大值
    private long mMaxRandomId = -1;
    //随机id的默认最小值
    private long mMinRandomId = Long.MIN_VALUE;
    //是否使用粘性
    private boolean mIsUseStickyHeader = false;
    //loadingview关于数据方面的最大缓存值
    private int mMaxDataCacheCount = 12;
    //loadingview关于头方面的最大缓存值
    private int mMaxHeaderCacheCount = 6;

    //根据type存储layoutId
    private SparseIntArray mLayouts;
    //根据type存储等级
    private SparseIntArray mLevels;
    //根据level存储的数据
    private SparseArray<LevelData<T>> mLevelOldData;
    //数据的缓存
    private LruCache<Integer, List<T>> mDataCache;
    //头的缓存
    private LruCache<Integer, T> mHeaderCache;
    //老数据
    protected List<T> mOldData;
    //当前数据
    protected List<T> mData;
    //跟data无关且在data之前的条目数量
    private int mPreDataCount = 0;
    private RecyclerView.Adapter mAdapter;

    public RecyclerViewAdapterHelper(List<T> data) {

        mData = data == null ? new ArrayList<T>() : data;

        if (mOldData == null) {
            mOldData = new ArrayList<T>();
        }

        if (mLevelOldData == null) {
            mLevelOldData = new SparseArray<LevelData<T>>();
        }

        if (mLevels == null) {
            mLevels = new SparseIntArray();
        }

        if (mLayouts == null) {
            mLayouts = new SparseIntArray();
        }
    }

    /**
     * 绑定adapter
     *
     * @param adapter
     */
    public void bindAdapter(RecyclerView.Adapter adapter) {

        mAdapter = adapter;
    }

    public int getItemViewType(int position) {

        T item = mData.get(position);
        if (item != null) {
            return item.getItemType();
        }

        return DEFAULT_VIEW_TYPE;
    }

    /**
     * @param type                     数据类型
     * @param level                    数据级别
     * @param layoutResId              正常类型布局id
     * @param headerResId              头类型布局id
     * @param shimmerLayoutResId       loading 数据类型布局id
     * @param shimmerHeaderLayoutResId loading 头类型布局id
     */
    public void registerMoudleWithShimmer(@IntRange(from = 0, to = 999) int type, @IntRange(from = 0) int level,
                                          @LayoutRes int layoutResId, @LayoutRes int headerResId,
                                          @LayoutRes int shimmerLayoutResId, @LayoutRes int shimmerHeaderLayoutResId) {

        registerMoudle(type, level, layoutResId, headerResId);

        int shimmerType = type - 2000;
        mLevels.put(shimmerType, level);
        mLayouts.put(shimmerType, shimmerLayoutResId);

        int shimmerHeaderType = type - 3000;
        mLevels.put(shimmerHeaderType, DEFAULT_HEADER_LEVEL);
        mLayouts.put(shimmerHeaderType, shimmerHeaderLayoutResId);
    }

    public void registerMoudleWithShimmer(@IntRange(from = 0, to = 999) int type, @IntRange(from = 0) int level,
                                          @LayoutRes int layoutResId, @LayoutRes int headerResId, @LayoutRes int shimmerHeaderLayoutResId) {

        registerMoudle(type, level, layoutResId, headerResId);

        int shimmerHeaderType = type - 3000;
        mLevels.put(shimmerHeaderType, DEFAULT_HEADER_LEVEL);
        mLayouts.put(shimmerHeaderType, shimmerHeaderLayoutResId);
    }

    public void registerMoudleWithShimmer(@IntRange(from = 0, to = 999) int type, @IntRange(from = 0) int level,
                                          @LayoutRes int layoutResId, @LayoutRes int shimmerLayoutResId) {

        registerMoudle(type, level, layoutResId);

        int shimmerType = type - 2000;
        mLevels.put(shimmerType, level);
        mLayouts.put(shimmerType, shimmerLayoutResId);
    }

    public void registerMoudle(@IntRange(from = 0, to = 999) int type, @IntRange(from = 0) int level,
                               @LayoutRes int layoutResId, @LayoutRes int headerResId) {

        registerMoudle(type, level, layoutResId);

        int headerType = type - 1000;
        mLevels.put(headerType, DEFAULT_HEADER_LEVEL);
        mLayouts.put(headerType, headerResId);
    }

    public void registerMoudle(@IntRange(from = 0, to = 999) int type, @IntRange(from = 0) int level,
                               @LayoutRes int layoutResId) {

        mLevels.put(type, level);
        mLayouts.put(type, layoutResId);
    }

    /**
     * 提供粘性头headerId
     *
     * @param position
     * @return
     */
    public long getHeaderId(int position) {
        try {
            return mData.get(position).getHeaderId();
        } catch (Exception e) {
            return StickyHeaderDecoration.NO_HEADER_ID;
        }
    }

    public List<T> getData() {
        return mData;
    }

    /**
     * 传头部的type是拿不到数据的
     *
     * @param type
     * @return
     */
    public LevelData<T> getDataWithType(int type) {

        return mLevelOldData.get(getLevel(type));
    }

    /**
     * 设置随机id的最大值
     *
     * @param maxRandomId
     */
    public void setMaxRandomId(long maxRandomId) {

        mMaxRandomId = maxRandomId;
    }

    /**
     * 设置随机id的最小值
     *
     * @param minRandomId
     */
    public void setMinRandomId(long minRandomId) {

        mMinRandomId = minRandomId;
    }

    /**
     * 设置刷新数据前自定义的条目数量，防止混乱
     *
     * @param preDataCount
     */
    public void serPreDataCount(int preDataCount) {

        mPreDataCount = preDataCount;
    }

    /**
     * 不断向下取值，如果抛异常，你可以重新设置最大值，但切记不能重复啊，报错我可不负责啊，这么多的值都用完了，是在下输了
     * 有特殊要求的同学可以自己设计
     * 是不是随机有待商量
     *
     * @return
     */
    public long getRandomId() {

        if (mMaxRandomId <= mMinRandomId) {
            throw new RuntimeException("boy,you win !");
        }

        return mMaxRandomId--;
    }

    /**
     * 务必在调用之前确定缓存最大值，可调用setMaxHeaderCacheCount和setMaxHeaderCacheCount
     * 对应notifyMoudleDataChanged
     *
     * @param type
     * @param dataCount
     */
    public void notifyShimmerDataChanged(int type, @IntRange(from = 1) int dataCount) {

        List<T> datas = createShimmerDatas(type, dataCount);

        notifyMoudleChanged(datas, null, type, REFRESH_DATA);
    }

    /**
     * 只刷新数据
     *
     * @param data
     * @param type
     */
    public void notifyMoudleDataChanged(List<T> data, int type) {

        notifyMoudleChanged(data, null, type, REFRESH_DATA);
    }

    /**
     * 务必在调用之前确定缓存最大值，可调用setMaxHeaderCacheCount和setMaxHeaderCacheCount
     * 对应notifyMoudleHeaderChanged
     *
     * @param type
     */
    public void notifyShimmerHeaderChanged(int type) {

        T header = createShimmerHeader(type);

        notifyMoudleChanged(null, header, type, REFRESH_HEADER);
    }

    /**
     * 只刷新头
     *
     * @param header
     * @param type
     */
    public void notifyMoudleHeaderChanged(T header, int type) {

        notifyMoudleChanged(null, header, type, REFRESH_HEADER);
    }

    /**
     * 务必在调用之前确定缓存最大值，可调用setMaxHeaderCacheCount和setMaxHeaderCacheCount
     * 对应notifyMoudleDataAndHeaderChanged
     *
     * @param type
     * @param dataCount
     */
    public void notifyShimmerDataAndHeaderChanged(int type, @IntRange(from = 1) int dataCount) {

        T header = createShimmerHeader(type);
        List<T> datas = createShimmerDatas(type, dataCount);

        notifyMoudleChanged(datas, header, type, REFRESH_HEADER_DATA);
    }


    /**
     * 同时刷新头和数据
     *
     * @param data
     * @param header
     * @param type
     */
    public void notifyMoudleDataAndHeaderChanged(List<T> data, T header, int type) {

        notifyMoudleChanged(data, header, type, REFRESH_HEADER_DATA);
    }

    /**
     * 设置loadview的数据集合缓存最大值
     *
     * @param maxDataCacheCount
     */
    public void setMaxDataCacheCount(int maxDataCacheCount) {

        mMaxDataCacheCount = maxDataCacheCount;
    }

    /**
     * 设置loadview的头集合缓存最大值
     *
     * @param maxHeaderCacheCount
     */
    public void setMaxHeaderCacheCount(int maxHeaderCacheCount) {

        mMaxHeaderCacheCount = maxHeaderCacheCount;
    }

    /**
     * 返回比较的callback对象，提供新老数据
     *
     * @param oldData
     * @param newData
     * @return
     */
    protected DiffUtil.Callback getDiffCallBack(List<T> oldData, List<T> newData) {

        return new DiffCallBack<T>(oldData, newData);
    }

    /**
     * 是否要移动
     *
     * @return
     */
    protected boolean isDetectMoves() {
        return true;
    }

    /**
     * @param newData     刷新的数据
     * @param newHeader   刷新数据顶部的头,如果不需要传空就行了
     * @param type        刷新数据的类型,切忌,传头部类型是报错的,只要关心数据类型就行了
     * @param refreshType 刷新类型
     */
    protected void notifyMoudleChanged(List<T> newData, T newHeader, int type, int refreshType) {

        if (!isCanRefresh) {
            return;
        }
        isCanRefresh = false;

        startRefresh(newData, newHeader, type, refreshType);
    }

    /**
     * 重写此方法，实现同步或异步刷新
     * 同步稳定，但是可能会卡顿
     * 异步效果好，但可能会异常
     * 根据实际情况选择相应的刷新机制
     *
     * @param newData
     * @param newHeader
     * @param type
     * @param refreshType
     */
    protected abstract void startRefresh(List<T> newData, T newHeader, int type, int refreshType);

    /**
     * 开始刷新
     *
     * @param diffResult
     */
    protected final void handleResult(DiffUtil.DiffResult diffResult) {

        if (mAdapter != null) {
            diffResult.dispatchUpdatesTo(mAdapter);
        }
        isCanRefresh = true;
    }

    /**
     * 核心部分，新老数据比较
     *
     * @param newData
     * @param newHeader
     * @param type
     * @param refreshType
     * @return
     */
    protected final DiffUtil.DiffResult handleRefresh(List<T> newData, T newHeader, int type, int refreshType) {

        int level = getLevel(type);
        int sum = 0;

        for (int i = 0; i < level; i++) {

            LevelData<T> data = mLevelOldData.get(i);

            if (data == null) {
                continue;
            }

            if (data.getHeader() != null) {
                sum++;
            }

            List<T> list = data.getData();
            if (list == null || list.isEmpty()) {
                continue;
            }
            int size = list.size();
            sum += size;
        }

        LevelData<T> oldLevelData = mLevelOldData.get(level);
        List<T> oldData = null;
        T oldHeader = null;

        if (oldLevelData != null) {

            List<T> oldItemData = oldLevelData.getData();
            if (oldItemData != null && !oldItemData.isEmpty()) {
                if (refreshType == REFRESH_HEADER) {
                    oldData = oldItemData;
                } else {
                    mData.removeAll(oldItemData);
                }
            }

            T oldItemHeader = oldLevelData.getHeader();
            if (oldItemHeader != null) {
                if (refreshType == REFRESH_DATA) {
                    oldHeader = oldItemHeader;
                } else {
                    mData.remove(oldItemHeader);
                }
            }
        }

        int positionStart = sum + mPreDataCount;
        boolean isDataEmpty = newData == null || newData.isEmpty();
        boolean isHeaderEmpty = newHeader == null;

        if (!isDataEmpty && refreshType != REFRESH_HEADER) {
            mData.addAll(positionStart, newData);
        }
        if (!isHeaderEmpty && refreshType != REFRESH_DATA) {
            mData.add(positionStart, newHeader);
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(getDiffCallBack(mOldData, mData), isDetectMoves());
        mLevelOldData.put(level, new LevelData<T>(refreshType != REFRESH_HEADER ? newData : oldData, refreshType != REFRESH_DATA ? newHeader : oldHeader));
        mOldData.clear();
        mOldData.addAll(mData);

        return result;
    }

    public final int getLayoutId(int viewType) {

        return mLayouts.get(viewType);
    }

    private int getLevel(int type) {

        int level = mLevels.get(type, DEFAULT_HEADER_LEVEL);
        if (level <= DEFAULT_HEADER_LEVEL) {
            throw new RuntimeException("boy , are you sure register this data type (not include header type) ?");
        }

        return level;
    }

    /**
     * 根据type产生loading的数据集合
     *
     * @param type
     * @param dataCount
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<T> createShimmerDatas(int type, @IntRange(from = 1) int dataCount) {

        if (mDataCache == null) {
            mDataCache = new LruCache<Integer, List<T>>(mMaxDataCacheCount);
        }

        List<T> datas = mDataCache.get(type);

        if (datas == null) {
            datas = new ArrayList<T>();
            mDataCache.put(type, datas);
        }

        int size = datas.size();

        if (size < dataCount) {

            for (int i = 0, n = dataCount - size; i < n; i++) {
                datas.add((T) new ShimmerEntity());
            }
        }
        for (int i = 0; i < dataCount; i++) {

            T entity = datas.get(i);
            if (entity instanceof ShimmerEntity) {
                ShimmerEntity head = (ShimmerEntity) entity;
                head.setId(getRandomId());
                head.setType(type - 2000);
            }
        }

        return datas;
    }

    /**
     * 根据type产生loading的头
     *
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    private T createShimmerHeader(int type) {

        if (mHeaderCache == null) {
            mHeaderCache = new LruCache<Integer, T>(mMaxHeaderCacheCount);
        }

        T header = mHeaderCache.get(type);

        if (header == null) {
            header = (T) new ShimmerEntity();
            mHeaderCache.put(type, header);
        }

        if (header instanceof ShimmerEntity) {
            ShimmerEntity head = (ShimmerEntity) header;
            head.setId(getRandomId());
            head.setType(type - 3000);
        }

        return header;
    }

}
