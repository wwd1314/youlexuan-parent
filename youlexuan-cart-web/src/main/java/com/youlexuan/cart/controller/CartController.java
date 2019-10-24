package com.youlexuan.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.youlexuan.CONSTANT;
import com.youlexuan.cart.service.CartService;
import com.youlexuan.entity.Result;
import com.youlexuan.pojogroup.Cart;
import com.youlexuan.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private HttpServletRequest request;
    @Reference(timeout = 50000000)
    private CartService cartService;
    @Autowired
    private HttpServletResponse response;

    /**
     * 1，未登录：从cookie中取老购物车列表，计算出心购物车列表以后，再讲心购物车列表写到cookie
     * 2. 一登录 从redis中取购物车列表，计算出新的购物车列表后，再将新列表写到redis中
     * @param itemId
     * @param num
     * @return
     */
    @CrossOrigin(origins = "http://localhost:9105",allowCredentials = "true")
    @RequestMapping("/addCart")
    public Result addCart(Long itemId, Integer num){

        try{
           /* response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");
            response.setHeader("Access-Control-Allow-Credentials", "true");*/
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Cart> oldCartList = findCartList();
            List<Cart> cartList1 = cartService.addGoodsToCartList(oldCartList, itemId, num);
            if ("anonymousUser".equals(name)) {
                CookieUtil.setCookie(request, response, CONSTANT.CART_LIST_COOKIE_KEY, JSON.toJSONString(cartList1), 3600 * 24, "UTF-8");
            }else{
                cartService.saveCartListToRedis(name,cartList1);
            }
            return new Result(true,"添加购物车成功");

        }catch (Exception e){
            e.printStackTrace();
            return  new Result(false,e.toString());
        }

    }


    /**
     * 登陆过，那么从redis中取，未登录，从cookie中取
     *
     * @return
     */
    @RequestMapping("/findCartList")
    public List<Cart> findCartList() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(name);
        String cartList = CookieUtil.getCookieValue(request, CONSTANT.CART_LIST_COOKIE_KEY, "UTF-8");
        if (cartList==null || cartList.equals("")){
            cartList="[]";
        }
        List<Cart> cartList_cookie = JSON.parseArray(cartList,Cart.class);
        if ("anonymousUser".equals(name)){

            return cartList_cookie;
        }else{
            List<Cart> cartList_redis = cartService.findCartListFromRedis(name);
            if (cartList_cookie.size()>0){
                cartList_redis = cartService.mergeCartList(cartList_redis,cartList_cookie);
                //因为已经登录过了，清空cookie信息
                CookieUtil.deleteCookie(request,response,CONSTANT.CART_LIST_COOKIE_KEY);
                //将最新的购物车列表保存到redis中
                cartService.saveCartListToRedis(name,cartList_redis);
            }
            return cartList_redis;
        }

    }
}
