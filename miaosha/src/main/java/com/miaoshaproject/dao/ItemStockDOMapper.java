package com.miaoshaproject.dao;

import com.miaoshaproject.dataobject.ItemStockDO;
import org.apache.ibatis.annotations.Param;

import javax.annotation.PostConstruct;

public interface ItemStockDOMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ItemStockDO record);

    int insertSelective(ItemStockDO record);

    ItemStockDO selectByPrimaryKey(Integer id);

    ItemStockDO selectByItemId(Integer itemid);  //新增的

    int decreaseStock(@Param("itemId")Integer itemId, @Param("amount")Integer amount);  //新增的，库存扣减

    int updateByPrimaryKeySelective(ItemStockDO record);

    int updateByPrimaryKey(ItemStockDO record);
}