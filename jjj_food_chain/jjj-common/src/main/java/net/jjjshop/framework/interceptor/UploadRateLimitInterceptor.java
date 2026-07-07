package net.jjjshop.framework.interceptor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.framework.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 上传频率限制拦截器
 * 基于IP和用户维度进行限流，防止恶意上传攻击
 */
@Slf4j
@Component
public class UploadRateLimitInterceptor implements HandlerInterceptor {

    /**
     * 每个IP每秒最多上传次数
     */
    private static final double PERMITS_PER_SECOND = 2.0;

    /**
     * 缓存过期时间（分钟）
     */
    private static final int CACHE_EXPIRE_MINUTES = 10;

    /**
     * IP维度限流器缓存
     */
    private final LoadingCache<String, RateLimiter> ipRateLimiterCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(PERMITS_PER_SECOND);
                }
            });

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);

        try {
            RateLimiter rateLimiter = ipRateLimiterCache.get(ip);

            // 尝试获取令牌，最多等待100ms
            if (!rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                log.warn("上传频率超限，IP: {}, URI: {}", ip, request.getRequestURI());
                throw new BusinessException("上传过于频繁，请稍后再试");
            }

            return true;
        } catch (ExecutionException e) {
            log.error("限流器获取失败", e);
            // 限流器异常时放行，避免影响正常业务
            return true;
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
