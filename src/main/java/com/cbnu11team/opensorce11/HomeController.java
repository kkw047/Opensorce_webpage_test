package com.cbnu11team.opensorce11;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    // 1) 텍스트로 바로 확인 (http://localhost:8080/ok)
    @GetMapping("/ok")
    @ResponseBody
    public String ok() {
        return "OK - Spring Boot is running";
    }

    // 2) 뷰 파일로 확인 (http://localhost:8080/)
    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html을 찾아서 렌더링
    }
}
