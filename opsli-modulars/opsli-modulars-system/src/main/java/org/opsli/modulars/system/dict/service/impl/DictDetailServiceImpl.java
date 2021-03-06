/**
 * Copyright 2020 OPSLI 快速开发平台 https://www.opsli.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opsli.modulars.system.dict.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.opsli.api.wrapper.system.dict.DictDetailModel;
import org.opsli.api.wrapper.system.dict.DictModel;
import org.opsli.api.wrapper.system.dict.DictWrapper;
import org.opsli.common.constants.MyBatisConstants;
import org.opsli.common.exception.ServiceException;
import org.opsli.common.utils.HumpUtil;
import org.opsli.core.base.service.impl.CrudServiceImpl;
import org.opsli.core.cache.pushsub.enums.CacheType;
import org.opsli.core.cache.pushsub.msgs.DictMsgFactory;
import org.opsli.core.msg.CoreMsg;
import org.opsli.core.persistence.querybuilder.GenQueryBuilder;
import org.opsli.core.persistence.querybuilder.QueryBuilder;
import org.opsli.core.utils.DictUtil;
import org.opsli.modulars.system.SystemMsg;
import org.opsli.modulars.system.dict.entity.SysDictDetail;
import org.opsli.modulars.system.dict.mapper.DictDetailMapper;
import org.opsli.modulars.system.dict.service.IDictDetailService;
import org.opsli.modulars.system.dict.service.IDictService;
import org.opsli.plugins.redis.RedisPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


/**
 * @BelongsProject: opsli-boot
 * @BelongsPackage: org.opsli.modulars.test.service
 * @Author: Parker
 * @CreateTime: 2020-09-16 17:34
 * @Description: 数据字典 明细 接口实现类
 */
@Service
public class DictDetailServiceImpl extends CrudServiceImpl<DictDetailMapper, SysDictDetail, DictDetailModel> implements IDictDetailService {

    @Autowired(required = false)
    private DictDetailMapper mapper;
    @Autowired
    private IDictService iDictService;
    @Autowired
    private RedisPlugin redisPlugin;

