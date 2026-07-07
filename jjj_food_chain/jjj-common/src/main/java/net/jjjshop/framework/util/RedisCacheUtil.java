

package net.jjjshop.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class RedisCacheUtil {

    private static RedisCacheUtil redisCacheUtil;

    @Autowired
    private RedissonClient redisClient;

    /**
     * 将当前对象赋值给静态对象,调用spring组件: redisCacheUtil.redisClient.xxx()
     */
    @PostConstruct
    public void init(){
        redisCacheUtil = this;
    }
}
