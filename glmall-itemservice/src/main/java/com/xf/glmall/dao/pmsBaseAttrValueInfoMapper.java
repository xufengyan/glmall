package com.xf.glmall.dao;

import com.xf.glmall.entity.PmsBaseAttrInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface pmsBaseAttrValueInfoMapper {

    public List<PmsBaseAttrInfo> selectAttrValueListByValueIds(@Param("valueIds") String valueIds);
}