    /**
     * 新增
     * @param model model 数据模型
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DictDetailModel insert(DictDetailModel model) {

        SysDictDetail entity = super.transformM2T(model);
        // 唯一验证
        Integer count = mapper.uniqueVerificationByNameOrValue(entity);
        if(count != null && count > 0){
            // 重复
            throw new ServiceException(SystemMsg.EXCEPTION_DICT_DETAIL_UNIQUE);
        }

        DictDetailModel ret = super.insert(model);
        if(ret != null){
            List<DictDetailModel> listByTypeCode = this.findListByTypeCode(ret.getTypeCode());
            if(listByTypeCode != null && listByTypeCode.size() > 0){
                List<DictWrapper> dictWrapperList = Lists.newArrayListWithCapacity(listByTypeCode.size());
                for (DictDetailModel dictDetailModel : listByTypeCode) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(dictDetailModel.getTypeCode());
                    dictWrapperModel.setDictName(dictDetailModel.getDictName());
                    dictWrapperModel.setDictValue(dictDetailModel.getDictValue());
                    dictWrapperList.add(dictWrapperModel);
                }
                // 删除缓存
                this.clearCache(Collections.singletonList(
                        ret.getTypeCode()
                ));

                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperList, CacheType.DELETE)
                );
            }
        }

        return ret;
    }

    /**
     * 修改
     * @param model model 数据模型
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DictDetailModel update(DictDetailModel model) {

        SysDictDetail entity = super.transformM2T(model);
        // 唯一验证
        Integer count = mapper.uniqueVerificationByNameOrValue(entity);
        if(count != null && count > 0){
            // 重复
            throw new ServiceException(SystemMsg.EXCEPTION_DICT_DETAIL_UNIQUE);
        }

        // 旧数据 用于删除老缓存
        DictDetailModel oldModel = this.get(model);

        DictDetailModel ret = super.update(model);
        if(ret != null){
            List<DictDetailModel> listByTypeCode = this.findListByTypeCode(oldModel.getTypeCode());
            if(listByTypeCode != null && listByTypeCode.size() > 0){
                List<DictWrapper> dictWrapperList = Lists.newArrayListWithCapacity(listByTypeCode.size());
                for (DictDetailModel dictDetailModel : listByTypeCode) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(dictDetailModel.getTypeCode());
                    dictWrapperModel.setDictName(dictDetailModel.getDictName());
                    dictWrapperModel.setDictValue(dictDetailModel.getDictValue());
                    dictWrapperList.add(dictWrapperModel);
                }
                // 删除缓存
                this.clearCache(Collections.singletonList(
                        oldModel.getTypeCode()
                ));

                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperList, CacheType.DELETE)
                );
            }
        }

        return ret;
    }


    /**
     * 删除
     * @param id ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        DictDetailModel dictModel = this.get(id);
        boolean ret = super.delete(id);
        if(ret){
            List<DictDetailModel> listByTypeCode = this.findListByTypeCode(dictModel.getTypeCode());
            if(listByTypeCode != null && listByTypeCode.size() > 0){
                List<DictWrapper> dictWrapperList = Lists.newArrayListWithCapacity(listByTypeCode.size());
                for (DictDetailModel dictDetailModel : listByTypeCode) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(dictDetailModel.getTypeCode());
                    dictWrapperModel.setDictName(dictDetailModel.getDictName());
                    dictWrapperModel.setDictValue(dictDetailModel.getDictValue());
                    dictWrapperList.add(dictWrapperModel);
                }
                // 删除缓存
                this.clearCache(Collections.singletonList(
                        dictModel.getTypeCode()
                ));
                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperList, CacheType.DELETE)
                );
            }
        }
        return ret;
    }

    /**
     * 删除
     * @param model 数据模型
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DictDetailModel model) {
        DictDetailModel dictModel = this.get(model);
        boolean ret = super.delete(model);
        if(ret){
            // 删除缓存
            this.clearCache(Collections.singletonList(
                    dictModel.getTypeCode()
            ));

            List<DictDetailModel> listByTypeCode = this.findListByTypeCode(dictModel.getTypeCode());
            if(listByTypeCode != null && listByTypeCode.size() > 0){
                List<DictWrapper> dictWrapperList = Lists.newArrayListWithCapacity(listByTypeCode.size());
                for (DictDetailModel dictDetailModel : listByTypeCode) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(dictDetailModel.getTypeCode());
                    dictWrapperModel.setDictName(dictDetailModel.getDictName());
                    dictWrapperModel.setDictValue(dictDetailModel.getDictValue());
                    dictWrapperList.add(dictWrapperModel);
                }
                // 删除缓存
                this.clearCache(Collections.singletonList(
                        dictModel.getTypeCode()
                ));
                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperList, CacheType.DELETE)
                );
            }
        }
        return ret;
    }

    /**
     * 删除 - 多个
     * @param ids id数组
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAll(String[] ids) {
        QueryBuilder<SysDictDetail> queryBuilder = new GenQueryBuilder<>();
        QueryWrapper<SysDictDetail> queryWrapper = queryBuilder.build();
        List<?> idList = Convert.toList(ids);
        queryWrapper.in(HumpUtil.humpToUnderline(MyBatisConstants.FIELD_ID),idList);
        List<SysDictDetail> list = this.findList(queryWrapper);
        boolean ret = super.deleteAll(ids);

        if(ret){
            if(list != null && list.size() > 0){
                List<DictWrapper> dictWrapperModels = Lists.newArrayListWithCapacity(list.size());
                Set<String> typeCodes = new HashSet<>();
                // 封装数据
                for (SysDictDetail sysDictDetail : list) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(sysDictDetail.getTypeCode());
                    dictWrapperModel.setDictName(sysDictDetail.getDictName());
                    dictWrapperModel.setDictValue(sysDictDetail.getDictValue());

                    dictWrapperModels.add(dictWrapperModel);

                    typeCodes.add(dictWrapperModel.getTypeCode());
                }

                List<String> typeCodeList = Lists.newArrayListWithCapacity(typeCodes.size());
                typeCodeList.addAll(typeCodes);

                // 删除缓存
                this.clearCache(typeCodeList);

                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperModels, CacheType.DELETE)
                );
            }

        }
        return ret;
    }

    /**
     * 删除 - 多个
     * @param models 封装模型
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAll(Collection<DictDetailModel> models) {

        QueryBuilder<SysDictDetail> queryBuilder = new GenQueryBuilder<>();
        QueryWrapper<SysDictDetail> queryWrapper = queryBuilder.build();

        List<String> idList = Lists.newArrayListWithCapacity(models.size());
        for (DictDetailModel model : models) {
            idList.add(model.getId());
        }
        queryWrapper.in(HumpUtil.humpToUnderline(MyBatisConstants.FIELD_ID),idList);

        List<SysDictDetail> list = this.findList(queryWrapper);

        boolean ret = super.deleteAll(models);

        if(ret){
            if(list != null && list.size() > 0){
                List<DictWrapper> dictWrapperModels = Lists.newArrayListWithCapacity(list.size());
                Set<String> typeCodes = new HashSet<>();
                // 封装数据
                for (SysDictDetail sysDictDetail : list) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(sysDictDetail.getTypeCode());
                    dictWrapperModel.setDictName(sysDictDetail.getDictName());
                    dictWrapperModel.setDictValue(sysDictDetail.getDictValue());

                    dictWrapperModels.add(dictWrapperModel);

                    typeCodes.add(dictWrapperModel.getTypeCode());
                }

                List<String> typeCodeList = Lists.newArrayListWithCapacity(typeCodes.size());
                typeCodeList.addAll(typeCodes);

                // 删除缓存
                this.clearCache(typeCodeList);

                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperModels, CacheType.DELETE)
                );
            }
        }

        return ret;
    }

    /**
     * 根据 父类ID 全部删除
     * @param parentId 父类ID
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delByParent(String parentId) {
        if(StringUtils.isEmpty(parentId)){
            return false;
        }

        String key = HumpUtil.humpToUnderline("typeId");
        QueryBuilder<SysDictDetail> queryBuilder = new GenQueryBuilder<>();
        QueryWrapper<SysDictDetail> queryWrapper = queryBuilder.build();
        queryWrapper.eq(key, parentId);
        boolean removeFlag = super.remove(queryWrapper);
        if(removeFlag){
            DictModel dictModel = iDictService.get(parentId);
            List<DictDetailModel> listByTypeCode = this.findListByTypeCode(dictModel.getTypeCode());
            if(listByTypeCode != null && listByTypeCode.size() > 0){
                List<DictWrapper> dictWrapperList = Lists.newArrayListWithCapacity(listByTypeCode.size());
                for (DictDetailModel dictDetailModel : listByTypeCode) {
                    DictWrapper dictWrapperModel = new DictWrapper();
                    dictWrapperModel.setTypeCode(dictDetailModel.getTypeCode());
                    dictWrapperModel.setDictName(dictDetailModel.getDictName());
                    dictWrapperModel.setDictValue(dictDetailModel.getDictValue());
                    dictWrapperList.add(dictWrapperModel);
                }
                // 删除缓存
                this.clearCache(Collections.singletonList(
                        dictModel.getTypeCode()
                ));
                // 广播缓存数据 - 通知其他服务器同步数据
                redisPlugin.sendMessage(
                        DictMsgFactory.createMsg(dictWrapperList, CacheType.DELETE)
                );
            }
        }
        return removeFlag;
    }

    /**
     * 根据字典编号 查询出所有字典
     *
     * @param typeCode 字典编号
     * @return
     */
    @Override
    public List<DictDetailModel> findListByTypeCode(String typeCode) {
        if(StringUtils.isEmpty(typeCode)){
            return null;
        }

        String key = HumpUtil.humpToUnderline("typeCode");
        String deleted = HumpUtil.humpToUnderline("deleted");

        QueryBuilder<SysDictDetail> queryBuilder = new GenQueryBuilder<>();
        QueryWrapper<SysDictDetail> queryWrapper = queryBuilder.build();
        queryWrapper.eq(key, typeCode);
        queryWrapper.eq(deleted, '0');
        queryWrapper.orderByAsc("sort_no");
        List<SysDictDetail> list = this.findList(queryWrapper);
        // 转化对象
        return super.transformTs2Ms(list);
    }


    // ================

    /**
     * 清除缓存
     * @param typeCodeList
     */
    private void clearCache(List<String> typeCodeList) {
        // 删除缓存
        if (CollUtil.isNotEmpty(typeCodeList)) {
            int cacheCount = 0;
            for (String typeCode : typeCodeList) {
                cacheCount++;
                boolean tmp = DictUtil.delAll(typeCode);
                if(tmp){
                    cacheCount--;
                }
            }
            // 判断删除状态
            if(cacheCount != 0){
                // 删除缓存失败
                throw new ServiceException(CoreMsg.CACHE_DEL_EXCEPTION);
            }
        }
    }
}


