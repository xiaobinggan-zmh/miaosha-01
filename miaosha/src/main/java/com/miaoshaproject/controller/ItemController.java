package com.miaoshaproject.controller;


import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Controller("item")
@RequestMapping("/item")
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class ItemController extends BaseController{

    @Autowired
    private ItemService itemService;


    //首先需要一个创建商品的过程
    //创建商品的controller,sales跟创建商品无关，不需要写
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name="description") String description,
                                       @RequestParam(name="price") BigDecimal price,
                                       @RequestParam(name="stock") Integer stock,
                                       @RequestParam(name="imgUrl") String imgUrl) throws BusinessException {


        //我们的设计目的是，尽可能让controller简单，让service层复杂,业务逻辑尽可能聚合在service内部，用于实现内部的流转处理
        //封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        //创建商品，返回itemModel
        ItemModel itemModelForReturn = itemService.createItem(itemModel);

        //返回给前端
        ItemVO itemVO = this.convertVOFromModel(itemModelForReturn);

        //将vo转json给页面
        return CommonReturnType.create(itemVO);
    }



    //商品详情页浏览
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {
        ItemModel itemModel = itemService.getItemById(id);

        ItemVO itemVO = this.convertVOFromModel(itemModel);

        return CommonReturnType.create(itemVO);
    }

    //商品列表信息浏览
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem() {

        List<ItemModel> itemModelList = itemService.listItem();

        // 使用stream api将list内的itemModel转化为itemVO
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());

        return CommonReturnType.create(itemVOList);
    }






    //这里itemVO和itemModel内容一样，为什么还需要？他们的定义不同，许多还会用到聚合操作
    //比如说model层到dataobject层已经用到了一些比如说库存字段是通过dataobject层聚合出来的
    //为了前端交互逻辑的方便，很多时候吧viewobject定义的比model层更大，比如说为了前端的性能考虑我们会聚合一些活动的价格信息
    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);

        if(itemModel.getPromoModel() != null){
            //有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            itemVO.setPromoStatus(0);
        }

        return itemVO;
    }

}







