package net.jjjshop.common.util;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.settings.vo.TaskVo;
import net.jjjshop.config.constant.CommonRedisKey;
import net.jjjshop.framework.core.util.RequestDetailThreadLocal;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskUtils {

    @Autowired
    private RedissonClient redisClient;

    /**
     * 通过key查找设置信息
     * @param
     * @return
     * @throws
     */
    public JSONObject getTaskLog(String taskType,Integer userId, Long appId) {
        if(appId == null){
            appId = RequestDetailThreadLocal.getRequestDetail().getAppId();
        }
        String today = DateUtil.format(new Date(), "yyyy-MM-dd");
        String cacheKey = CommonRedisKey.getRedisKey(CommonRedisKey.TASK_DATA, taskType, today, userId.toString(), appId);
        // 如果缓存中存在,则返回
        RBucket<JSONObject> bucket = redisClient.getBucket(cacheKey);
        JSONObject object = bucket.get();
        if(object != null){
            return object;
        }else {
            return null;
        }
    }

    /**
     * 保存
     */
    public Boolean saveTask(TaskVo.Task task, Integer userId, Long appId){
        if(appId == null){
            appId = RequestDetailThreadLocal.getRequestDetail().getAppId();
        }
        String today = DateUtil.format(new Date(), "yyyy-MM-dd");
        String cacheKey = CommonRedisKey.getRedisKey(CommonRedisKey.TASK_DATA, task.getTaskType(), today, userId.toString(), appId);
        task.setStatus(1);
        JSONObject result = null;
        result = (JSONObject)JSONObject.toJSON(task);
        RBucket<JSONObject> bucket = redisClient.getBucket(cacheKey);
        bucket.set(result, 86400L, TimeUnit.SECONDS);
        return true;
    }
}
