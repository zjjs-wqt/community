package com.example.community.event;


import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.example.community.entity.DiscussPost;
import com.example.community.entity.Event;
import com.example.community.entity.Message;
import com.example.community.entity.Page;
import com.example.community.service.DiscussPostService;
import com.example.community.service.ElasticSearchService;
import com.example.community.service.MessageService;
import com.example.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {
    
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value ("${aliyun.bucket-name}")
    private String bucketName;

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Autowired
    private OSS ossClient;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if(record == null || record.value() == null ){
            logger.error("??????????????????!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null ){
            logger.error("??????????????????!");
            return;
        }
        
        //??????????????????
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String ,Object > content = new HashMap<>() ;
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        
        if(!event.getData().isEmpty()){
            for(Map.Entry<String , Object > entry : event.getData().entrySet() ){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        
        message.setContent(JSONObject.toJSONString(content));
        
        messageService.addMessage(message);
    }
    
    //??????????????????
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("?????????????????????");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("?????????????????????");
            return;
        }
        
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.saveDiscussPost(post);
    }

    //??????????????????
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("?????????????????????");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("?????????????????????");
            return;
        }
        
        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }
    
    
    //??????????????????
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("?????????????????????");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("?????????????????????");
            return;
        }
        
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");
        
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("??????????????????: " + cmd);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("??????????????????: " + e.getMessage() );
        }

        // ??????????????????.
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements Runnable{
        // ????????????
        private String fileName;
        // ????????????
        private String suffix;
        // ????????????????????????
        private Future future;
//        // ????????????
//        private long startTime;
//        // ????????????
//        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
//            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }
        
        @Override
        public void run() {
//            // ????????????
//            if (System.currentTimeMillis() - startTime > 30000) {
//                logger.error("??????????????????,????????????:" + fileName);
//                future.cancel(true);
//                return;
//            }
//            // ????????????
//            if (uploadTimes >= 3) {
//                logger.error("??????????????????,????????????:" + fileName);
//                future.cancel(true);
//                return;
//            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            String FileName = "share/" + fileName;
            if (file.exists()) {
                logger.info(String.format("????????????[%s].",FileName));
                // ??????????????????
                try {
//                  headerImage.transferTo(dest);
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, FileName, file);
                    Map<String, String> map = new HashMap<>();
                    //????????????????????????
                    map.put("x-oss-object-acl", "public-read");
                    putObjectRequest.setHeaders(map);
                    PutObjectResult putResult = ossClient.putObject(putObjectRequest);
                } finally {
                    logger.info(String.format("????????????[%s].",fileName));
                    future.cancel(true);
                }
            } else {
                logger.info("??????????????????[" + fileName + "].");
            }
        }
    
    }
}
