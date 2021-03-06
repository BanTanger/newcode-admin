package com.bantanger.controller;

import com.bantanger.dao.CommentMapper;
import com.bantanger.dao.DiscussPostMapper;
import com.bantanger.entity.Comment;
import com.bantanger.entity.DiscussPost;
import com.bantanger.entity.Page;
import com.bantanger.entity.User;
import com.bantanger.service.CommentService;
import com.bantanger.service.DiscussPostService;
import com.bantanger.service.UserService;
import com.bantanger.util.CommunityConstant;
import com.bantanger.util.CommunityUtil;
import com.bantanger.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.management.ObjectName;
import java.sql.Array;
import java.util.*;

/**
 * @author bantanger 半糖
 * @version 1.0
 * @Date 2022/7/29 17:39
 */

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        // 用户未登录！
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登陆哦！");
        }
        // 用户有权限：
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        // 对文章内容进行处理
        discussPostService.addDiscussPost(discussPost);

        return CommunityUtil.getJSONString(200, "发布成功！");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model,
                                 Page page) {
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        // 作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

        // 评论Vo列表（展现给用户看到的数据列表，包含user和comment类）
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(commentList != null) {
            for (Comment comment : commentList) {
                // 评论Vo
                Map<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 作者
                commentVo.put("user", user);

                // 回复列表(嵌套在评论里）
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 回复Vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if(replyList != null) {
                    for(Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply );
                        // 作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 判断有无回复目标
                        User targetUser = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", targetUser);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);

                // 回复数量
                int replyCount = commentService.findCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }
}
