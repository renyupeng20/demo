package com.example.springbootactivitidemo;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringbootActivitiDemoApplicationTests {


    @Autowired
    private StringEncryptor stringEncryptor;

    @Test
    public void contextLoads() {
        //加密方法
        System.out.println(stringEncryptor.encrypt(""));
        System.out.println(stringEncryptor.decrypt(""));
    }


}
