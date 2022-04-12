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
            model.addAttribute("error","你还没有选择图片");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        if(filename==null){
            model.addAttribute("error","图片名错误");
            return "/site/setting";
        }
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;
//        File dest = new File(uploadPath+"/"+filename);
        filename = "header/" + filename;
        try {
//            headerImage.transferTo(dest);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,filename,headerImage.getInputStream());
            Map<String, String> map = new HashMap<>();
            //设置为公开读可见
            map.put("x-oss-object-acl","public-read");
            putObjectRequest.setHeaders(map);
            PutObjectResult putResult = ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            logger.error("上传文件失败："+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！",e);
        } finally {
//            ossClient.shutdown();
        }
        //更新当前头像路径
        User user = hostHolder.getUser();
//        String headerUrl = getUrl(filename);
        String headerUrl = "https://" + bucketName + "." + endpoint + "/" + filename;
//        String headerUrl = domain + contextPath + "/user/header/" + filename;
        int i = userService.updateHeader(user.getId(), headerUrl);
        System.out.println(i);
        return "redirect:/index";
    }
    
//    //废弃
//    @LoginRequired
//    @RequestMapping(path = "/upload" , method = RequestMethod.POST)
//    public String uploadHeader(MultipartFile headerImage , Model model){
//        if( headerImage == null ){
//            model.addAttribute("error","您还没有上传图片！");
//            return "/site/setting";
//        }
//
//        String filename = headerImage.getOriginalFilename();
//        String suffix = filename.substring(filename.lastIndexOf("."));
//        if(StringUtils.isBlank(suffix)){
//            model.addAttribute("error","文件的格式不正确！");
//            return "/site/setting";
//        }
//
//        // 生成随机文件名
//        filename = CommunityUtil.generateUUID() + suffix ;
//        //确定文件的存放路径
//        File dest = new File(uploadPath  +  "/"  +  filename);
//        try {
//            //存储文件
//            headerImage.transferTo(dest);
//        } catch (IOException e) {
//            logger.error("上传文件失败" + e.getMessage());
//            throw new RuntimeException("上传文件失败，服务器发生异常",e);
//        }
//
//        // 更新当前用户的头像的路径（web访问路径）
//        // http://localhost:8080/community/user/header/xxx.png
//        User user = hostHolder.getUser();
//        String headerUrl = domain + contextPath + "/user/header/" + filename;
//        userService.updateHeader(user.getId(),headerUrl);
//
//        return "redirect:/index";
//    }

//    //废弃
//    @RequestMapping(path = "/header/{filename}" , method = RequestMethod.GET )
//    public void getHeader(@PathVariable("filename") String filename , HttpServletResponse response){
//
//        //服务器存放路径
//        filename = uploadPath + "/" + filename;
//        //文件后缀解析
//        String suffix = filename.substring(filename.lastIndexOf("."));
//        //响应图片
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
//            logger.error("获取头像失败: " + e.getMessage() );
//        }
//
//
//    }

    /**
     * 获取oss中头像的url
     * @param fileName
     * @return
     */
    public String getUrl(String fileName) {
        // 设置URL过期时间为2年  3600l* 1000*24*365*2
        Date expiration = new Date(new Date().getTime() + 3600l * 1000 * 24 * 365 * 2);
        
        // 生成URL
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName);
        generatePresignedUrlRequest.setExpiration(expiration);
        URL url = ossClient.generatePresignedUrl(generatePresignedUrlRequest);
        
        if (url != null)
        {
            return url.toString();
        }
        return null;
    }
    

    //修改密码
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

    //个人主页
    @RequestMapping(path = "/profile/{userId}" , method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId ,Model model){
        User user = userService.findUserById(userId);
        if( user == null ){
            throw new RuntimeException("该用户不存在" );
        }
        
        //用户的基本信息
        model.addAttribute("user",user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        
        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);
        //是否已关注
        boolean hasFollowed = false;
        if(hostHolder.getUser()!= null ){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        
        
        return "/site/profile";
    }
    
    //我的帖子
    @RequestMapping(path = "/mypost/{userId}" , method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId , Page page , Model model ){
        User user = userService.findUserById(userId);
        if( user == null ){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user",user);
        
        //分页信息
        page.setPath("/user/mypost/"+userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        
        //帖子列表
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
    
    //我的回复
    @RequestMapping(path = "/myreply/{userId}" , method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId , Page page , Model model ){
        User user = userService.findUserById(userId);
        if(user == null ){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user",user);
        
        //分页信息
        page.setPath("/user/myreply/" + userId);
        page.setRows(commentService.findUserCount(userId));
        
        //回复列表
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
