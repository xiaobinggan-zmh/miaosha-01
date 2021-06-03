package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service   //指明为一个service
public class UserServiceImpl implements UserService {  //说明可以通过UserService获取到用户领域模型的对象

    @Autowired
    private UserDOMapper userDOMapper;   //把我们的UserDOMapper类引进来

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;


    @Override
    public UserModel getUserById(Integer id) {   //重写方法

        //调用userDOMapper获取到对应的用户dataobject
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);  //根据主键id获取到UserDO的对象
        //这个UserDO是不能透传给前端的，因为每一层都要有每一层的设计思想，就是springmvc中的一个model的定义，
        // 在我们做互联网项目需要三层，第一层叫做dataobject,是与数据库完完全全一一映射的，
        // 但是在service层，不能简单的把数据库的一一映射透传给想要这个service的服务，也就是说在service层必须有一个model的概念

        //我们这边的业务逻辑是需要通过用户id查询到加密的密码，所以需要改造UserPasswordDOMapper自动生成的，mapping里的文件
        if(userDO == null){
            return null;
        }
        //通过用户id查询到加密的密码信息
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());

        return convertFromDataObject(userDO, userPasswordDO); //通过DataObject把我们的usermodel转出来，并且返回给controller
    }



    //用户注册
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {

        if(userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

//        if(StringUtils.isEmpty(userModel.getName())
//                //为什么使用StringUtils，对于我们的应用程序来说，空字符串就相当于用户没有填写对应的信息，因此要判断两次
//                || userModel.getGender() == null
//                || userModel.getAge() == null
//                || StringUtils.isEmpty(userModel.getTelphone())){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
//        }

        //使用validator校验userModel的各个属性是否符合校验规则
        ValidationResult result = validator.validate(userModel);

        //验证userModel，如果有错误，会将这个boolean置为true
        if(result.isHasErrors()){
            //抛异常，封装错误信息
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }



        //把用户的注册信息注册进去

        //实现model -> dataobject,因为我们DAO层认的是dataobject
        UserDO userDO = convertFromModel(userModel);

        try{   //用户信息注册 insert 插入
            userDOMapper.insertSelective(userDO);  //为什么使用insertSelective而不使用insert方法？
            // 他会首先判断对应的字段在dataobject里是否为null,入股不为null就执行insert操作，如果为null的话就跳过，就是不insert这个字段，
            //意义就是数据库提供什么默认值，我就提供什么默认值，如果说我们采用的时insert的话，对应字段如果是null，就会用null字段覆盖掉数据库里的默认值
            //这个操作在updata里尤其有用
        }catch(DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已重复注册");
        }


        //给userModel设置id
        userModel.setId(userDO.getId());   //我少了这一句，所以一直出错
        //将自增id取出来之后复制给对应的userModel，以便于转发给userPasswordDO，防止密码表这找不到对应的用户表id



        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);  //密码注册

        return;
    }


    //登录
    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {

        //通过用户的手机获取用户信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);

        if(userDO == null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());

        //将dataobject -> model
        UserModel userModel = convertFromDataObject(userDO,userPasswordDO);

        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        //我们把刚才用户加密的密码导进来
        if(!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;  //相等的话直接return,如果用户登陆成功，我们将这个userModel返回给controll层
    }




    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }


    private UserDO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);

        return userDO;
    }


    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        //通过UserDO和UserPasswordDO可以完美组装成一个UserModel的对象

        if(userDO == null){
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);  //把userDO的属性copy到userModel内

        if(userPasswordDO != null){
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }

        return userModel;
    }
}

