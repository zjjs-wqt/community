package com.example.community.util;


import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    
    //替换符
    private static final String REPLACEMENT="***";
    
    //初始根节点
    private TrieNode rootNode = new TrieNode();
    
    //初始化方法
    @PostConstruct
    public void init(){
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                )
        {
            String keyword ;
            while((keyword = reader.readLine())!=null){
                //添加到前缀数
                this.addKeyWord(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感文件失败：" + e.getMessage());
        }


    }

    //将一个敏感词添加到前缀数中
    private void addKeyWord(String keyword) {
        TrieNode tempNode = rootNode;
        for(int i = 0 ; i < keyword.length(); i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            
            if(subNode == null ){
                //初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c,subNode);
            }
            
            //指针指向子节点，进入下一轮循环
            tempNode = subNode;
            
            //设置结束标识
            if( i  == keyword.length() - 1 ){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针一
        TrieNode tempNode = rootNode;
        
        //指针二
        int begin = 0 ;
        
        //指针三
        int position = 0 ;
        
        //结果
        StringBuilder sb = new StringBuilder();
        
        while( begin < text.length() ){
            if(position < text.length() ) {
                Character c = text.charAt(position);

                //跳过符号
                if (isSymbol(c)) {
                    //若指针处于根节点。将此符号记入结果，让指针二往下走一步
                    if (tempNode == rootNode) {
                        begin++;
                        sb.append(c);
                    }
                    position++;
                    continue;
                }
                tempNode = tempNode.getSubNode(c);
                if (tempNode == null) {
                    //以begin开头的字符串不是敏感词
                    sb.append(text.charAt(position));
                    //进入下一位置
                    position = ++begin ;

                    // 重新指向根节点
                    tempNode = rootNode;
                }
                else if(tempNode.isKeywordEnd()){
                    sb.append(REPLACEMENT);
                    begin=++position;
                    tempNode = rootNode;
                }
                else {
                    position++;
                }


            }  
            else {
                sb.append(text.charAt(begin));
                position = ++begin;
            }
        }
        return sb.toString();
    }
    
    //判断是否为符号
    private boolean isSymbol(Character c){
        // 0X2E80 - 0X9FFF 东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c)  && (c < 0x2E80 || c > 0X9FFF);
    }
    //前缀树
    private class TrieNode {
        
        //关键词结束标识
        private boolean isKeywordEnd = false ;

        //子节点(map中key是下级字符，value是下级节点)
        private Map< Character,TrieNode> subNodes = new HashMap<>();
        
        
        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }



        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
        
        //添加子节点
        public void addSubNode(Character c , TrieNode node){
            subNodes.put(c,node);
        }
        
        //获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
    
}
