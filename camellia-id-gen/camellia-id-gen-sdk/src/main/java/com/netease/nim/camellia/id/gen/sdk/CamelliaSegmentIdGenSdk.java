package com.netease.nim.camellia.id.gen.sdk;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.segment.AbstractCamelliaSegmentIdGen;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于http协议访问中心化部署的发号器
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaSegmentIdGenSdk extends AbstractCamelliaSegmentIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSegmentIdGenSdk.class);

    private final CamelliaIdGenSdkConfig config;
    private final OkHttpClient okHttpClient;

    private final boolean cacheEnable;

    public CamelliaSegmentIdGenSdk(CamelliaIdGenSdkConfig config) {
        if (config.getUrl() == null || config.getUrl().trim().length() == 0) {
            throw new CamelliaIdGenException("url is empty");
        }
        this.config = config;
        this.okHttpClient = CamelliaIdGenHttpUtils.initOkHttpClient(config);
        CamelliaIdGenSdkConfig.SegmentIdGenSdkConfig segmentIdGenSdkConfig = config.getSegmentIdGenSdkConfig();
        this.cacheEnable = segmentIdGenSdkConfig.isCacheEnable();

        this.step = segmentIdGenSdkConfig.getStep();
        this.cacheMaxCapacity = step * 10;
        this.maxRetry = segmentIdGenSdkConfig.getMaxRetry();
        this.retryIntervalMillis = segmentIdGenSdkConfig.getRetryIntervalMillis();

        this.cacheMap = new ConcurrentLinkedHashMap.Builder<String, LinkedBlockingQueue<Long>>()
                .initialCapacity(segmentIdGenSdkConfig.getTagCount()).maximumWeightedCapacity(segmentIdGenSdkConfig.getTagCount()).build();
        this.lockMap = new ConcurrentLinkedHashMap.Builder<String, AtomicBoolean>()
                .initialCapacity(segmentIdGenSdkConfig.getTagCount() * 2).maximumWeightedCapacity(segmentIdGenSdkConfig.getTagCount() * 2).build();
        this.asyncLoadThreadPool = segmentIdGenSdkConfig.getAsyncLoadThreadPool();

        logger.info("CamelliaSegmentIdGenSdk init success, url = {}, cacheEnable = {}, step = {}, tagCount = {}",
                config.getUrl(), cacheEnable, step, segmentIdGenSdkConfig.getTagCount());
    }

    @Override
    public List<Long> genIds(String tag, int count) {
        if (cacheEnable) {
            return super.genIds(tag, count);
        } else {
            return _genIds(tag, count);
        }
    }

    @Override
    public long genId(String tag) {
        if (cacheEnable) {
            return super.genId(tag);
        } else {
            return _genId(tag);
        }
    }

    @Override
    protected void loadCache(LinkedBlockingQueue<Long> cache, String tag, int loadCount) {
        List<Long> ids = _genIds(tag, loadCount);
        for (Long id : ids) {
            cache.offer(id);
        }
    }

    private long _genId(String tag) {
        try {
            String fullUrl = config.getUrl() + "/camellia/id/gen/segment/genId?tag=" + URLEncoder.encode(tag, "utf-8");
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }

    private List<Long> _genIds(String tag, int count) {
        try {
            String fullUrl = config.getUrl() + "/camellia/id/gen/segment/genIds?tag=" + URLEncoder.encode(tag, "utf-8") + "&count=" + count;
            return CamelliaIdGenHttpUtils.genIds(okHttpClient, fullUrl);
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }

}
