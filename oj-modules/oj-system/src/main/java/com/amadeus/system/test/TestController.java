package com.amadeus.system.test;

import com.amadeus.system.test.service.ITestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ITestService testService;

    @RequestMapping("/list")
    public List<?> getList(){
        return testService.list();
    }

}
