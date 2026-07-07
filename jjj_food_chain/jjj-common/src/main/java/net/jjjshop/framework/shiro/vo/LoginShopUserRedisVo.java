

package net.jjjshop.framework.shiro.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import net.jjjshop.framework.common.bean.ClientInfo;

import java.util.Date;

/**
 * 登录用户Redis对象，后台使用
 **/
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class LoginShopUserRedisVo extends LoginShopUserVo {

    private static final long serialVersionUID = -3858850188055605806L;

    /**
     * 包装后的盐值
     */
    private String salt;

    /**
     * 登录ip
     */
    private ClientInfo clientInfo;

    /**
     * 最后活动时间
     */
    private Date lastActiveTime;

    /**
     * 是否在线
     */
    private Boolean online;

}
