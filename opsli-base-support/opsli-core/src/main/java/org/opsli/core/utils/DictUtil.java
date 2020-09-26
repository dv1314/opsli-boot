package org.opsli.core.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opsli.api.base.result.ResultVo;
import org.opsli.api.web.system.dict.DictDetailApi;
import org.opsli.api.wrapper.system.dict.DictWrapper;
import org.opsli.api.wrapper.system.dict.DictDetailModel;
import org.opsli.common.constants.CacheConstants;
import org.opsli.common.constants.DictConstants;
import org.opsli.core.cache.local.CacheUtil;
import org.opsli.plugins.redis.RedisLockPlugins;
import org.opsli.plugins.redis.RedisPlugin;
import org.opsli.plugins.redis.lock.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @BelongsProject: opsli-boot
 * @BelongsPackage: org.opsli.core.utils
 * @Author: Parker
 * @CreateTime: 2020-09-22 11:17
 * @Description: 字典工具类
 */
@Slf4j
@Component
public class DictUtil {

    /** Redis插件 */
    private static RedisPlugin redisPlugin;

    /** Redis分布式锁 */
    private static RedisLockPlugins redisLockPlugins;

    /** 字典Service */
    private static DictDetailApi dictDetailApi;


    /**
     * 根据 字典值 取 字典名称
     * @param typeCode 字典类型Code
     * @param dictValue 字典值
     * @param defaultVal 默认值
     * @return
     */
    public static String getDictNameByValue(String typeCode, String dictValue, String defaultVal){

        String dictName = "";
        DictDetailModel cacheModel = CacheUtil.getHash(DictConstants.CACHE_PREFIX_VALUE + typeCode,
                dictValue, DictDetailModel.class);
        if (cacheModel != null){
            dictName = cacheModel.getDictName();
        }
        if (StringUtils.isNotEmpty(dictName)) return dictName;


        // 防止缓存穿透判断
        boolean hasNilFlag = CacheUtil.hasNilFlag("dict:" + typeCode + ":" + dictValue);
        if(hasNilFlag){
            return defaultVal;
        }



        // 锁凭证 redisLock 贯穿全程
        RedisLock redisLock = new RedisLock();
        redisLock.setLockName("dictLock:" + typeCode + ":" + dictValue)
                .setAcquireTimeOut(3000L)
                .setLockTimeOut(5000L);

        try {
            // 这里增加分布式锁 防止缓存击穿
            // ============ 尝试加锁
            redisLock = redisLockPlugins.tryLock(redisLock);
            if(redisLock == null){
                return defaultVal;
            }

            // 查询数据库 并保存到缓存内
            ResultVo<List<DictDetailModel>> resultVo = dictDetailApi.findListByTypeCode(typeCode);
            if(resultVo.isSuccess()){
                List<DictDetailModel> dictDetailModels = resultVo.getData();
                for (DictDetailModel model : dictDetailModels) {
                    if(model.getDictValue().equals(dictValue)){
                        // 名称
                        dictName = model.getDictName();
                        DictWrapper dictWrapperModel = new DictWrapper();
                        dictWrapperModel.setTypeCode(model.getTypeCode());
                        dictWrapperModel.setDictName(model.getDictName());
                        dictWrapperModel.setDictValue(model.getDictValue());
                        dictWrapperModel.setModel(model);
                        // 保存至缓存
                        DictUtil.put(dictWrapperModel);
                        break;
                    }
                }
            }

        }catch (Exception e){
            log.error(e.getMessage(),e);
            return defaultVal;
        }finally {
            // ============ 释放锁
            redisLockPlugins.unLock(redisLock);
            redisLock = null;
        }

        // 如果名称还是 为空 则赋默认值
        if(StringUtils.isEmpty(dictName)){
            // 加入缓存防穿透
            // 设置空变量 用于防止穿透判断
            CacheUtil.putNilFlag("dict:" + typeCode + ":" + dictValue);
            dictName = defaultVal;
        }
        return dictName;
    }

