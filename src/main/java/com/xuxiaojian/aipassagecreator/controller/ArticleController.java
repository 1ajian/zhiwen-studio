package com.xuxiaojian.aipassagecreator.controller;

import com.mybatisflex.core.paginate.Page;
import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.DeleteRequest;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.manager.SseEmitterManager;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleCreateRequest;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleQueryRequest;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.vo.ArticleVO;
import com.xuxiaojian.aipassagecreator.service.ArticleService;
import com.xuxiaojian.aipassagecreator.service.UserService;
import com.xuxiaojian.aipassagecreator.service.impl.ArticleAsyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ClassName: ArticleController
 * Package: com.xuxiaojian.aipassagecreator.controller
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-29 0:06
 * @Version 1.0
 */
@RestController
@RequestMapping("/article")
@Tag(name = "文章接口")
@Slf4j
public class ArticleController {
    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR,"选题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        String taskId = articleService.createArticleTask(request.getTopic(), loginUser);

        //异步执行生成文章
        articleAsyncService.executeArticleGeneration(taskId,request.getTopic());

        return ResultUtils.success(taskId);
    }

    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取文章生成进度(SSE)")
    public SseEmitter getProgress(@PathVariable String taskId,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),ErrorCode.PARAMS_ERROR,"任务ID不能为空");

        //校验权限
        User loginUser = userService.getLoginUser(httpServletRequest);
        articleService.getArticleDetail(taskId,loginUser);

        //创建SSE Emitter
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        log.info("SSE连接已建立,taskId={}",taskId);
        return emitter;
    }


    @GetMapping("/{taskId}")
    @Operation(summary = "获取文章详情")
    @AuthCheck(mustRole = "user")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId,HttpServletRequest request) {
        ThrowUtils.throwIf(taskId == null || taskId.trim().isEmpty(),ErrorCode.PARAMS_ERROR,"任务ID不能为空");
        User loginUser = userService.getLoginUser(request);
        ArticleVO articlVO = articleService.getArticleDetail(taskId, loginUser);
        return ResultUtils.success(articlVO);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询文章列表")
    @AuthCheck(mustRole = "user")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request,
                                                     HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<ArticleVO> articleVOPage = articleService.listArticleByPage(request, loginUser);
        return ResultUtils.success(articleVOPage);

    }

    @PostMapping("/delete")
    @Operation(summary = "删除文章")
    @AuthCheck(mustRole = "user")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest deleteRequest,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        Boolean result = articleService.deleteArticle(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }
}


