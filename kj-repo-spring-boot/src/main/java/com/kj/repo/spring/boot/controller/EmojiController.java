package com.kj.repo.spring.boot.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kj
 */
@RestController
public class EmojiController {

    @RequestMapping("/emoji")
    public void emoji(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("⬇️⬇️⬇");
    }

}
