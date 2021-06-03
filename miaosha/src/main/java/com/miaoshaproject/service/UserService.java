package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;

public interface UserService {
    //通过用户id获取用户对象的方法
    UserModel getUserById(Integer id);

    //用户的注册流程用到的，需要一个service去处理对应用户注册的请求
    void register(UserModel userModel) throws BusinessException;  //在userModel完成用户注册的整套流程

    //用户登录服务
    //telphone是用户注册手机，password是用户加密后的密码
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
