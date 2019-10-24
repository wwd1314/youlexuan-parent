package com.youlexuan.shop.controller;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {


    @RequestMapping("/name")
    public Map name(){
        Map map = new HashMap();
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        map.put("username",userName);
        map.put("lastLoginTime",new Date());
        return map;
    }
}
