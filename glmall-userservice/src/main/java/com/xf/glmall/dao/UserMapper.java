package com.xf.glmall.dao;

import com.xf.glmall.entity.UmsMember;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Component
public interface UserMapper extends Mapper<UmsMember> {

    List<UmsMember> selectAllUser();

}
