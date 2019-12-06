package com.xf.glmall.service;



import com.xf.glmall.entity.UmsMember;
import com.xf.glmall.entity.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember getlogin(UmsMember umsMember);

    void addUserToken(String token, String memberLevelId);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember checkUms);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
