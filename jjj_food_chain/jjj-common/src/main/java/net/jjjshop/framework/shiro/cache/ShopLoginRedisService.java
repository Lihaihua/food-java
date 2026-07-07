

package net.jjjshop.framework.shiro.cache;


import net.jjjshop.framework.shiro.jwt.JwtToken;
import net.jjjshop.framework.shiro.vo.LoginShopUserRedisVo;
import net.jjjshop.framework.shiro.vo.LoginShopUserVo;

/**
 * 登录信息Redis缓存操作服务
 **/
public interface ShopLoginRedisService {

    /**
     * 缓存登录信息
     *
     * @param jwtToken
     * @param loginShopUserVo
     */
    void cacheLoginInfo(JwtToken jwtToken, LoginShopUserVo loginShopUserVo);


    /**
     * 刷新登录信息
     *
     * @param oldToken
     * @param username
     * @param newJwtToken
     */
    void refreshLoginInfo(String oldToken, String username, JwtToken newJwtToken);

    /**
     * 通过用户名，从缓存中获取登录用户LoginSysUserRedisVo
     *
     * @param username
     * @return
     */
    LoginShopUserRedisVo getLoginShopUserRedisVo(String username);

    /**
     * 通过用户名称获取盐值
     *
     * @param username
     * @return
     */
    String getSalt(String username);

    /**
     * 删除对应用户的Redis缓存
     *
     * @param token
     * @param username
     */
    void deleteLoginInfo(String token, String username);

    /**
     * 判断token在redis中是否存在
     *
     * @param token
     * @return
     */
    boolean exists(String token);

    /**
     * 删除用户所有登录缓存
     *
     * @param username
     */
    void deleteUserAllCache(String username);

    /**
     * 刷新最后活动时间并续期
     *
     * @param token token
     */
    void refreshLastActive(String token);

    /**
     * 标记token被踢下线
     *
     * @param token 被踢下线的token
     */
    void markKickedOut(String token);

    /**
     * 检查token是否被踢下线
     *
     * @param token token
     * @return boolean
     */
    boolean isKickedOut(String token);

}
