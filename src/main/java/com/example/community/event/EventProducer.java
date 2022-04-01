package com.example.community.event;


import com.alibaba.fastjson.JSONObject;
import com.example.community.entity.Event;
import com.example.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    
    @Autowired
    private KafkaTemplate kafkaTemplate;
    
    //处理事件
    public void fireEvent(Event event){
        //将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
    
    
}
