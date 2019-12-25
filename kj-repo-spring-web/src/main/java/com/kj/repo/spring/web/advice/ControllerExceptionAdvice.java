package com.kj.repo.spring.web.advice;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.kj.repo.infra.base.ResultInfo;

/**
 * @author kj
 */
@ControllerAdvice
public class ControllerExceptionAdvice {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @InitBinder
    public void initBinder(WebDataBinder binder) {
    }

    @ModelAttribute
    public void modelAttribute(Model model) {
        model.addAttribute("author", "kj");
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResultInfo<Void> exceptionHandler(HttpServletRequest request, Throwable t) {
        logger.info("", t);
        return ResultInfo.fail("exception");
    }

}
