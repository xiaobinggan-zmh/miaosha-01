package com.miaoshaproject.response;

import java.time.Period;
import java.util.concurrent.Callable;

public class CommonReturnType {   //response用来新建软件包用来处理http返回的
    //表明对应请求的返回处理结果，"success"或"fail"
    private String status;  //通过status让前端判定这个请求服务端有没有正确的受理

    //若status=success,则data内返回前端需要的json数据
    //若status=fail,则data内使用通用的错误码格式
    private Object data;  //让前端获获取到有意义的信息


    //定义一个通用的创建方法
    public static CommonReturnType create(Object result){
        //定义一个二重构的函数
        return CommonReturnType.create(result,"success");
    }

    //这个方法的意义是当我的controller完成了处理,我调用对应的creat方法，如果不带任何status的话，对应的status就是success,
    //然后创建CommonReturnType并且把对应的值返回
    public static CommonReturnType create(Object result, String status){  //使用一个函数重载的方式

        CommonReturnType type = new CommonReturnType();
        type.setStatus(status);
        type.setData(result);
        return type;
    }




    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