    /**
     * 根据 字典名称 取 字典值
     * @param typeCode 字典类型Code
     * @param dictName 字典名称
     * @param defaultVal 默认值
     * @return
     */
    public static String getDictValueByName(String typeCode, String dictName, String defaultVal){

        String dictValue = "";
        DictDetailModel cacheModel = CacheUtil.getHash(DictConstants.CACHE_PREFIX_NAME + typeCode,
                dictName, DictDetailModel.class);
        if (cacheModel != null){
            dictValue = cacheModel.getDictValue();
        }
        if (StringUtils.isNotEmpty(dictValue)) return dictValue;

        // 防止缓存穿透判断
        boolean hasNilFlag = CacheUtil.hasNilFlag("dict:" + typeCode + ":" + dictName);
        if(hasNilFlag){
            return defaultVal;
        }

        // 锁凭证 redisLock 贯穿全程
        RedisLock redisLock = new RedisLock();
        redisLock.setLockName("dictLock:" + typeCode + ":" + dictName)
                .setAcquireTimeOut(3000L)
                .setLockTimeOut(10000L);

        try {
            // 这里增加分布式锁 防止缓存击穿
            // ============ 尝试加锁
            redisLock = redisLockPlugins.tryLock(redisLock);
            if(redisLock == null){
                return defaultVal;
            }

            // 查询数据库 并保存到缓存内
            ResultVo<List<DictDetailModel>> resultVo = dictDetailApi.findListByTypeCode(typeCode);
            if(resultVo.isSuccess()){
                List<DictDetailModel> dictDetailModels = resultVo.getData();
                for (DictDetailModel model : dictDetailModels) {
                    if(model.getDictName().equals(dictName)){
                        // 值
                        dictValue = model.getDictValue();
                        DictWrapper dictWrapperModel = new DictWrapper();
                        dictWrapperModel.setTypeCode(model.getTypeCode());
                        dictWrapperModel.setDictName(model.getDictName());
                        dictWrapperModel.setDictValue(model.getDictValue());
                        dictWrapperModel.setModel(model);
                        // 保存至缓存
                        DictUtil.put(dictWrapperModel);
                        break;
                    }
                }
            }

        }catch (Exception e){
            log.error(e.getMessage(),e);
            return defaultVal;
        }finally {
            // ============ 释放锁
            redisLockPlugins.unLock(redisLock);
            redisLock = null;
        }


        // 如果值还是 为空 则赋默认值
        if(StringUtils.isEmpty(dictValue)){
            // 加入缓存防穿透
            // 设置空变量 用于防止穿透判断
            CacheUtil.putNilFlag("dict:" + typeCode + ":" + dictName);
            dictValue = defaultVal;
        }
        return dictValue;
    }

    /**
     * 根据字典code 获得字典列表
     * @param typeCode
     * @return
     */
    public static List<DictWrapper> getDictList(String typeCode){
        List<DictWrapper> dictWrapperModels = Lists.newArrayList();
        try {
            String key = CacheUtil.handleKey(CacheConstants.EDEN_HASH_DATA, DictConstants.CACHE_PREFIX_VALUE + typeCode);
            Map<Object, Object> dictMap = redisPlugin.hGetAll(key);
            Set<Map.Entry<Object, Object>> entries = dictMap.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                // 赋值
                JSONObject jsonObject = (JSONObject) entry.getValue();
                if(jsonObject == null){
                    continue;
                }
                DictDetailModel model = jsonObject.toJavaObject(DictDetailModel.class);
                DictWrapper dictWrapperModel = new DictWrapper();
                dictWrapperModel.setTypeCode(typeCode);
                dictWrapperModel.setDictName(model.getDictName());
                dictWrapperModel.setDictValue(model.getDictValue());
                dictWrapperModels.add(dictWrapperModel);
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
            dictWrapperModels = Lists.newArrayList();
        }
        return dictWrapperModels;
    }


    // ===============


    /**
     * 删除 字典
     * @param model 字典模型
     * @return
     */
    public static void put(DictWrapper model){
        CacheUtil.putEdenHash(DictConstants.CACHE_PREFIX_NAME + model.getTypeCode(),
                model.getDictName(), model.getModel());
        CacheUtil.putEdenHash(DictConstants.CACHE_PREFIX_VALUE + model.getTypeCode(),
                model.getDictValue(), model.getModel());
        // 删除 空属性 拦截
        CacheUtil.putNilFlag("dict:" + model.getTypeCode() + ":" + model.getDictName());
        CacheUtil.putNilFlag("dict:" + model.getTypeCode() + ":" + model.getTypeCode());
    }

    /**
     * 删除 字典
     * @param model 字典模型
     * @return
     */
    public static void del(DictWrapper model){
        CacheUtil.delEdenHash(DictConstants.CACHE_PREFIX_NAME + model.getTypeCode(), model.getDictName());
        CacheUtil.delEdenHash(DictConstants.CACHE_PREFIX_VALUE + model.getTypeCode(), model.getDictValue());
    }

    /**
     * 删除 typeCode 下所有字典
     * @param typeCode 字典编号
     * @return
     */
    public static void delAll(String typeCode){
        List<DictWrapper> dictWrapperList = DictUtil.getDictList(typeCode);
        for (DictWrapper dictWrapperModel : dictWrapperList) {
            DictUtil.del(dictWrapperModel);
        }
    }


    // ===================================

    @Autowired
    public  void setRedisPlugin(RedisPlugin redisPlugin) {
        DictUtil.redisPlugin = redisPlugin;
    }

    @Autowired
    public  void setRedisLockPlugins(RedisLockPlugins redisLockPlugins) {
        DictUtil.redisLockPlugins = redisLockPlugins;
    }

    @Autowired
    public  void setDictDetailApi(DictDetailApi dictDetailApi) {
        // DictUtil.dictDetailApi = dictDetailApi;
    }
}
