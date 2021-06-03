package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;


    @Override
    @Transactional
    // 假如orderDOMapper.insertSelective(orderDO);操作失败了对应整个事务的回滚，
    // 由于我把这些Mapper的操作也包含在这@Transactional标记的覆盖范围内，因此他也会回滚，这样可能下一个事务拿到的还是上一次sequence的值
    // 但是其实针对sequence的定义，就算是事务失败回滚了，这个sequence也不应该被重复的使用，这是为了保证全局唯一性的一个策略
    // 针对这种方式，我们怎么解决呢
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException {

        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确。
        ItemModel itemModel = itemService.getItemById(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
        UserModel userModel = userService.getUserById(userId);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
        }
        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "购买数量不正确");
        }

        //校验活动信息
        if(promoId != null){  //如果为null,则认为是普通商品的下单
            // 1校验对应活动是否存在这个适用商品
            //看传过来的秒杀模型id是否和商品模型中聚合的秒杀模型的id一致（该商品有秒杀活动，会将秒杀模型聚合进商品Model）
            if(promoId.intValue() != itemModel.getPromoModel().getId()){
                throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            //（2）校验活动是否正在进行中
            //即使id是秒杀模型的id，也不保险，还要校验是不是正在进行的秒杀
            }else if(itemModel.getPromoModel().getStatus() != 2){
                throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"轰动还未开始");
            }
        }

        //2.订单减库存
        //可以选择落单减库存，支付减库存（无法保证支付之后库存还有，不会锁库存）。
        //我们现在选择落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {   //如果返回值是false，说明减库存失败了
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);  //这个我们在EmBusinessError定义了
        }

        //3.订单入库
        //当我们成功的冻结了用户的库存，并且数量校验都通过了，我们就可以实行订单入库操作
        //创建OrderModel对象，封装数据
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);

        if(promoId != null){  //如果promoId != null时，应该取活动价格
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{  //否则取平时价格
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount))); //订单总价 商品单价X数量


        //生成交易流水号，也就是订单号
        orderModel.setId(generateOrderNo());
        //将OrderModel转为OrderDo
        OrderDO orderDO = convertFromOrderModel(orderModel);
        //保存订单
        orderDOMapper.insertSelective(orderDO);

        // 加上商品的销量
        itemService.increaseSales(itemId, amount);

        //4.返回前端
        return orderModel;

    }







//测试日期输出，为20210531
//    public static void main(String[] args){
//        LocalDateTime now = LocalDateTime.now();
//        System.out.println(now.format(DateTimeFormatter.ISO_DATE).replace("-",""));
//    }
    //生成交易流水号
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo() {

        // 订单号有16位
        StringBuilder stringBuilder = new StringBuilder();

        // 前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        //格式化后的格式是2018-12-12带横线的，将-去掉
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //中间6位为自增序列，某一天的某一个时间点，订单号不重复
        //数据库中创建一张自增序列表sequence_info
        //从sequence_info表中获取当前序列值
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        //获取库中当前的序列值
        sequence = sequenceDO.getCurrentValue();
        //获取当前之后，生成新的，步长+1
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStop());
        //之后马上更新表中的sequence
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);

        String sequenceStr = String.valueOf(sequence);

        //我们要凑足6位，就要看还差几位
        for(int i = 0; i < 6 - sequenceStr.length(); i++) {   //我们这个sequenceStr没有设置最大值，有可能超过六位
            //序列值前面的几位补0
            stringBuilder.append(0);
        }
        //将序列拼接上去
        //000001
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位，00-99，分库分表，订单水平拆分
        //订单信息落到拆分后的100个库的100张表中，分散数据库从查询和落单压力
        //订单号不变，这条订单记录一定会落到某一个库的某一张表上
        // Integer userId = 1000122;
        //userId % 100
        //暂时写死
        stringBuilder.append("00");

        return stringBuilder.toString();
    }





    //将Model转为dataObject
    private OrderDO convertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        //单独处理商品价格和订单金额
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
