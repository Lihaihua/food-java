

package net.jjjshop.framework.shiro.util;

import lombok.extern.slf4j.Slf4j;
import net.jjjshop.config.properties.JwtProperties;
import net.jjjshop.framework.util.HttpServletRequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * JwtToken工具类
 **/
@Slf4j
@Component
public class JwtTokenUtil {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LEGACY_TOKEN_NAME = "token";

    private static String tokenName;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        tokenName = jwtProperties.getTokenName();
        log.debug("tokenName:{}", tokenName);
    }

    /**
     * 获取token名称
     *
     * @return
     */
    public static String getTokenName(String model) {
        return AUTHORIZATION_HEADER;
    }

    /**
     * 从请求头或者请求参数中
     *
     * @return
     */
    public static String getToken(String model) {
        return getToken(HttpServletRequestUtil.getRequest(), model);
    }

    public static String getModel(String path){
        String model = "";
        if(path.startsWith("/api/admin/")){
            model = "admin";
        }else if(path.startsWith("/api/shop/")){
            model = "shop";
        }
        return model;
    }

    /**
     * 从请求头或者请求参数中
     *
     * @param request
     * @return
     */
    public static String getToken(HttpServletRequest request, String model) {
        if (request == null) {
            throw new IllegalArgumentException("request不能为空");
        }
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(authorization)) {
            return extractBearerToken(authorization);
        }
        return request.getHeader(getLegacyTokenName(model));
    }

    public static String buildAuthorizationValue(String token) {
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("token不能为空");
        }
        return BEARER_PREFIX + token;
    }

    private static String extractBearerToken(String authorization) {
        String value = StringUtils.trimToEmpty(authorization);
        if (StringUtils.startsWithIgnoreCase(value, BEARER_PREFIX)) {
            return StringUtils.trimToEmpty(value.substring(BEARER_PREFIX.length()));
        }
        return value;
    }

    private static String getLegacyTokenName(String model) {
        String legacyBase = tokenName;
        if (StringUtils.isBlank(legacyBase) || AUTHORIZATION_HEADER.equalsIgnoreCase(legacyBase)) {
            legacyBase = LEGACY_TOKEN_NAME;
        }
        if (StringUtils.isBlank(model)) {
            return legacyBase;
        }
        return legacyBase + model;
    }
}
