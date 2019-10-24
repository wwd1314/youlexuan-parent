package com.youlexuan.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.youlexuan.CONSTANT;
import com.youlexuan.cart.service.CartService;
import com.youlexuan.mapper.TbItemMapper;
import com.youlexuan.pojo.TbItem;
import com.youlexuan.pojo.TbOrderItem;
import com.youlexuan.pojogroup.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private TbItemMapper itemMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 没有购物车列表
     *          商品的对应的商家的购物车也没有，购物车里的购物项也咩有
     *                  新建购物车pojo
     *                  新建购物项pojo，将购物项set购物车中
     *                  将购物车放到购物车List中
     * @param cartlist
     * @param itemId
     * @param num
     * @return
     */
    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartlist, Long itemId, Integer num) {


        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        String sellerId = item .getSellerId();
        Cart cart  = searchCarBySellerId(cartlist,sellerId);

        if (cart==null){
            //没有商家的购物车
            cart = new Cart();
            cart.setSellerId(sellerId);
            cart.setSellerName(item.getSeller());
            List<TbOrderItem> orderItemList = new ArrayList<>();
            TbOrderItem orderItem = creatOrderItem(item,num);
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList);

            //将cart放入到cartllist
            cartlist.add(cart);
        }else {
            //购物车列表中有升价对应得到购物车
            TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);
            if (orderItem == null) {
                orderItem =  creatOrderItem(item,num);
                cart.getOrderItemList().add(orderItem);
            } else
            {
                //购物车中有商品对应的购物项
                orderItem.setNum(orderItem.getNum()+num);
                orderItem.setTotalFee(new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum()));

                if (orderItem.getNum()==0){
                    cart.getOrderItemList().remove(orderItem);
                }
                if (cart.getOrderItemList().size()<=0){
                    cartlist.remove(cart);
                }


            }




        }
        return cartlist;

    }


    /**
     * 根skuId查找对应的orderItem
     * @param orderItemList
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
        for (TbOrderItem orderItem:orderItemList){
            if (orderItem.getItemId().equals(itemId)){
                return orderItem;
            }
        }

        return null;
    }

    private TbOrderItem creatOrderItem(TbItem item, Integer num) {
        if(num<=0){
            throw new RuntimeException("数量非法");
        }

        TbOrderItem orderItem=new TbOrderItem();
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setItemId(item.getId());
        orderItem.setNum(num);
        orderItem.setPicPath(item.getImage());
        orderItem.setPrice(item.getPrice());
        orderItem.setSellerId(item.getSellerId());
        orderItem.setTitle(item.getTitle());
        orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue()*num));
        return orderItem;
    }

    /**
     * 根据sellerid查找对应的cart
     * @param cartlist
     * @param sellerId
     * @return
     */
    private Cart searchCarBySellerId(List<Cart> cartlist, String sellerId) {
        for (Cart cart:cartlist){
            if (cart.getSellerId().equals(sellerId)){
                return cart;
            }

        }
        return null;
    }


    @Override
    public List<Cart> findCartListFromRedis(String name) {
        System.out.println("从redis中获取购物车List。。。。。。");
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps(CONSTANT.CART_LIST_REDIS_KEY).get(name);
        if (cartList==null){
            cartList =  new ArrayList<>();
        }
        return cartList;
    }

    @Override
    public void saveCartListToRedis(String name, List<Cart> cartList1) {
        System.out.println("将购物车列表放入到redis中。。。。。。");
       redisTemplate.boundHashOps(CONSTANT.CART_LIST_REDIS_KEY).put(name,cartList1);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart>  cartList2){
        for (Cart cart:cartList2){
            for (TbOrderItem orderItem:cart.getOrderItemList()){
                cartList1 = addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList1;
    }
}
