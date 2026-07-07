

package net.jjjshop.framework.shiro.cache;


import net.jjjshop.framework.shiro.jwt.JwtToken;
import net.jjjshop.framework.shiro.vo.LoginUserRedisVo;
import net.jjjshop.framework.shiro.vo.LoginUserVo;

/**
 * 登录信息Redis缓存操作服务
 **/
public interface UserLoginRedisService {

    /**
     * 缓存登录信息
     *
     * @param jwtToken
     * @param loginUserVo
     */
    void cacheLoginInfo(JwtToken jwtToken, LoginUserVo loginUserVo);

    /**
     * 通过用户名，从缓存中获取登录用户LoginUserRedisVo
     *
     * @param username
     * @return
     */
    LoginUserRedisVo getLoginUserRedisVo(String username);

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
     * 刷新最后活动时间并续期
     *
     * @param token token
     */
    void refreshLastActive(String token);

}
