package com.baoxian.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsoleController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
