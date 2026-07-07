package net.jjjshop.common.settings.vo;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@ApiModel("交易设置VO")
public class TradeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "未支付订单自动关闭时间不能为空")
    @Min(value = 0, message = "自动关闭时间必须大于等于0")
    //未支付订单自动关闭时间
    private Integer closeDays;

    @NotNull(message = "自动关闭时间类型不能为空")
    //自动关闭时间类型,1=天,2=小时,3=分钟
    private Integer closeType;

    @NotNull(message = "自动收货天数不能为空")
    private Integer receiveDays;

    public TradeVo() {
        this.closeDays = 1;
        this.closeType = 1;
        this.receiveDays = 7;
    }

}
