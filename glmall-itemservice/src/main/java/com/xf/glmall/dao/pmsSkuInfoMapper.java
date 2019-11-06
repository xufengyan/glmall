package com.xf.glmall.dao;

import com.xf.glmall.entity.PmsSkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface pmsSkuInfoMapper extends Mapper<PmsSkuInfo> {


    List<PmsSkuInfo> selectSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> selectSkuAll();
}
