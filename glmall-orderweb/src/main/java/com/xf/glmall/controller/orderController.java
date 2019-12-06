package com.xf.glmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xf.glmall.annotations.LoginRequired;
import com.xf.glmall.entity.OmsCartItem;
import com.xf.glmall.entity.OmsOrder;
import com.xf.glmall.entity.OmsOrderItem;
import com.xf.glmall.entity.UmsMemberReceiveAddress;
import com.xf.glmall.service.UserService;
import com.xf.glmall.service.orderService;
import com.xf.glmall.service.skuService;
import com.xf.glmall.util.MD5util;
import com.xf.glmall.utli.unitKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class orderController {



    @Reference
    skuService skuService;

    @Reference
    UserService userService;

    @Reference
    orderService orderService;

    @Autowired
    MD5util md5Util;


    /**
     * 结算页面
     * @param receiveAddressId
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String tradeCode,String receiveAddressId,ModelAndView model, HttpServletRequest request) {
        String memberId =(String) request.getAttribute("memberId");
        String nickname =(String) request.getAttribute("nickname");

        String proidSum="";
        List<OmsCartItem> omsCartItems = skuService.cartList(memberId);

        for (OmsCartItem omsCartItem : omsCartItems) {
            if(omsCartItem.getIsChecked() == 1){
                proidSum+=omsCartItem.getProductId();
            }
        }
        //检查交易码
        Boolean bool = orderService.checkTradeCode(memberId,proidSum,tradeCode);

        if(bool){//验证通过
            List<OmsOrderItem> omsOrderItems =new ArrayList<>();
            //订单对象
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("希望快点到");
            String outTradeNum="glmall";
            outTradeNum = outTradeNum + System.currentTimeMillis();
            SimpleDateFormat sfm=new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNum = outTradeNum + sfm.format(new Date());
            omsOrder.setOrderSn(outTradeNum);//外部订单号
            omsOrder.setTotalAmount(getTotalAmount(omsCartItems));
            omsOrder.setOrderType(1);
            //根据前段传递的地址id，查询详细地址
            UmsMemberReceiveAddress address = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(address.getCity());
            omsOrder.setReceiverDetailAddress(address.getDetailAddress());
            omsOrder.setReceiverName(address.getName());
            omsOrder.setReceiverPhone(address.getPhoneNumber());
            omsOrder.setReceiverPostCode(address.getPostCode());
            omsOrder.setReceiverProvince(address.getProvince());
            omsOrder.setReceiverRegion(address.getRegion());
            //java虚拟机自带时间计算工具
            Calendar c = Calendar.getInstance();
            //当前时间加一天
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setSourceType(0);
            omsOrder.setOrderType(0);

            //根据用户id，查询用户购物车选中的商品，对选中商家进行结算
            for (OmsCartItem omsCartItem : omsCartItems) {
                if(omsCartItem.getIsChecked() == 1){
                    OmsOrderItem omsOrderItem =new OmsOrderItem();
                    //检验价格
                    Boolean b = skuService.checkPrice(omsCartItem.getProductId(),omsCartItem.getPrice());
                    if(b){
                        model.addObject("message","商品价格不一致，请重新提交订单");
                        model.setViewName("tradeFail");
                    }
                    //检验库存,远程调用库存系统

                    //添加对用的订单详情
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setOrderSn(outTradeNum);//对外订单编号
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(null);
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());

                    omsOrderItems.add(omsOrderItem);
                }
            }
            //将订单详情加入订单中
            omsOrder.setOmsOrderItems(omsOrderItems);
            //将订单和订单详情添加入数据库
            orderService.saveOrder(omsOrder);
            //重定向到支付系统
            String submitCode = orderService.gentradeCode(memberId,omsOrder.getOrderSn());
            model.addObject("orderSn",omsOrder.getOrderSn());
            model.addObject("totalAmount",omsOrder.getTotalAmount());
            //支付时，反正重复支付的验证码
            model.addObject("submitCode",submitCode);
            model.setViewName("redirect:http://cart.xf.com:8185/paymemt/index.html");

        }else {
            //验证不通过
            model.addObject("message","请刷新页面或者重新提交订单");
            model.setViewName("tradeFail");
        }

        return model;
    }



        /**
         * 提交订单，必须登录才能访问
         * @param model
         * @param request
         * @return
         */
    @RequestMapping("toTrade.html")
    @LoginRequired(loginSuccess = true)
    public ModelAndView toTrade(ModelAndView model, HttpServletRequest request){

        String memberId =(String) request.getAttribute("memberId");
        String nickname =(String) request.getAttribute("nickname");
        List<OmsCartItem> omsCartItems = skuService.cartList(memberId);

        //将页面清单集合转换为清单集合
        List<OmsOrderItem> omsOrderItems =new ArrayList<>();
        String proidSum="";
        for (OmsCartItem omsCartItem : omsCartItems) {
            if(omsCartItem.getIsChecked() == 1){
                OmsOrderItem omsOrderItem =new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductId(omsCartItem.getProductId());
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItems.add(omsOrderItem);
                proidSum+=omsCartItem.getProductId();
            }
        }

        List<UmsMemberReceiveAddress> userAddressList = userService.getReceiveAddressByMemberId(memberId);

        BigDecimal totalAmount = getTotalAmount(omsCartItems);

        //生成交易码
        String tradeCode = orderService.gentradeCode(memberId,proidSum);

        model.addObject("omsOrderItems",omsOrderItems);
        model.addObject("userAddressList",userAddressList);
        model.addObject("totalAmount",totalAmount);
        model.addObject("tradeCode",tradeCode);
        model.setViewName("trade");
        return model;
    }


    /**
     * 计算选中的商品的总价
     *
     * @param omsCartItems
     * @return
     */
    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {

        BigDecimal totalAmount = new BigDecimal(0);
        for (OmsCartItem cartItem : omsCartItems) {
            if (cartItem.getIsChecked() == 1) {
                totalAmount = totalAmount.add(cartItem.getTotalPrice());
            }
        }
        return totalAmount;
    }
}
