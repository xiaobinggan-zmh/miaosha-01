package com.miaoshaproject.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {  //我们就可以在我们的 应用程序 和 校验逻辑 之间通过ValidationResult做对应的对接

    //校验结果是否有错
    private boolean hasErrors = false;  //默认为false，不会有空指针错误的发生

    //存放错误信息的map
    private Map<String, String> errorMsgMap = new HashMap<>();



    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Map<String, String> getErrorMsgMap() {
        return errorMsgMap;
    }

    public void setErrorMsgMap(Map<String, String> errorMsgMap) {
        this.errorMsgMap = errorMsgMap;
    }



    //实现通用的通过格式化字符串信息获取错误结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errorMsgMap.values().toArray(), ",");   //values(),返回此映射中包含的值的 Collection 视图
        //toArray()返回包含此 collection 中所有元素的数组
    }





}
