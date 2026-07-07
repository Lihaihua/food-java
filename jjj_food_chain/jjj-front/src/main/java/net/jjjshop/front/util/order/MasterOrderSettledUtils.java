package net.jjjshop.front.util.order;

import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.user.User;
import net.jjjshop.common.enums.DeliveryTypeEnum;
import net.jjjshop.common.enums.OrderSourceEnum;
import net.jjjshop.common.util.OrderUtils;
import net.jjjshop.framework.common.exception.BusinessException;
import net.jjjshop.front.param.order.OrderBuyParam;
import net.jjjshop.front.service.order.OrderService;
import net.jjjshop.front.service.supplier.SupplierService;
import net.jjjshop.front.vo.product.ProductBuyVo;
import net.jjjshop.front.vo.supplier.SupplierVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 普通订单结算
 */
@Slf4j
@Component
@Scope("prototype")
public class MasterOrderSettledUtils extends OrderSettledUtils {

    @Autowired
    private OrderService orderService;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private OrderUtils orderUtils;

    @Override
    public Map<String, Object> settlement(User user, List<ProductBuyVo> productList, OrderBuyParam orderBuyParam){
        super.init();
        // 差异化规则
        this.orderSource.setSource(OrderSourceEnum.MASTER.getValue());
        return super.settlement(user, productList, orderBuyParam);
    }

    /**
     * 验证订单商品的状态
     */
    @Override
    public void validateProductList(){
        SupplierVo supplier = supplierService.getDetailById(this.orderBuyParam.getShopSupplierId());
        //店铺状态0营业中1停止营业
        if(supplier.getIsDelete() == 1 || supplier.getStatus() == 1){
            throw new BusinessException("店铺已歇业");
        }
        if(supplier.getIsRecycle() == 1){
            throw new BusinessException("店铺已禁用");
        }

        if(Objects.equals(this.orderBuyParam.getDelivery(), DeliveryTypeEnum.WAIMAI.getValue())) {
            // 外卖配送
            if (this.orderData.getAddress() != null) {
                //距离门店距离
                float distance = orderUtils.getDistance(supplier, this.orderData.getAddress().getLongitude(), this.orderData.getAddress().getLatitude()).floatValue();
                //配送范围km
                if (supplier.getDeliveryDistance() > 0 && distance > (supplier.getDeliveryDistance() * 1000)) {
                    throw new BusinessException("超出配送范围" + supplier.getDeliveryDistance() + "km");
                }
            }
        }
        Map<Integer, List<ProductBuyVo>> map = this.productList.stream().collect(Collectors.groupingBy(ProductBuyVo::getProductSkuId));
        map.forEach((key, value) -> {
            //该规格总购买数量
            Integer productNum = value.stream().map(ProductBuyVo::getProductNum).reduce(0, Integer::sum);
            // 判断商品库存
            if(productNum > value.get(0).getSku().getStockNum()){
                throw new BusinessException(String.format("很抱歉，商品 [%s] 库存不足", value.get(0).getProductName()));
            }
        });

        for(ProductBuyVo product:this.productList){
            // 判断商品是否下架
            if(product.getProductStatus() != 10){
                throw new BusinessException(String.format("很抱歉，商品 [%s] 已下架", product.getProductName()));
            }
            // 是否是会员商品
            if(StringUtils.isNotEmpty(product.getGradeIds())){
                String[] gradeIdArr = product.getGradeIds().split(",");
                if(!Arrays.asList(gradeIdArr).contains("" + this.user.getGradeId())){
                    throw new BusinessException("很抱歉，此商品仅特定会员可购买");
                }
            }
            // 判断是否超过限购数量
            if(product.getLimitNum() > 0){
                Integer hasNum = orderService.getHasBuyOrderNum(this.user.getUserId(), product.getProductId());
                if(hasNum + product.getProductNum() > product.getLimitNum()){
                    throw new BusinessException("很抱歉，购买超过此商品最大限购数量");
                }
            }
        }
    }
}
