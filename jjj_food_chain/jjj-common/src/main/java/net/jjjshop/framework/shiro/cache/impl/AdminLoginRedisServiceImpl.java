

package net.jjjshop.framework.shiro.cache.impl;

import net.jjjshop.config.constant.CommonRedisKey;
import net.jjjshop.config.properties.JwtProperties;
import net.jjjshop.framework.common.bean.ClientInfo;
import net.jjjshop.framework.shiro.cache.AdminLoginRedisService;
import net.jjjshop.framework.shiro.convert.ShiroMapstructConvert;
import net.jjjshop.framework.shiro.jwt.JwtToken;
import net.jjjshop.framework.shiro.vo.JwtTokenRedisVo;
import net.jjjshop.framework.shiro.vo.LoginAdminUserRedisVo;
import net.jjjshop.framework.shiro.vo.LoginAdminUserVo;
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
import java.util.concurrent.TimeUnit;

/**
 * 登录信息Redis缓存服务类
 **/
@Service
public class AdminLoginRedisServiceImpl implements AdminLoginRedisService {

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
    public void cacheLoginInfo(JwtToken jwtToken, LoginAdminUserVo loginAdminUserVo) {
        if (jwtToken == null) {
            throw new IllegalArgumentException("jwtToken不能为空");
        }
        if (loginAdminUserVo == null) {
            throw new IllegalArgumentException("loginAdminUserVo不能为空");
        }
        // token
        String token = jwtToken.getToken();
        // 盐值
        String salt = jwtToken.getSalt();
        // 登录用户名称
        String username = loginAdminUserVo.getUserName();
        // token md5值
        String tokenMd5 = DigestUtils.md5Hex(token);

        // Redis缓存JWT Token信息
        JwtTokenRedisVo jwtTokenRedisVo = ShiroMapstructConvert.INSTANCE.jwtTokenToJwtTokenRedisVo(jwtToken);

        // 用户客户端信息
        ClientInfo clientInfo = ClientInfoUtil.get(HttpServletRequestUtil.getRequest());

        // Redis缓存登录用户信息
        // 将loginAdminUserVo对象复制到loginAdminUserRedisVo
        LoginAdminUserRedisVo loginAdminUserRedisVo = new LoginAdminUserRedisVo();
        BeanUtils.copyProperties(loginAdminUserVo, loginAdminUserRedisVo);
        loginAdminUserRedisVo.setSalt(salt);
        loginAdminUserRedisVo.setClientInfo(clientInfo);

        // Redis过期时间与JwtToken过期时间一致
        Duration expireDuration = Duration.ofSeconds(jwtToken.getExpireSecond());

        // 判断是否启用单个用户登录，如果是，这每个用户只有一个有效token
        boolean singleLogin = jwtProperties.isSingleLogin();
        if (singleLogin) {
            deleteUserAllCache(username);
        }

        // 1. tokenMd5:jwtTokenRedisVo
        String loginTokenRedisKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN, tokenMd5);
        redisClient.getBucket(loginTokenRedisKey).set(jwtTokenRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);

        // 2. username:loginAdminUserRedisVo（包含 salt、lastActiveTime、online，无需单独存储）
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_USER, username)).set(loginAdminUserRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);

        // 3. 将 tokenMd5 添加到用户的 token 集合中（用于单点登录）
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);
        tokenSet.add(tokenMd5);
        tokenSet.expire(expireDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public LoginAdminUserRedisVo getLoginAdminUserRedisVo(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        return (LoginAdminUserRedisVo) redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_USER, username)).get();
    }

    @Override
    public String getSalt(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username不能为空");
        }
        LoginAdminUserRedisVo loginAdminUserRedisVo = getLoginAdminUserRedisVo(username);
        return loginAdminUserRedisVo != null ? loginAdminUserRedisVo.getSalt() : null;
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
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN, tokenMd5)).delete();
        // 2. 删除用户信息
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_USER, username)).delete();
        // 3. 从 token 集合中移除，如果集合为空则删除集合
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN_SET, username);
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
        return redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN, tokenMd5)).isExists();
    }

    @Override
    public void deleteUserAllCache(String username) {
        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);

        if (tokenSet.isEmpty()) {
            return;
        }

        for (String tokenMd5 : tokenSet) {
            String tokenKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN, tokenMd5);

            JwtTokenRedisVo jwtTokenRedisVo = (JwtTokenRedisVo) redisClient.getBucket(tokenKey).get();
            if (jwtTokenRedisVo != null && StringUtils.isNotBlank(jwtTokenRedisVo.getToken())) {
                markKickedOut(jwtTokenRedisVo.getToken());
            }

            redisClient.getBucket(tokenKey).delete();
        }

        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_USER, username)).delete();
        tokenSet.delete();
    }

    @Override
    public void refreshLastActive(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String tokenKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN, tokenMd5);

        JwtTokenRedisVo jwtTokenRedisVo = (JwtTokenRedisVo) redisClient.getBucket(tokenKey).get();
        if (jwtTokenRedisVo == null) {
            return;
        }

        Long expireSecond = jwtProperties.getBackExpireSecond();
        String username = jwtTokenRedisVo.getUsername();

        Duration expireDuration = Duration.ofSeconds(expireSecond);
        redisClient.getBucket(tokenKey).set(jwtTokenRedisVo, expireDuration.toMillis(), TimeUnit.MILLISECONDS);
        redisClient.getBucket(CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_USER, username)).expire(expireSecond, TimeUnit.SECONDS);

        String tokenSetKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_TOKEN_SET, username);
        RSet<String> tokenSet = redisClient.getSet(tokenSetKey);
        tokenSet.expire(expireDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void markKickedOut(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String kickedKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_KICKED, tokenMd5);
        Long expireSecond = jwtProperties.getBackExpireSecond();
        redisClient.getBucket(kickedKey).set("kicked", Duration.ofSeconds(expireSecond).toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isKickedOut(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        String tokenMd5 = DigestUtils.md5Hex(token);
        String kickedKey = CommonRedisKey.getRedisKey(CommonRedisKey.ADMIN_LOGIN_KICKED, tokenMd5);
        return redisClient.getBucket(kickedKey).isExists();
    }


}
