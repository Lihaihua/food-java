

package net.jjjshop.framework.shiro.cache.impl;

import net.jjjshop.config.constant.CommonRedisKey;
import net.jjjshop.config.properties.JwtProperties;
import net.jjjshop.framework.common.bean.ClientInfo;
import net.jjjshop.framework.shiro.cache.UserLoginRedisService;
import net.jjjshop.framework.shiro.convert.ShiroMapstructConvert;
import net.jjjshop.framework.shiro.jwt.JwtToken;
import net.jjjshop.framework.shiro.vo.JwtTokenRedisVo;
import net.jjjshop.framework.shiro.vo.LoginUserRedisVo;
import net.jjjshop.framework.shiro.vo.LoginUserVo;
import net.jjjshop.framework.util.ClientInfoUtil;
import net.jjjshop.framework.util.HttpServletRequestUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录信息Redis缓存服务类
 **/
@Service
public class UserLoginRedisServiceImpl implements UserLoginRedisService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedissonClient redisClient;

    /**
     * key-value: 有过期时间-->token过期时间
     * 1. tokenMd5:jwtTokenRedisVo
     * 2. username:loginSysUserRedisVo
     * 3. username:salt
     * hash: 没有过期时间，统计在线的用户信息
     * username:num
     */
    @Override
    public void cacheLoginInfo(JwtToken jwtToken, LoginUserVo loginUserVo) {
        if (jwtToken == null) {
            throw new IllegalArgumentException("jwtToken不能为空");
        }
        if (loginUserVo == null) {
            throw new IllegalArgumentException("loginUserVo不能为空");
        }
        // token
        String token = jwtToken.getToken();
        // 盐值
        String salt = jwtToken.getSalt();
        // 登录用户id
        String username = "" + loginUserVo.getUserId();
        // token md5值
        String tokenMd5 = DigestUtils.md5Hex(token);

        // Redis缓存JWT Token信息
        JwtTokenRedisVo jwtTokenRedisVo = ShiroMapstructConvert.INSTANCE.jwtTokenToJwtTokenRedisVo(jwtToken);

        // 用户客户端信息
        ClientInfo clientInfo = ClientInfoUtil.get(HttpServletRequestUtil.getRequest());

        // Redis缓存登录用户信息
        // 将loginUserVo对象复制到loginUserRedisVo
        LoginUserRedisVo loginUserRedisVo = new LoginUserRedisVo();
        BeanUtils.copyProperties(loginUserVo, loginUserRedisVo);
        loginUserRedisVo.setSalt(salt);
        loginUserRedisVo.setClientInfo(clientInfo);

        // Redis过期时间与JwtToken过期时间一致
        Duration expireDuration = Duration.ofSeconds(jwtToken.getExpireSecond());
        // 1. tokenMd5:jwtTokenRedisVo
        String loginTokenRedisKey = CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_TOKEN, tokenMd5);
        redisClient.getBucket(loginTokenRedisKey).set(jwtTokenRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);
        // 2. username:loginUserRedisVo（包含salt，无需单独存储）
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_USER, username)).set(loginUserRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public LoginUserRedisVo getLoginUserRedisVo(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        return (LoginUserRedisVo) redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_USER, username)).get();
    }

    @Override
    public String getSalt(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        // 从LoginUserRedisVo中获取salt，无需单独存储
        LoginUserRedisVo loginUserRedisVo = getLoginUserRedisVo(username);
        return loginUserRedisVo != null ? loginUserRedisVo.getSalt() : null;
    }

    @Override
    public void deleteLoginInfo(String token, String username) {
        if (token == null) {
            throw new IllegalArgumentException("token不能为空");
        }
        if (username == null) {
            throw new IllegalArgumentException("username不能为空");
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        // 1. delete tokenMd5
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_TOKEN, tokenMd5)).delete();
        // 2. delete username
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_USER, username)).delete();
    }

    @Override
    public boolean exists(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token不能为空");
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        return redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_TOKEN, tokenMd5)).isExists();
    }

    @Override
    public void refreshLastActive(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String tokenKey = CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_TOKEN, tokenMd5);

        // 获取 token 信息
        JwtTokenRedisVo jwtTokenRedisVo = (JwtTokenRedisVo) redisClient.getBucket(tokenKey).get();
        if (jwtTokenRedisVo == null) {
            return;
        }

        // 使用user端专用过期时间
        Long expireSecond = jwtProperties.getExpireSecond();

        // 续期策略：只有距离上次续期超过1天（86400秒）时才续期
        Long now = System.currentTimeMillis() / 1000;
        Long lastRenewTime = jwtTokenRedisVo.getLastRenewTime();

        // 兼容旧数据：如果 lastRenewTime 为 null，或者距离上次续期超过1天，则续期
        if (lastRenewTime == null || now - lastRenewTime >= 86400) {
            String username = jwtTokenRedisVo.getUsername();

            // 更新续期时间
            jwtTokenRedisVo.setLastRenewTime(now);

            // 刷新2个Key的过期时间
            redisClient.getBucket(tokenKey).set(jwtTokenRedisVo, expireSecond, TimeUnit.SECONDS);
            redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.USER_LOGIN_USER, username)).expire(expireSecond, TimeUnit.SECONDS);
        }
    }


}
