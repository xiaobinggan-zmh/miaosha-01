package com.miaoshaproject.error;


public class BusinessException extends Exception implements CommonError{
    //我们定义的流程控制机制就是通过Exception的方式，也就是说我的程序出现任何跑不下去的异常
    //统一抛出一个Exception，这个Exception(编译期异常)会被最后的colltroller层的一个springboot的handler捕获处理

    //内部需要强关联一个对应的CommonError
    private CommonError commonError;


    //并且有一个构造函数可以方便我们去使用
    //这个构造方法的意义是直接接受EmBusinessError的传参用于构造业务异常
    public BusinessException(CommonError commonError){
        super(); //记得调用super方法，理由是对Exception自身会有一些初始化的方式在里面
        this.commonError = commonError;
    }



    //接收自定义errMsg的方式构造业务异常
    public BusinessException(CommonError commonError, String errMsg){ //接受一个CommonError的实现类，并且自定义一个errMsg
        super();
        this.commonError = commonError;
        this.commonError.setErrMsg(errMsg); //通过二次改写errMsg的方式去做
    }




    @Override
    public int getErrCode() {

        return this.commonError.getErrCode();  //这里需要改写
    }

    @Override
    public String getErrMsg() {

        return this.commonError.getErrMsg(); //这里需要改写
    }

    @Override
    public CommonError setErrMsg(String errMsg) {

        this.commonError.setErrMsg(errMsg); //这里需要改写
        return this;
    }
}
