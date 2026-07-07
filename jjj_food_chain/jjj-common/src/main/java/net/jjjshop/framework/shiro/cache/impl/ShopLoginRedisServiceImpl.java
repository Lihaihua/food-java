

package net.jjjshop.framework.shiro.cache.impl;

import net.jjjshop.config.constant.CommonRedisKey;
import net.jjjshop.config.properties.JwtProperties;
import net.jjjshop.framework.common.bean.ClientInfo;
import net.jjjshop.framework.shiro.cache.ShopLoginRedisService;
import net.jjjshop.framework.shiro.convert.ShiroMapstructConvert;
import net.jjjshop.framework.shiro.jwt.JwtToken;
import net.jjjshop.framework.shiro.vo.JwtTokenRedisVo;
import net.jjjshop.framework.shiro.vo.LoginShopUserRedisVo;
import net.jjjshop.framework.shiro.vo.LoginShopUserVo;
import net.jjjshop.framework.util.ClientInfoUtil;
import net.jjjshop.framework.util.HttpServletRequestUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 登录信息Redis缓存服务类
 **/
@Service
public class ShopLoginRedisServiceImpl implements ShopLoginRedisService {

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
    public void cacheLoginInfo(JwtToken jwtToken, LoginShopUserVo loginShopUserVo) {
        if (jwtToken == null) {
            throw new IllegalArgumentException("jwtToken不能为空");
        }
        if (loginShopUserVo == null) {
            throw new IllegalArgumentException("loginShopUserVo不能为空");
        }
        // token
        String token = jwtToken.getToken();
        // 盐值
        String salt = jwtToken.getSalt();
        // 登录用户名称
        String username = loginShopUserVo.getUserName();
        // token md5值
        String tokenMd5 = DigestUtils.md5Hex(token);

        // Redis缓存JWT Token信息
        JwtTokenRedisVo jwtTokenRedisVo = ShiroMapstructConvert.INSTANCE.jwtTokenToJwtTokenRedisVo(jwtToken);

        // 用户客户端信息
        ClientInfo clientInfo = ClientInfoUtil.get(HttpServletRequestUtil.getRequest());

        // Redis缓存登录用户信息
        // 将loginShopUserVo对象复制到loginShopUserRedisVo
        LoginShopUserRedisVo loginShopUserRedisVo = new LoginShopUserRedisVo();
        BeanUtils.copyProperties(loginShopUserVo, loginShopUserRedisVo);
        loginShopUserRedisVo.setSalt(salt);
        loginShopUserRedisVo.setClientInfo(clientInfo);

        // Redis过期时间与JwtToken过期时间一致
        Duration expireDuration = Duration.ofSeconds(jwtToken.getExpireSecond());

        // 判断是否启用单个用户登录，如果是，这每个用户只有一个有效token
        boolean singleLogin = jwtProperties.isSingleLogin();
        if (singleLogin) {
            deleteUserAllCache(username);
        }

        // 1. tokenMd5:jwtTokenRedisVo
        String loginTokenRedisKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN, tokenMd5);
        redisClient.getBucket(loginTokenRedisKey).set(jwtTokenRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);

        // 2. username:loginShopUserRedisVo（包含 salt、lastActiveTime、online，无需单独存储）
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_USER, username)).set(loginShopUserRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);

        // 3. 将 tokenMd5 添加到用户的 token 集合中（用于单点登录）
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);
        tokenSet.add(tokenMd5);
        tokenSet.expire(expireDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void refreshLoginInfo(String oldToken, String username, JwtToken newJwtToken) {
        // 获取缓存的登录用户信息
        LoginShopUserRedisVo loginShopUserRedisVo = getLoginShopUserRedisVo(username);
        // 删除之前的token信息
        deleteLoginInfo(oldToken, username);
        // 缓存登录信息
        cacheLoginInfo(newJwtToken, loginShopUserRedisVo);
    }

    @Override
    public LoginShopUserRedisVo getLoginShopUserRedisVo(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        return (LoginShopUserRedisVo) redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_USER, username)).get();
    }

    @Override
    public String getSalt(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        LoginShopUserRedisVo loginShopUserRedisVo = getLoginShopUserRedisVo(username);
        return loginShopUserRedisVo != null ? loginShopUserRedisVo.getSalt() : null;
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
        // 1. 删除 token 信息
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN, tokenMd5)).delete();
        // 2. 删除用户信息
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_USER, username)).delete();
        // 3. 从 token 集合中移除，如果集合为空则删除集合
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);
        tokenSet.remove(tokenMd5);
        if (tokenSet.isEmpty()) {
            tokenSet.delete();
        }
    }

    @Override
    public boolean exists(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token不能为空");
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        return redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN, tokenMd5)).isExists();

    }

    @Override
    public void deleteUserAllCache(String username) {
        // 使用 RSet 获取用户的所有 tokenMd5
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);

        if (tokenSet.isEmpty()) {
            return;
        }

        // 遍历所有 tokenMd5，标记被踢下线并删除 token
        for (String tokenMd5 : tokenSet) {
            String tokenKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN, tokenMd5);

            // 获取 token 信息，用于标记被踢下线
            JwtTokenRedisVo jwtTokenRedisVo = (JwtTokenRedisVo) redisClient.getBucket(tokenKey).get();
            if (jwtTokenRedisVo != null && StringUtils.isNotBlank(jwtTokenRedisVo.getToken())) {
                markKickedOut(jwtTokenRedisVo.getToken());
            }

            // 删除 token
            redisClient.getBucket(tokenKey).delete();
        }

        // 删除用户信息和 token 集合
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_USER, username)).delete();
        tokenSet.delete();
    }
    @Override
    public void refreshLastActive(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String tokenKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN, tokenMd5);

        // 获取 token 信息
        JwtTokenRedisVo jwtTokenRedisVo = (JwtTokenRedisVo) redisClient.getBucket(tokenKey).get();
        if (jwtTokenRedisVo == null) {
            return;
        }

        // 使用shop端专用过期时间
        Long expireSecond = jwtProperties.getBackExpireSecond();

        String username = jwtTokenRedisVo.getUsername();

        // 更新用户活跃时间和在线状态
        LoginShopUserRedisVo loginShopUserRedisVo = getLoginShopUserRedisVo(username);
        if (loginShopUserRedisVo != null) {
            loginShopUserRedisVo.setLastActiveTime(new Date());
            loginShopUserRedisVo.setOnline(true);
        }

        Duration expireDuration = Duration.ofSeconds(expireSecond);

        // 刷新3个Key的过期时间
        redisClient.getBucket(tokenKey).set(jwtTokenRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);
        if (loginShopUserRedisVo != null) {
            redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_USER, username)).set(loginShopUserRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);
        }
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);
        tokenSet.expire(expireDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void markKickedOut(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String kickedKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_KICKED, tokenMd5);

        // 使用shop端过期时间，确保在token自然过期前都能检测到被踢状态
        Long expireSecond = jwtProperties.getBackExpireSecond();

        // 标记被踢下线，保留时间与token过期时间一致
        redisClient.getBucket(kickedKey).set("kicked", Duration.ofSeconds(expireSecond).toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isKickedOut(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String kickedKey = CommonRedisKey.getRedisKey(CommonRedisKey.SHOP_LOGIN_KICKED, tokenMd5);
        return redisClient.getBucket(kickedKey).isExists();
    }

}
