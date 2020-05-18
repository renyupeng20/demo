package com.example.springbootactivitidemo;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringbootActivitiDemoApplicationTests {


    @Autowired
    private StringEncryptor stringEncryptor;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;

    @Test
    public void contextLoads() {
        //加密方法  Wp7Q02lVsr8Wh4PSnInKgg==
        System.out.println(stringEncryptor.encrypt("123456"));
        System.out.println(stringEncryptor.decrypt("Wp7Q02lVsr8Wh4PSnInKgg=="));
    }

    @Test
    void contextLoads1() {
        System.out.println("Number of process definitions : " + repositoryService.createProcessDefinitionQuery().count());
        System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
        //runtimeService.startProcessInstanceByKey("oneTaskProcess");
        //System.out.println("Number of tasks after process start: " + taskService.createTaskQuery().count());
    }


}
