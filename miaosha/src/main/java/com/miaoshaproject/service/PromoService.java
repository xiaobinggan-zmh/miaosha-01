package com.miaoshaproject.service;

import com.miaoshaproject.service.model.PromoModel;

public interface PromoService {

    //根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);  //我们应该提供这样一种服务给商品服务做聚合
    //也就是我的秒杀服务要根据当前的商品idn能够查询出即将要开始的活动，并且展示出对应的价格




}
