

package net.jjjshop.config.constant;

import net.jjjshop.config.properties.SpringBootJjjProperties;

/**
 *  redis key 常量
 **/
public interface CommonRedisKey {

    // --------------------admin端登录信息开始----------------------
    /**
     * 登录用户token信息key
     * login:token:tokenMd5
     */
    String ADMIN_LOGIN_TOKEN = "food.admin:login:token:%s";

    /**
     * 登录用户信息key
     * login:user:username
     */
    String ADMIN_LOGIN_USER = "food.admin:login:user:%s";

    /**
     * admin端用户token集合（用于单点登录）
     * admin:login:token:set:username
     */
    String ADMIN_LOGIN_TOKEN_SET = "food.admin:login:token:set:%s";

    /**
     * admin端登录被踢下线标记
     * admin:login:kicked:tokenMd5
     */
    String ADMIN_LOGIN_KICKED = "food.admin:login:kicked:%s";

    // --------------------admin端登录信息结束----------------------

    // --------------------shop端登录信息开始----------------------
    /**
     * 登录用户token信息key
     * login:token:tokenMd5
     */
    String SHOP_LOGIN_TOKEN = "food.shop:login:token:%s";

    /**
     * 登录用户信息key
     * login:user:username
     */
    String SHOP_LOGIN_USER = "food.shop:login:user:%s";

    /**
     * shop登录被踢下线标记
     * shop:login:kicked:token
     * 用于区分是被踢下线还是正常过期
     */
    String SHOP_LOGIN_KICKED = "food.shop:login:kicked:%s";

    /**
     * shop端用户token集合（用于单点登录）
     * shop:login:token:set:username
     */
    String SHOP_LOGIN_TOKEN_SET = "food.shop:login:token:set:%s";

    // --------------------shop端登录信息结束----------------------

    // --------------------cashier端登录信息开始----------------------
    /**
     * 登录用户token信息key
     * login:token:tokenMd5
     */
    String CASHIER_LOGIN_TOKEN = "food.cashier:login:token:%s";

    /**
     * 登录用户信息key
     * login:user:username
     */
    String CASHIER_LOGIN_USER = "food.cashier:login:user:%s";

    /**
     * cashier登录被踢下线标记
     * cashier:login:kicked:token
     * 用于区分是被踢下线还是正常过期
     */
    String CASHIER_LOGIN_KICKED = "food.cashier:login:kicked:%s";

    /**
     * shop端用户token集合（用于单点登录）
     * cashier:login:token:set:username
     */
    String CASHIER_LOGIN_TOKEN_SET = "food.cashier:login:token:set:%s";

    // --------------------cashier端登录信息结束----------------------

    // --------------------front端登录信息开始----------------------
    /**
     * 登录用户token信息key
     * login:token:tokenMd5
     */
    String USER_LOGIN_TOKEN = "food.user:login:token:%s";

    /**
     * 登录用户信息key
     * login:user:username
     */
    String USER_LOGIN_USER = "food.user:login:user:%s";

    // --------------------front端登录信息结束----------------------

    /**
     * 地区缓存
     */
    String REGION_DATA = "food:region.data";
    /**
     * 设置缓存
     */
    String SETTING_DATA = "food_setting.data:%s:%s";
    /**
     * 所有设置缓存
     */
    String SETTING_DATA_ALL = "food_setting.data:%s:*";
    /**
     * 商品分类缓存
     */
    String PRODUCT_CATEGORY_DATA = "food:product.category.data:%s";
    /**
     * uat测试安全ip
     */
    String UAT_IP = "food:uat.ip";
    /**
     * 任务设置缓存
     */
    String TASK_DATA= "food:task.data.%s:%s:%s:%s";


    public static String getRedisKey(String key, Object...params){
        return SpringBootJjjProperties.getCachePrefix() + "." + String.format(key, params);
    }
}
