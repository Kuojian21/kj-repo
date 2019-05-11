package com.kj.repo.spring.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kuojian21
 */
@RestController
public class IndexController {

    @RequestMapping("/")
    public Object index() {
        return "ok";
    }

}
