package com.xf.glmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.sun.org.apache.regexp.internal.RE;
import com.sun.org.apache.xpath.internal.operations.Mod;
import com.xf.glmall.entity.UmsMember;
import com.xf.glmall.service.UserService;
import com.xf.glmall.util.HttpclientUtil;
import com.xf.glmall.util.MD5util;
import com.xf.glmall.utli.JwtUtil;
import com.xf.glmall.utli.weiboKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class loginController {


    String jwtKey = "xfglmall2019";
    String md5key = "login";

    @Reference
    UserService userService;

    @Autowired
    MD5util md5Util;


    /**
     * 第三方登录回调地址
     * @param code
     * @param request
     * @param state
     * @return
     */
    @RequestMapping("thirdPartylogin")
    public ModelAndView thirdPartylogin(String code,String state, HttpServletRequest request, ModelAndView model){
        //第一步，用户授权登录
        // 网页上点击 https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=clentId&redirect_uri="返回的地址"&state="xfglmall"

        //第二步，通过回调地址访问当前网站的地址 http://cart.xf.com:8183/thirdPartylogin
        //获取到code  -----code会在10分钟内过期


        //第三步，通过code获取到access_token验证
        //grant_type 授权类型 固定值为authorization_code
        //client_id 审核通过后分配的网站的appid
        //client_secret 审核通过后分配的网站的appkey
        //code 上一步获取的code
        //redirect_uri 返回的地址

        String accessJsonStr = HttpclientUtil.doGet("https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=&client_secret=&code=&redirect_uri=");

        Map<String,Object> accessMap = JSON.parseObject(accessJsonStr,Map.class);

        //第四步，通过access_token获取到qq上的用户信息
        String userJsonStr = HttpclientUtil.doGet("https://graph.qq.com/oauth2.0/me?access_token="+accessMap.get("access_token"));

        Map<String,Object> userMap = JSON.parseObject(userJsonStr,Map.class);

        //查询数据库是否存在当前用户

        //不存在则保存

        //存在则直接登录

        //生成当前项目的登录token

        //将token传递到首页


        return model;
    }

    /**
     * 验证token是否合法
     *
     * @param token
     * @param currentIp 当前操作用户的IP地址
     * @return
     */
    @RequestMapping("varify")
    @ResponseBody
    public String varify(String token, String currentIp) {

        String ipmd = md5Util.MD5PasswordEncryptionComm(md5key, currentIp);
        String jwtKeymd = md5Util.MD5PasswordEncryptionComm(md5key, jwtKey);
        //校验token
        Map<String, Object> decode = JwtUtil.decode(token, jwtKeymd, ipmd);
        if (decode != null) {
            decode.put("status", "success");
        } else {
            decode.put("statuc","fail");
        }
        return JSON.toJSONString(decode);
    }



    /**
     * 验证账户信息，如果信息正确，颁发token
     *
     * @param umsMember
     * @param model
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, ModelAndView model, HttpServletRequest request) {

        String token = "";
        UmsMember umsMemberLogin = userService.getlogin(umsMember);

        if (umsMemberLogin != null) {

            //生成登录token
            token = getToken(umsMemberLogin.getId(),umsMemberLogin.getNickname(),request);
            userService.addUserToken(token, umsMemberLogin.getId());

        } else {
            //登录失败
            token = "fail";
        }
        return token;
    }


    /**
     * @param model
     * @return
     */
    @GetMapping("loginIndex.html")
    public ModelAndView loginIndex(String ReturnUrl, ModelAndView model) {
        model.addObject("ReturnUrl", ReturnUrl);
        model.setViewName("index");
        return model;
    }


    @RequestMapping("backLogin")
    public String backLogin(String code,String state,HttpServletRequest request){
        //第一步用户授权： https://api.weibo.com/oauth2/authorize?client_id=2889971318&redirect_uri=http://cart.xf.com:8183/backLogin&state=xf

        //第二步：通过回调地址调用该方法，获取到code

        //第三步，通过code获取到access_token验证
        //grant_type 授权类型 固定值为authorization_code
        //client_id 审核通过后分配的网站的appid
        //client_secret 审核通过后分配的网站的appkey
        //code 上一步获取的code
        //redirect_uri 返回的地址
        Map<String,String> codeMap=new HashMap<>();
        codeMap.put("client_id", weiboKey.client_id);
        codeMap.put("client_secret",weiboKey.client_secret);
        codeMap.put("grant_type",weiboKey.grant_type);
        codeMap.put("code",code);
        codeMap.put("redirect_uri",weiboKey.redirect_uri);
        String aUrl="https://api.weibo.com/oauth2/access_token?";
        String accessJsonStr = HttpclientUtil.doPost(aUrl,codeMap);
        Map<String,Object> accessMap = JSON.parseObject(accessJsonStr,Map.class);

        //第四步，通过accessMap获取用户的信息
        String s="https://api.weibo.com/2/users/show.json?access_token="+accessMap.get("access_token")+"&uid="+accessMap.get("uid");
        String wbUserStr = HttpclientUtil.doGet(s);
        Map<String,Object> wbUserMap = JSON.parseObject(wbUserStr,Map.class);

        UmsMember umsMember=new UmsMember();
        umsMember.setAccessCode(code);
        umsMember.setAccessToken((String)accessMap.get("access_token"));
        umsMember.setSourceType(1);//1表示来源微博用户
        umsMember.setCity((String)wbUserMap.get("location"));
        umsMember.setSourceUid((String)wbUserMap.get("idstr"));
        umsMember.setNickname((String)wbUserMap.get("screen_name"));
        umsMember.setUsername((String)wbUserMap.get("screen_name"));
        String token=null;
        //查询当前用户是否保存过
        UmsMember checkUms=new UmsMember();
        checkUms.setSourceUid(umsMember.getSourceUid());
        UmsMember ifuser = userService.checkOauthUser(checkUms);
        if(ifuser==null){
            //将用户的信息保存到数据库
            umsMember = userService.addOauthUser(umsMember);

        }else{
            umsMember=ifuser;
        }
        token= getToken(umsMember.getId(),umsMember.getNickname(),request);
        userService.addUserToken(token,umsMember.getId());

        return "redirect:http://cart.xf.com:8181/index.html?token="+token;
    }


    public String getToken(String memberId,String nickname,HttpServletRequest request){
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId);
        userMap.put("nickname", nickname);

        String token=null;
        //通过nginx转发的客户端ip  x-forwarded-for
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }

        String ipmd = md5Util.MD5PasswordEncryptionComm(md5key, ip);
        String jwtKeymd = md5Util.MD5PasswordEncryptionComm(md5key, jwtKey);
        //生成登录token
       return token = JwtUtil.encode(jwtKeymd, userMap, ipmd);

    }

}
