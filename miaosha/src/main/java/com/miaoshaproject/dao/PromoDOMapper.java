package com.miaoshaproject.dao;

import com.miaoshaproject.dataobject.PromoDO;

public interface PromoDOMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PromoDO record);

    int insertSelective(PromoDO record);

    PromoDO selectByPrimaryKey(Integer id);

    PromoDO selectByItemId(Integer itemId);  //新增的

    int updateByPrimaryKeySelective(PromoDO record);

    int updateByPrimaryKey(PromoDO record);
}