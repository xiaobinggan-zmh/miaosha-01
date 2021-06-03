package com.miaoshaproject.controller;


import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


@Controller("user")   //用来被spring扫描到
@RequestMapping("/user")  //在url上需要通过这个路径访问
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //让我们完成返回web请求当中加上跨域allowhead的标签

public class UserController extends BaseController{   //去继承基类一样可以达到效果

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;  //通过bean的方式注入进来，
    // 这个HttpServletRequest是一个单例的模式，怎么支持多个用户的并发访问呢
    //通过springbean包装的HttpServletRequest，他的本质是一个process，内部拥有srcloval方式的map,让用户在每个线程中处理他自己的request,
    //并且有srcloval清除的机制，因此拿到这个东西可以放心的使用
    //这个HttpServletRequest就是对应当前用户的http请求



    //用户登录接口
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //入参校验
        if(org.apache.commons.lang3.StringUtils.isEmpty(telphone) ||
                org.apache.commons.lang3.StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMD5(password));

        //将登陆凭证加入到用户登陆成功的session内，后面我们会讲到采用分布式会话session的机制去解决在分布式环境下的用户登录的一些问题
        //在这个课程中我们假设用户是单点的登录
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);

        //并且给前端返回一个正确的信息
        return CommonReturnType.create(null);

    }


    //用户注册接口
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telphone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     //刚才发送到用户手机上的otpCode，用来标识对应用户的手机号的确是用户自带的而不是伪造的
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Integer gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpcode相符合
        String inSessionOtpCode = (String)this.httpServletRequest.getSession().getAttribute(telphone);
        //这里session取出来的是一个object所以需要强转为String

        //将inSessionOtpCode和otpCode进行比对
        if(!StringUtils.equals(otpCode, inSessionOtpCode)){
            //为什么用类库中的equals,因为内部给我们做了一个判空的处理
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不符合");
        }//如果是合法的就进入用户的注册流程


        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMD5(password));  //传上来的是个明文的password,我们需必须要加密的password存到数据库里

        userService.register(userModel);
        return CommonReturnType.create(null);  //返回一个注册成功

    }




    //对密码进行MD5加密修改
    public String EncodeByMD5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        String newstr = base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }





    //用户获取otp短信接口
    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    //{}这个括起来的意思是数组
    //意思是我的这个getotp方法，必须映射到http的post请求才能够生效
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="telphone")String telphone){
        //需要按照一定的规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);

        //将OTP验证码通对应用户的手机号关联
        //建立关联的方式有很多，需要有key value对的属性，当用户反复点击getOtp时，可以做反复的覆盖，使用httpsession的方式，或者分布式的方式，
        // 这里我们使用第一种，使用此方式绑定他的手机号和otpcode
        httpServletRequest.getSession().setAttribute(telphone,otpCode);

        //将OTP验证码通过短信通道发送给用户，省略
        //为了我们程序调试的方便
        System.out.println("telphone = " + telphone + " &otpCode = " + otpCode);

        return CommonReturnType.create(null);
    }





    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id")Integer id) throws BusinessException {  //输入定义为id
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);//我们讲一个领域模型返回给了前端用户，这是非常不科学的

        //若获取的对应用户信息不存在
        //我们下一个要解决的问题是，通过springboot自带的springmvc的handelexception去解决一个通用的异常处理方式
        if(userModel == null){
            //userModel.setEncrptPassword("123");  //不爆出异常了，让程序抛出一个空指针
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
            //这个直接被抛到tomcat的容器层，容器层处理异常的方式是返回500的错误页，
            //我们有没有一种机制可以拦截掉对应tomcat的异常处理方式，然后去解决掉对应的问题，springboot提供一种通用的解决方式
        }

        //将核心领域模型用户对象转化为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }



    //将Model转为VO
    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}