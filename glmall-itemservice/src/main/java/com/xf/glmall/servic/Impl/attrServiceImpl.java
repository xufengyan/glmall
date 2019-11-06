package com.xf.glmall.servic.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.xf.glmall.dao.pmsBaseAttrValueInfoMapper;
import com.xf.glmall.entity.PmsBaseAttrInfo;
import com.xf.glmall.service.attrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Service
public class attrServiceImpl implements attrService {


    @Autowired
    pmsBaseAttrValueInfoMapper attrValueInfoMapper;


    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueIds(Set<String> valueIdSet) {
            //将set集合转换为一个string字符串 比如：1,3,4
        String valueIds = StringUtils.join(valueIdSet, ",");

        return attrValueInfoMapper.selectAttrValueListByValueIds(valueIds);
    }
}
