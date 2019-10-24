package com.youlexuan.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.youlexuan.order.service.OrderService;
import com.youlexuan.pay.service.AliPayService;
import com.youlexuan.pay.service.PayLogService;
import com.youlexuan.pojo.TbPayLog;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private AliPayService aliPayService;

    @Reference
    private PayLogService payLogService;

    @Reference
    private OrderService orderService;

    @RequestMapping("/creatNative")
    public Map creatNative(){
       String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(userId+"[[[[[[");
        TbPayLog payLog = payLogService.searchPayLogByUserId(userId);
        System.out.println(payLog+"-------");
        if (payLog!=null){
           String total_amount = payLog.getTotalFee()+"";
            String out_trade_no = payLog.getOutTradeNo()+"";
       /* String total_amount = "1";
        String out_trade_no = new IdWorker().nextId() +"";*/
            return aliPayService.createNative(out_trade_no,total_amount);
        }else{
            return new HashMap();
        }

    }





    @RequestMapping("/queryPayStatu")
    public Map queryPayStatu(String out_trade_no){
        Map resultMap =  null;
        int x = 0;
        while (true){
            try {
                resultMap = aliPayService.queryPayStatus(out_trade_no);
            }catch (Exception e){
                e.printStackTrace();
            }

            if(resultMap.get("tradestatus")!=null&&resultMap.get("tradestatus").equals("TRADE_SUCCESS")){
                resultMap.put("success",true);
                resultMap.put("message","交易成功");
                orderService.updateOrderStatus(out_trade_no, (String) resultMap.get("trade_no"));
                break;
            }

            if(resultMap.get("tradestatus")!=null&&resultMap.get("tradestatus").equals("TRADE_CLOSED")){
                resultMap.put("success",false);
                resultMap.put("message","未付款交易超时关闭，或支付完成后全额退款");
                break;
            }

            if(resultMap.get("tradestatus")!=null&&resultMap.get("tradestatus").equals("TRADE_FINISHED")){
                resultMap.put("success",true);
                resultMap.put("message","交易结束，不可退款");
                orderService.updateOrderStatus(out_trade_no, (String) resultMap.get("trade_no"));

                break;
            }

            //休息一会再调用
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            x++;

            if(x>3){
                resultMap.put("success",false);
                resultMap.put("tradestatus","timeout");
                resultMap.put("message","二维码超时");
                break;
            }

        }

        return  resultMap;

    }
}
