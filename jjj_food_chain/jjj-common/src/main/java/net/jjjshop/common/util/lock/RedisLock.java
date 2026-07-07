package net.jjjshop.common.util.lock;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Slf4j
@Data
public class RedisLock {
    @Autowired
    RedissonClient redisClient;

    /**
     * 获取锁，true 则得到锁，false 已被锁定
     * @param lockName       锁名称
     * @param lockExoire     锁时间
     * @return
     */
    public Boolean getLock(String lockName, Integer lockExoire) {
        RLock lock = redisClient.getLock(lockName);
        try {
            // tryLock(waitTime, leaseTime, unit)
            // waitTime: 等待获取锁的时间（0表示不等待）
            // leaseTime: 锁自动释放时间
            return lock.tryLock(0, lockExoire, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁失败: {}", lockName, e);
            return false;
        }
    }

    /**
     * 删除锁
     * @param lockName
     */
    public void delLock(String lockName) {
        RLock lock = redisClient.getLock(lockName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 获取锁Key
     * @param prefix    前缀
     * @param name      名称
     * @return
     */
    public static String getFullKey(String prefix, String name) {
        return prefix + "_" + name;
    }
}



