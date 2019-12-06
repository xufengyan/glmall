package com.xf.glmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.xf.glmall.annotations.LoginRequired;
import com.xf.glmall.util.HttpclientUtil;
import com.xf.glmall.utli.CookieUtil;
import jdk.nashorn.internal.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    /**
     * 拦截器
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //反射：通过类名，或者类对象获得类整体信息的过程

        String token = "";
        //拦截器
        //使用java反射获取当前方法的注解
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;

            //通过反射判断该方法有没有LoginRequired这个注解
            LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

            //是否拦截
            if (methodAnnotation == null) {
                return true;
            }
            //获取当前是否需要登录才能访问
            boolean loginSuccess = methodAnnotation.loginSuccess();

            String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
            String newToken = request.getParameter("token");
            if (StringUtils.isNotBlank(oldToken) || StringUtils.isNotBlank(newToken)) {
                //如果进来，表示用户登录过
                token = StringUtils.isNotBlank(newToken) ? newToken : oldToken;
            }
            String success = "false";
            Map<String,Object> successMap=new HashMap<>();
            if (StringUtils.isNotBlank(token)) {
                //通过nginx转发的客户端ip  x-forwarded-for
                String ip =request.getHeader("x-forwarded-for");
                if(StringUtils.isBlank(ip)){
                    ip =request.getRemoteAddr();
                    if(StringUtils.isBlank(ip)){
                        ip="127.0.0.1";
                    }
                }
                //调用认证中心进行验证
                //使用httpclient发送http请求
                String successJson = HttpclientUtil.doGet("http://cart.xf.com:8183/varify?token=" + token +"&currentIp="+ip);
                successMap = JSON.parseObject(successJson, Map.class);
                success = successMap.get("status").toString();
            }
            //必须登录才能访问
            if (loginSuccess) {
                //验证通过
                if ("success".equals(success)) {
                    request.setAttribute("memberId", successMap.get("memberId"));
                    request.setAttribute("nickname", successMap.get("nickname"));
                    //向cookie更新token
                    if(StringUtils.isNotBlank(token)){
                        CookieUtil.setCookie(request,response,"oldToken", token,60*60*2,true);
                    }

                } else {
                    //验证不通过
                    //重定向到登录页面
                    response.sendRedirect("http://cart.xf.com:8183/loginIndex.html?ReturnUrl=" + request.getRequestURL());
                    return false;
                }
            } else {//不登录也可以访问
                //如果验证通过
                if ("success".equals(success)) {
                    //也加入用户信息
                    request.setAttribute("memberId", successMap.get("memberId"));
                    request.setAttribute("nickname", successMap.get("nickname"));
                    //向cookie更新token
                    if(StringUtils.isNotBlank(token)) {
                        CookieUtil.setCookie(request,response,"oldToken", token,60*60*2,true);
                    }

                }
            }

        }

        return true;
    }


}