package com.miaoshaproject.service.impl;


import com.miaoshaproject.dao.ItemDOMapper;
import com.miaoshaproject.dao.ItemStockDOMapper;
import com.miaoshaproject.dataobject.ItemDO;
import com.miaoshaproject.dataobject.ItemStockDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import jdk.nashorn.internal.ir.GetSplitState;
import org.hibernate.validator.constraints.br.TituloEleitoral;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    //引入自定义的校验器
    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;




    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        ////首先进行入参校验
        ValidationResult validationResult = validator.validate(itemModel);
        if (validationResult.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }


        //将ItemMode -> ItemDO（dataObject）
        ItemDO itemDO = this.convertItemDOFromItemModel(itemModel);


        ////将ItemDO写入数据库
        //写入后返回了itemDO的id
        this.itemDOMapper.insertSelective(itemDO);


        //将id给itemModel
        itemModel.setId(itemDO.getId());

        //itemModel -> itemStockDO
        ItemStockDO itemStockDO = this.convertItemStockDOFromModel(itemModel);

        //将ItemStockDO写入数据库
        this.itemStockDOMapper.insertSelective(itemStockDO);  //老师这里没写this

        //返回创建完成的对象,通过调getItemById完成
        return this.getItemById(itemModel.getId());
    }


    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = this.convertModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }




    //根据商品id查询商品,先查出itemDO,再查出对应的stock，封装成itemModel
    @Override
    public ItemModel getItemById(Integer id) {   //根据id查询商品
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null) {
            return null;
        }

        // 操作获得库存数量
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        // 将dataObject->model
        ItemModel itemModel = this.convertModelFromDataObject(itemDO, itemStockDO);



        //获取活动商品信息，判断商品是否在秒杀活动内
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());

        if(promoModel != null && promoModel.getStatus().intValue() != 3){  //这个商品有秒杀活动
            itemModel.setPromoModel(promoModel);  //通过这种方式，我们采用了模型聚合的方式，将秒杀商品和秒杀活动关联起来
        }

        return itemModel;
    }


    //库存扣减
    @Override
    @Transactional    //保证事物的一致性的
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);  //这样写性能会好很多
        if (affectedRow > 0) {
            // 更新库存成功
            return true;
        } else {
            // 更新库存失败
            return false;
        }
    }



    //商品销量增加
    @Override
    @Transactional  //这个操作也要在事务内
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }




    //将ItemModel转为ItemDO的转换方法
    private ItemDO convertItemDOFromItemModel(ItemModel itemModel) {

        if (itemModel == null) {
            return null;
        }

        ItemDO itemDO = new ItemDO();

        //UserModel中的price是BigDecimal类型而不用Double，Double在java内部传到前端，会有精度问题，不精确
        //有可能1.9，显示时是1.999999，为此在Service层，将price定为比较精确的BigDecimal类型
        //但是在拷贝到Dao层时，存入的是Double类型，拷贝方法对应类型不匹配的属性，不会进行拷贝。
        //在拷贝完，将BigDecimal转为Double，再set进去
        BeanUtils.copyProperties(itemModel, itemDO);

        //转为double
        itemDO.setPrice(itemModel.getPrice().doubleValue());

        return itemDO;
    }


    //从itemModel中取stock和id转为ItemStockDO的方法
    private ItemStockDO convertItemStockDOFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());

        return itemStockDO;
    }



    private ItemModel convertModelFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));  //这里疑惑
        itemModel.setStock(itemStockDO.getStock());

        return itemModel;
    }

}