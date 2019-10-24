package com.youlexuan.pay.service;

import com.youlexuan.pojo.TbPayLog;

public interface PayLogService {
    /**
     * 根据userId查找之父日志
     *      作用一：生成二维码时从支付日志中得到支付的ID一级之父的金额
     */
    public TbPayLog searchPayLogByUserId(String userID);
}
