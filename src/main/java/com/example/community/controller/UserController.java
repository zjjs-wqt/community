package com.example.community.controller;

import ch.qos.logback.core.status.StatusUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.example.community.annotation.LoginRequired;
import com.example.community.entity.Comment;
import com.example.community.entity.DiscussPost;
import com.example.community.entity.Page;
import com.example.community.entity.User;
import com.example.community.service.*;
import com.example.community.util.CommunityConstant;
import com.example.community.util.CommunityUtil;
import com.example.community.util.HostHolder;
import com.sun.mail.imap.protocol.MODSEQ;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;



import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;
import java.io.*;
import java.net.URL;
import java.util.*;

@Controller
@RequestMapping("user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;
    
    @Value("${community.path.domain}")
    private String domain;
    
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;
    
    @Autowired
    private FollowService followService;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private CommentService commentService;

    @Value ("${aliyun.bucket-name}")
    private String bucketName;

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Autowired
    private OSS ossClient;
    
    @LoginRequired
    @RequestMapping(path = "/setting" , method = RequestMethod.GET)
    public String getSetttingPage(){
        return "/site/setting";
    }



    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","????????????????????????");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        if(filename==null){
            model.addAttribute("error","???????????????");
            return "/site/setting";
        }
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","?????????????????????");
            return "/site/setting";
        }
        //?????????????????????
        filename = CommunityUtil.generateUUID() + suffix;
//        File dest = new File(uploadPath+"/"+filename);
        filename = "header/" + filename;
        try {
//            headerImage.transferTo(dest);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,filename,headerImage.getInputStream());
            Map<String, String> map = new HashMap<>();
            //????????????????????????
            map.put("x-oss-object-acl","public-read");
            putObjectRequest.setHeaders(map);
            PutObjectResult putResult = ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            logger.error("?????????????????????"+e.getMessage());
            throw new RuntimeException("?????????????????????????????????????????????",e);
        } finally {
//            ossClient.shutdown();
        }
        //????????????????????????
        User user = hostHolder.getUser();
//        String headerUrl = getUrl(filename);
        String headerUrl = "https://" + bucketName + "." + endpoint + "/" + filename;
//        String headerUrl = domain + contextPath + "/user/header/" + filename;
        int i = userService.updateHeader(user.getId(), headerUrl);
        System.out.println(i);
        return "redirect:/index";
    }
    
//    //??????
//    @LoginRequired
//    @RequestMapping(path = "/upload" , method = RequestMethod.POST)
//    public String uploadHeader(MultipartFile headerImage , Model model){
//        if( headerImage == null ){
//            model.addAttribute("error","???????????????????????????");
//            return "/site/setting";
//        }
//
//        String filename = headerImage.getOriginalFilename();
//        String suffix = filename.substring(filename.lastIndexOf("."));
//        if(StringUtils.isBlank(suffix)){
//            model.addAttribute("error","???????????????????????????");
//            return "/site/setting";
//        }
//
//        // ?????????????????????
//        filename = CommunityUtil.generateUUID() + suffix ;
//        //???????????????????????????
//        File dest = new File(uploadPath  +  "/"  +  filename);
//        try {
//            //????????????
//            headerImage.transferTo(dest);
//        } catch (IOException e) {
//            logger.error("??????????????????" + e.getMessage());
//            throw new RuntimeException("??????????????????????????????????????????",e);
//        }
//
//        // ???????????????????????????????????????web???????????????
//        // http://localhost:8080/community/user/header/xxx.png
//        User user = hostHolder.getUser();
//        String headerUrl = domain + contextPath + "/user/header/" + filename;
//        userService.updateHeader(user.getId(),headerUrl);
//
//        return "redirect:/index";
//    }

//    //??????
//    @RequestMapping(path = "/header/{filename}" , method = RequestMethod.GET )
//    public void getHeader(@PathVariable("filename") String filename , HttpServletResponse response){
//
//        //?????????????????????
//        filename = uploadPath + "/" + filename;
//        //??????????????????
//        String suffix = filename.substring(filename.lastIndexOf("."));
//        //????????????
//        response.setContentType("image/" + suffix );
//        try (
//                FileInputStream fis = new FileInputStream(filename);
//                OutputStream os = response.getOutputStream();
//        ){
//
//            byte[] buffer = new byte[1024];
//            int b = 0 ;
//            while (( b = fis.read(buffer)) != -1 ){
//                os.write(buffer , 0 , b);
//            }
//        } catch (IOException e) {
//            logger.error("??????????????????: " + e.getMessage() );
//        }
//
//
//    }

    /**
     * ??????oss????????????url
     * @param fileName
     * @return
     */
    public String getUrl(String fileName) {
        // ??????URL???????????????2???  3600l* 1000*24*365*2
        Date expiration = new Date(new Date().getTime() + 3600l * 1000 * 24 * 365 * 2);
        
        // ??????URL
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName);
        generatePresignedUrlRequest.setExpiration(expiration);
        URL url = ossClient.generatePresignedUrl(generatePresignedUrlRequest);
        
        if (url != null)
        {
            return url.toString();
        }
        return null;
    }
    

    //????????????
    @LoginRequired
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword (String oldPassword , String newPassword , String confirmPassword , Model model){
        User user = hostHolder.getUser();
        Map<String ,Object> map  =  userService.updatePassword(user.getId(),oldPassword,newPassword , confirmPassword );
        if( map == null || map.isEmpty()){
            return "redirect:/logout";
        }
        else {
            model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

    //????????????
    @RequestMapping(path = "/profile/{userId}" , method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId ,Model model){
        User user = userService.findUserById(userId);
        if( user == null ){
            throw new RuntimeException("??????????????????" );
        }
        
        //?????????????????????
        model.addAttribute("user",user);
        //????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        
        //????????????
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);
        //???????????????
        boolean hasFollowed = false;
        if(hostHolder.getUser()!= null ){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        
        
        return "/site/profile";
    }
    
    //????????????
    @RequestMapping(path = "/mypost/{userId}" , method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId , Page page , Model model ){
        User user = userService.findUserById(userId);
        if( user == null ){
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user",user);
        
        //????????????
        page.setPath("/user/mypost/"+userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        
        //????????????
        List<DiscussPost> discussPostList = 
                discussPostService.findDiscussPost(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String,Object>> discussVOList = new ArrayList<>();
        if(discussPostList != null ){
            for(DiscussPost post : discussPostList){
                Map<String , Object > map = new HashMap<>();
                map.put("discussPost", post);
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                discussVOList.add(map);
            }
        }
        model.addAttribute("discussPosts",discussVOList);
        
        return "/site/my-post";
    }
    
    //????????????
    @RequestMapping(path = "/myreply/{userId}" , method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId , Page page , Model model ){
        User user = userService.findUserById(userId);
        if(user == null ){
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user",user);
        
        //????????????
        page.setPath("/user/myreply/" + userId);
        page.setRows(commentService.findUserCount(userId));
        
        //????????????
        List<Comment> commentList = commentService.findUserComments(userId,page.getOffset(), page.getLimit());
        List<Map<String,Object>> commentVOList = new ArrayList<>();
        if(commentList != null ){
            for(Comment comment : commentList ){
                Map<String , Object > map = new HashMap<>();
                map.put("comment", comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("discussPost", post);
                commentVOList.add(map);
            }
        }
        
        model.addAttribute("comments",commentVOList);
        
        return "/site/my-reply";
    }

}
