

package net.jjjshop.shop.controller.setting;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.config.constant.AgentRedisKey;
import net.jjjshop.config.constant.CommonRedisKey;
import net.jjjshop.framework.common.api.ApiResult;
import net.jjjshop.framework.core.util.RequestDetailThreadLocal;
import net.jjjshop.framework.log.annotation.OperationLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(value = "clear", tags = {"clear"})
@RestController
@RequestMapping("/shop/setting/clear")
public class ClearController {

    @Autowired
    private RedissonClient redisClient;

    @RequestMapping(value = "/index", method = RequestMethod.POST)
    @RequiresPermissions("/setting/clear/index")
    @OperationLog(name = "index")
    @ApiOperation(value = "index", response = String.class)
    public ApiResult<String> index() {
        String allKey = String.format(CommonRedisKey.SETTING_DATA_ALL, RequestDetailThreadLocal.getRequestDetail().getAppId());
        RKeys keys = redisClient.getKeys();
        Iterable<String> keysIterable = keys.getKeysByPattern(allKey);
        keys.delete(keysIterable.iterator().next());
        // 分销设置
        String allAgentKey = String.format(AgentRedisKey.SETTING_DATA_ALL, RequestDetailThreadLocal.getRequestDetail().getAppId());
        Iterable<String> agentKeysIterable = keys.getKeysByPattern(allAgentKey);
        keys.delete(agentKeysIterable.iterator().next());
        return ApiResult.ok(null, "清理成功");
    }
}
