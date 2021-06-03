package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class BaseController {  //将通用的异常处理机制放到基类里

    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";

    //定义exceptionhandler解决未被controller层吸收的exception异常
    //对于web等系统的异常来说，controller层的异常商业务处理的最后一道关口，我们要定义一种处理机制，符合spring钩子类处理的设计思想在里面
    @ExceptionHandler(Exception.class)   //指明当他收到什么样的exception之后才会进入他的处理环节，在这里我们定义的是exception的根类
    @ResponseStatus(HttpStatus.OK)  //应该是业务逻辑出的问题，而并非是服务端不能处理的错误导致返回500的错，
    // 所以我们在这里定义说即便我的controller抛出了exception之后捕获到他，并且返回一个叫HttpStatus.OK的那个200
    @ResponseBody
    //有了这个之后，我们再
    public Object handlerException(HttpServletRequest request, Exception ex){

        Map<String, Object> responseData = new HashMap<>();

        if(ex instanceof BusinessException){
            BusinessException businessException = (BusinessException)ex;  //强转
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        }else{
            System.out.println(ex.getMessage());//添加这句
            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
        }
        return CommonReturnType.create(responseData, "fail");   //替换为这个
//        CommonReturnType commonReturnType = new CommonReturnType();
//        commonReturnType.setStatus("fail");

//        commonReturnType.setData(responseData);
//        return commonReturnType;
    }
}
