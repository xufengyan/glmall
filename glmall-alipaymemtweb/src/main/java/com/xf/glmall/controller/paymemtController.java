package com.xf.glmall.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.sun.javafx.scene.control.skin.LabeledImpl;
import com.xf.glmall.annotations.LoginRequired;
import com.xf.glmall.config.AlipayConfig;
import com.xf.glmall.entity.OmsOrder;
import com.xf.glmall.entity.PaymentInfo;
import com.xf.glmall.service.orderService;
import com.xf.glmall.service.paymemtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ThemeResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("paymemt")
@Controller
public class paymemtController {


    @Autowired
    AlipayClient alipayClient;

    @Reference
    orderService orderService;

    @Reference
    paymemtService paymemtSercice;

    /**
     * 跳转到支付提交页面
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("index.html")
    @LoginRequired(loginSuccess = true)
    public ModelAndView paymemtIndex(String orderSn,String submitCode, ModelAndView model, HttpServletRequest request){

        String memberId = (String)request.getAttribute("memberId");
        String nickname =(String) request.getAttribute("nickname");
        //生成提交订单的交易码
        //
        model.addObject("submitCode",submitCode);
        model.addObject("orderSn",orderSn);

        model.setViewName("index");
        return model;
    }

    /**
     * 支付宝支付
     * @return
     */
    @RequestMapping("alipay")
    @LoginRequired (loginSuccess = true)
    @ResponseBody
    public ModelAndView alipay(ModelAndView model, String orderSn, String submitCode, HttpServletRequest request, HttpServletResponse response) throws IOException {

        String message = "";
        String memberId = (String)request.getAttribute("memberId");
        String nickname =(String) request.getAttribute("nickname");
        Boolean code = orderService.checkTradeCode(memberId, orderSn, submitCode);
        if(code){
            OmsOrder order =new OmsOrder();
            order.setOrderSn(orderSn);
            OmsOrder omsOrder = orderService.getOrderByorderSn(order);
            String form =null;
            if(null!=omsOrder){
                //支付宝返回的不是一个连接，而是一个form表单

                //创建api对应点request
                AlipayTradePagePayRequest alipayrRequest = new AlipayTradePagePayRequest();
                //回调函数
                alipayrRequest.setReturnUrl(AlipayConfig.return_payment_url);
                alipayrRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
                Map<String,Object> map =new HashMap<>();
                map.put("out_trade_no",orderSn);
                map.put("product_code","FAST_INSTANT_TRADE_PAY");
                map.put("total_amount",0.01);
                map.put("subject","我的法克5G超牛逼手机");
                alipayrRequest.setBizContent(JSON.toJSONString(map));
                try {
                    form = alipayClient.pageExecute(alipayrRequest).getBody();


                }catch (AlipayApiException e){
                    e.printStackTrace();
                }
                //保存支付前信息
                PaymentInfo paymentInfo =new PaymentInfo();
                paymentInfo.setCreateTime(new Date());
                paymentInfo.setOrderId(omsOrder.getId());
                paymentInfo.setOutTradeNo(omsOrder.getOrderSn());
                paymentInfo.setPaymentStatus("未付款");
                paymentInfo.setTotalAmount(omsOrder.getTotalAmount());
                int mag = paymemtSercice.savePaymemtInfo(paymentInfo);


                //将返回的支付宝返回的form输出到页面，
                //直接返回form会验证不通过
                try {
                    response.setContentType("text/html;charset=utf-8");
                    response.getWriter().write(form);//直接将完整的表单html输出到页面
                    response.getWriter().flush();
                }catch (IOException o){
                    o.printStackTrace();
                }finally {
                    response.getWriter().close();
                }



            }else{

                message = "订单不存在，请重新提交订单";
            }
        } else {
            message = "订单已失效，请重新提交订单";
        }
        model.setViewName("failure");
        return model;
    }


    /**
     * 支付宝支付成功后的回调
     * @param model
     * @return
     */
    @RequestMapping("/alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public ModelAndView alipayCallBackReturn(ModelAndView model){

        return model;
    }





}
