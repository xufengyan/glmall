package com.xf.glmall.util;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.stereotype.Component;

@Component
public class MD5util {

    public static void main(String[] args){
        String password="123456";
//        //MD5加密
//        String pwd1=new Md5Hash(password).toString();
//        System.out.println("Md5第一次加密："+pwd1);
//        String pwd2=new Md5Hash(pwd1).toString();
//        System.out.println("Md5第二次加密："+pwd2);
//        String pwd3=new Md5Hash(pwd2).toString();
//        System.out.println("Md5第三次加密："+pwd3);

//        使用Md5加盐
        String slot="umsMemberInfoxufeng";
//        散列一次，加盐
        String pwd4=new Md5Hash(password,slot,2).toString();
        System.out.println("使用Md5散列一次加密加并加盐："+pwd4);

//        散列两次，加盐
//        String pwd5=new Md5Hash(password,slot,2).toString();
//        System.out.println("使用Md5散列两次加密加并加盐："+pwd5);
    }


    /**
     * 对密码进行加密加盐
     * @param username
     * @param logKey
     * @param password
     * @return
     */
    public String MD5PasswordEncryption(String username,String logKey,String password){
        String slot=logKey+username;
        String pwdMD5=new Md5Hash(password,slot,2).toString();
    return pwdMD5;
    }

    public String MD5PasswordEncryptionComm(String slot,String value){
        String pwdMD5=new Md5Hash(value,slot,2).toString();
        return pwdMD5;
    }

}
