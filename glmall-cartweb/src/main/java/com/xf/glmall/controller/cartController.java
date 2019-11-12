package com.xf.glmall.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.xf.glmall.entity.OmsCartItem;
import com.xf.glmall.entity.PmsSkuInfo;
import com.xf.glmall.service.skuService;
import com.xf.glmall.utli.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

@Controller
public class cartController {


    @Reference
    skuService skuService;


    /**
     * 修改购物车列表选中状态
     * @param model
     * @return
     */
    @RequestMapping("checkCart")
    public ModelAndView checkCart(Integer isChecked,String skuId,ModelAndView model) {


        String menberId="1";
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setMemberId(menberId);
//        修改商品选中状态
        skuService.checkCart(omsCartItem);
//        查询商品列表
        List<OmsCartItem> cartList = skuService.cartList(menberId);
        model.addObject("totalAmount",getTotalAmount(cartList));
        model.addObject("cartList",cartList);
        model.setViewName("cartListInner");
        return model;
    }


    /**
     * 查询购物车列表
     *
     * @param model
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("cartList")
    public ModelAndView cartList(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        String menberId = "1";
        if (StringUtils.isNotBlank(menberId)) {
            //如果用户存在，则去查询数据库
            omsCartItems = skuService.cartList(menberId);

        } else {
            //不存在的话就去查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(BigDecimal.valueOf(omsCartItem.getQuantity())));
        }
        model.addObject("cartList", omsCartItems);
        model.addObject("totalAmount",getTotalAmount(omsCartItems));
        model.setViewName("cartList");

        return model;

    }

    /**
     * 添加入购物车
     *
     * @param model
     * @param skuId    商品id
     * @param quantity 数量
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("addToCart")
    public ModelAndView addToCart(ModelAndView model, String skuId, Integer quantity, HttpServletRequest request, HttpServletResponse response) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();

        //查询该商品的信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        //将商品属性放入购物车类中
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setQuantity(quantity);
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());//默认图片
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductSkuCode("11111");

        String menberId = "1";

        //判断用户是否登录
        if (StringUtils.isNotBlank(menberId)) {//如果登录了就将数据放入数据库

            //数据库中查询购物车
            OmsCartItem omsCartItemFromDb = skuService.ifCartExistByUser(menberId, skuId);

            if (omsCartItemFromDb != null) {
                //如果当前商品用户添加过
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity() + quantity);
                skuService.updateCart(omsCartItemFromDb);

            } else {
                //如果当前商品用户没有添加过，则将这条购物车记录存放到数据库
                omsCartItem.setMemberId(menberId);
                int message = skuService.addCart(omsCartItem);
            }
            //查询当前用户的购物车数据，同步到cookie上
            List<OmsCartItem> omsCartItemsDb = skuService.flushCartCahe(menberId);


        } else {//没有登录则将数据放入cookie中

            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);

                //判断当前的数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems, omsCartItem);
                if (exist) {
                    //如果如果当前商品存在，则商品添加数量
                    for (OmsCartItem cartItem : omsCartItems) {

                        if (cartItem.getProductSkuId().equals(skuId)) {
                            cartItem.setQuantity(cartItem.getQuantity() + quantity);
                        }
                    }
                } else {
                    //否则则新增一条购物车记录
                    omsCartItems.add(omsCartItem);
                }
            } else {
                omsCartItems.add(omsCartItem);
            }
            //将数据放入cookie中
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItems), 60 * 60 * 72, true);
        }

        model.addObject("skuInfo", skuInfo);
        model.addObject("quantity", quantity);
        model.setViewName("success");
        return model;
//        return "redirect:success.html";
    }


    //判断cookie中是否存在这个商品记录
    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {

        Boolean bool = false;
        for (OmsCartItem cartItem : omsCartItems) {
            if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())) {
                bool = true;
            }
        }
        return bool;
    }


    /**
     * 计算选中的商品的总价
     * @param omsCartItems
     * @return
     */
    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems){

        BigDecimal totalAmount=new BigDecimal(0);
        for (OmsCartItem cartItem : omsCartItems) {
            if(cartItem.getIsChecked()==1){
                totalAmount = totalAmount.add(cartItem.getTotalPrice());
            }
        }

        return totalAmount;
    }


}
