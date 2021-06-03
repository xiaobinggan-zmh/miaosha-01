package com.miaoshaproject.error;


import com.miaoshaproject.response.CommonReturnType;

//接下来我们需要把error信息取出来，也就是我们需要定义一个枚举
public enum EmBusinessError implements CommonError{


    //我们会定义一个通用的错误类型，通常是以10001开头,有了这个东西之后我们可以解决程序的必填校验，输入是不是邮箱号等，
    // 有个这个通用的错误码我们可以不用去定义无数的，但是我们对于errMsg的描述是需要有详尽的，可能是今天这个场景下对应用户名没传，
    // 在第二天另一个场景下是邮箱没传

    PARAMETER_VALIDATION_ERROR(10001, "参数不合法"),
    UNKNOWN_ERROR(10002, "未知错误"),

    //20000开头为用户信息相关错误定义，我们一般在做分布式开发的时候会遇到一些问题，全局需要有一个统一的状态码的流转
    //我们在企业级应用里往往存在我是负责用户模块，你是负责商品模块，我们两个之间的信息怎么互通？尤其是那些错误信息，需要标识一个全局错误码
    //当我们的系统越多，我们对应的错误妈的管理就要在一个通用的文件里面去做对应的管理，我们现在只不过是模仿了这样的做法
    //我们把2开头的所有信息定义为用户的错误码，然后我们需要一个构造函数，不然对于这样的请求是会报错的
    USER_NOT_EXIST(20001,"用户不存在"),  //定义一个简单的错误码
    USER_LOGIN_FAIL(20002, "用户手机号或密码不正确"),
    USER_NOT_LOGIN(20003, "用户还未登录"),


    //30000开头为交易信息错误定义
    STOCK_NOT_ENOUGH(30001,"库存不足"),
    ;




    //也就是说，当我对应的这个USER_NOT_EXIST被定义出来之后，他可以直接通过对应的构造方法构造出来一个实现CommonError接口的子类，
    // 这个子类是一个enum类型
    private EmBusinessError(int errCode, String errMsg){
        this.errCode = errCode;
        this.errMsg = errMsg;
    }


    //error枚举是可以拥有全员变量属性的，因为枚举本身就是一个面向对象的类
    private int errCode;
    private String errMsg;



//实现它三个方法
    @Override
    public int getErrCode() {
        return this.errCode;  //这里需要改写
    }

    @Override
    public String getErrMsg() {
        return this.errMsg; //这里需要改写
    }

    //所以我们需要一个接口去改动我们对应的errMsg
    @Override
    public CommonError setErrMsg(String errMsg) { //可以通过定制化的方式去改动他
        this.errMsg = errMsg;
        return this;
    }
}



