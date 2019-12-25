package com.kj.repo.spring.boot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kj
 */
@RestController
public class IndexController {

    @RequestMapping("/")
    public Object index() {
        return "ok";
    }

}
