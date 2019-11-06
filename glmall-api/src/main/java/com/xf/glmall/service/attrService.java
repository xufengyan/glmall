package com.xf.glmall.service;

import com.xf.glmall.entity.PmsBaseAttrInfo;

import java.util.List;
import java.util.Set;

public interface attrService {

    List<PmsBaseAttrInfo> getAttrValueListByValueIds(Set<String> valueIdSet);
}
