package net.jjjshop.job.lock;


import net.jjjshop.config.properties.SpringBootJjjProperties;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 */
@Component
public class CommonRedisHelper {

    @Autowired
    RedissonClient redisClient;

    /**
     * 加分布式锁
     *
     * @param track
     * @param sector
     * @param timeout
     * @return
     */
    public boolean setNx(String track, String sector, long timeout) {
        String cacheKey = this.getCacheKey(track, sector);
        RBucket<Long> bucket = redisClient.getBucket(cacheKey);

        Boolean flag = bucket.trySet(System.currentTimeMillis());
        // 如果成功设置超时时间, 防止超时
        if (flag) {
            bucket.set(getLockValue(track, sector), timeout, TimeUnit.SECONDS);
        }
        return flag;
    }

    /**
     * 删除锁
     *
     * @param track
     * @param sector
     */
    public void delete(String track, String sector) {
        redisClient.getBucket(this.getCacheKey(track, sector)).delete();
    }

    /**
     * 查询锁
     * @return 写锁时间
     */
    public long getLockValue(String track, String sector) {
        RBucket<Long> bucket = redisClient.getBucket(this.getCacheKey(track, sector));
        Long createTime = bucket.get();
        return createTime != null ? createTime : 0L;
    }


    private String getCacheKey(String track, String sector){
        return SpringBootJjjProperties.getCachePrefix() + "." + track + sector;
    }

}