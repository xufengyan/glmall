package com.xf.glmall.service;



import com.xf.glmall.entity.UmsMember;
import com.xf.glmall.entity.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
