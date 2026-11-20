package com.amadeus.common.core.constants;

public class JwtConstants {
    /**  jwt中存储的用户信息  **/
    public final static String LOGIN_USER_ID = "userId"; // userId 主键id

    public final static String LOGIN_USER_KEY = "userKey";  //生成的uuid 作为用户的唯一标识

    public final static String LOGIN_USER_NICK_NAME = "nickName"; // 签发令牌时的用户昵称
}
