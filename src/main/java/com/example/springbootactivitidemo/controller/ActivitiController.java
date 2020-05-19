package com.example.springbootactivitidemo.controller;

import com.example.springbootactivitidemo.util.CustomProcessDiagramGenerator;
import com.example.springbootactivitidemo.util.NewCustomProcessDiagramGenerator;
import com.example.springbootactivitidemo.util.WorkflowConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.val;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ActivitiController {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 1 .在线创建模型
     */
    @RequestMapping("/create")
    public void create(HttpServletRequest request, HttpServletResponse response, String name, String key) throws IOException {
        try {
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            Model modelData = repositoryService.newModel();

            ObjectNode modelObjectNode = objectMapper.createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, "");
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
            modelData.setMetaInfo(modelObjectNode.toString());
            modelData.setName(name);
            modelData.setKey(key);

            //保存模型
            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
            response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelData.getId());
        } catch (Exception e) {
            System.out.println("创建模型失败：");
        }
    }


    /**
     * 2 .将创建好的模型部署到流程定义中
     *
     * @param modelId
     * @return
     * @throws Exception
     */
    @GetMapping("/deployProcess")
    @ResponseBody
    public String deploy(String modelId) throws Exception {
        //获取模型
        Model modelData = repositoryService.getModel(modelId);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
            return "模型数据为空，请先设计流程并成功保存，再进行发布。";
        }
        JsonNode modelNode = new ObjectMapper().readTree(bytes);
        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return "数据模型不符要求，请至少设计一条主线流程。";
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
        //发布流程
        String processName = modelData.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(modelData.getName())
                .addString(processName, new String(bpmnBytes, "UTF-8"))
                .deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);
        return "SUCCESS";
    }


    /**
     * 3 .启动流程
     */
    @GetMapping("/startProcess")
    @ResponseBody
    public String startProcess(String processKey, int leaveDays) {
        Map<String, Object> variables = new HashMap();
        if (leaveDays > 1) {
            variables.put("action", false);
        } else {
            variables.put("action", true);
        }
        //使用流程定义Key启动，key对应bpmn文件中的id，key启动默认是最新版本的流程定义
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
        System.out.println("流程实例id" + processInstance.getId());
        System.out.println("流程定义ID" + processInstance.getProcessDefinitionId());//流程定义ID
        return "流程实例id" + processInstance.getId() + "流程定义ID" + processInstance.getProcessDefinitionId();
    }


    /**
     * 4 .查询代理人任务
     */
    @GetMapping("/queryTask")
    @ResponseBody
    public String queryTask(String assignee) {
        //创建一个任务查询对象
        TaskQuery taskQuery = taskService.createTaskQuery();
        //办理人的任务列表
        List<Task> list = taskQuery.taskAssignee(assignee).orderByTaskCreateTime().desc().list();
        //遍历任务列表
        if (!CollectionUtils.isEmpty(list)) {
            return list.stream().map(p -> {
                System.out.println("任务的办理人：" + p.getAssignee());
                System.out.println("任务的id：" + p.getId());
                System.out.println("任务的名称：" + p.getName());
                return "任务的办理人：" + p.getAssignee() + "任务的id：" + p.getId() + "任务的名称：" + p.getName();
            }).collect(Collectors.joining("=================="));
        }
        return "没有任务需要处理";
    }

    /**
     * 5 .处理任务
     */
    @GetMapping("/compileTask")
    @ResponseBody
    public String compileTask(String taskId, String comment) {
        val taskQuery = taskService.createTaskQuery();
        Task task = taskQuery.taskId(taskId).singleResult();
        return Optional.ofNullable(task).map(p -> {
            String processInstanceId = p.getProcessInstanceId();
            taskService.addComment(taskId, processInstanceId, comment);
            taskService.complete(p.getId());
            return taskId + "任务处理成功!";
        }).orElseGet(() -> "没有" + taskId + "的任务");
    }

    /**
     * 6 .查看流程图
     *
     * @throws Exception
     */
    @GetMapping(value = "/viewImage", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] getFlowImgByInstanceId(String processInstanceId) throws IOException {
        // 获取历史流程实例
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();


        // 获取流程中已经执行的节点，按照执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceId().asc().list();

        // 高亮已经执行流程节点ID集合
        List<String> executedActivityIdList = historicActivityInstances.stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());

        ProcessDiagramGenerator processDiagramGenerator = new CustomProcessDiagramGenerator();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
        // 高亮流程已发生流转的线id集合
        List<String> highLightedFlowIds = executedFlowIdList(historicActivityInstances, bpmnModel);
        // 使用默认配置获得流程图表生成器，并生成追踪图片字符流
        InputStream inputStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", executedActivityIdList, highLightedFlowIds, "宋体", "微软雅黑", "黑体", null, 2.0);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        return bytes;
    }


    private List<String> executedFlowIdList(List<HistoricActivityInstance> activities, BpmnModel bpmnModel) {
        List<String> executedFlowIdList = new ArrayList<>();
        activities.forEach(act -> executedFlowIdList.addAll(getIncomeActivities(act, activities, bpmnModel)));
        return executedFlowIdList;
    }

    private List<String> getIncomeActivities(HistoricActivityInstance act, List<HistoricActivityInstance> activities, BpmnModel bpmnModel) {
        List<String> executedFlowIdList = new ArrayList<>();
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(act.getActivityId());//获取该任务在图中的node
        List<SequenceFlow> sequenceFlows = flowNode.getIncomingFlows();//获取该node的所有入口
        if (CollectionUtils.isEmpty(sequenceFlows)) {
            return executedFlowIdList;
        }
        sequenceFlows.forEach(seq -> activities.forEach(hact -> {
            GraphicInfo labelGraphicInfo = bpmnModel.getLabelGraphicInfo(hact.getId());
            if (hact.getEndTime() != null && seq.getSourceRef().equals(hact.getActivityId())) {
                executedFlowIdList.add(seq.getId());
                return;
            }
        }));
        return executedFlowIdList;
    }


    /**
     * 自定义画流程图类后的查看流程图方法
     *
     * @param processInstanceId
     * @param response
     * @throws Exception
     */
    @GetMapping("/view")
    @ResponseBody
    public void processTracking(String processInstanceId, HttpServletResponse response) throws Exception {
        // 获取历史流程实例
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());

        // 获取流程中已经执行的节点，按照执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceId().asc().list();

        // 高亮已经执行流程节点ID集合
        List<String> executedActivityIdList = historicActivityInstances.stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());

        // 高亮流程已发生流转的线id集合
        List<String> highLightedFlowIds = executedFlowIdList(historicActivityInstances, bpmnModel);

        Set<String> currIds = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list()
                .stream().map(e -> e.getActivityId()).collect(Collectors.toSet());

        InputStream inputStream = new NewCustomProcessDiagramGenerator()
                .generateDiagram(bpmnModel, WorkflowConstants.IMAGE_TYPE, executedActivityIdList, highLightedFlowIds,
                        WorkflowConstants.ACTIVITY_FONT_NAME, WorkflowConstants.LABEL_FONT_NAME, WorkflowConstants.ANNOTATION_FONT_NAME,
                        WorkflowConstants.CLASS_LOADER, WorkflowConstants.SCALE_FACTOR,
                        new Color[]{WorkflowConstants.COLOR_NORMAL, WorkflowConstants.COLOR_CURRENT}, currIds);
        int len;
        byte[] b = new byte[1024];

        while ((len = inputStream.read(b)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }

    }


}
