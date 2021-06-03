package com.miaoshaproject.validator;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component   //实现为一个spring bean，进行类扫描的时候会扫描到他
public class ValidatorImpl implements InitializingBean {

    private Validator validator;  //这个是真正通过javax定义的一套接口实现的一个validator的工具



    //实现校验方法并返回校验结果
    public ValidationResult validate(Object bean){ //可以validate任意的bean对象

        //创建自定义的验证结果对象，用来封装是否错误和错误信息
        ValidationResult result = new ValidationResult();

        //调验证器的验证方法，返回一个set
        //也就是说，如果bean里面的一些参数的规则有违背了对应Validation定义的，，这个set里面就会有这个值
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);

        if(constraintViolationSet.size() > 0){

            //bean中有错误
            result.setHasErrors(true);

            //遍历constraintViolationSet
            constraintViolationSet.forEach(constraintViolation->{  //对应到他管道执行的方法，如果说有异常的话，首先获得异常的errMsg

                //获取bean的属性上注解定义的错误信息
                String errMsg = constraintViolation.getMessage();

                //获取是哪个属性有错误
                String propertyName = constraintViolation.getPropertyPath().toString();

                //将错误信息和对应的属性放入错误map里
                result.getErrorMsgMap().put(propertyName, errMsg);
            });
        }
        //将这个map返回
        return result;
    }



    @Override
    public void afterPropertiesSet() throws Exception {  //springbean初始化完成之后会回调ValidatorImpl的 afterPropertiesSet()方式
        //在这个方式里面

        //将hibernate validator通过工厂的初始化方法使其实例化
        //用这个方式，通过一个DefaultValidatorFactory我们build一个实例化的实现javax validator接口的校验器
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
