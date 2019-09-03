package com.xf.glmall.controller;

import com.xf.glmall.entity.UmsMember;
import com.xf.glmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("glmall.user")
public class userController {

    @Autowired
    private UserService userService;

    @RequestMapping("selectUser")
    @ResponseBody
    public String selectUser(){
        return "我类个去";
    }


    @RequestMapping("index")
    @ResponseBody
    public List<UmsMember> selectUserby(){
        List<UmsMember> list =userService.getAllUser();
        return list;
    }
}
