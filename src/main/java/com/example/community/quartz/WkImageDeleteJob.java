package com.example.community.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

public class WkImageDeleteJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(WkImageDeleteJob.class);
    
    @Value("${wk.image.storage}")
    private String wkImageStorage ;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        File[] files = new File(wkImageStorage).listFiles();
        if(files == null || files.length == 0 ){
            logger.info("[任务取消]没有WK图片!");
            return;
        }
        
        for(File file : files ){
            //删除一分钟之前创建的图片
            if(System.currentTimeMillis() - file.lastModified() > 60 * 1000 ){
                logger.info("删除WK图片:" + file.getName());
                file.delete();
            }
        }
    }
}
