package com.xuxiaojian.aipassagecreator.controller;

import com.mybatisflex.core.paginate.Page;
import com.xuxiaojian.aipassagecreator.annotation.AuthCheck;
import com.xuxiaojian.aipassagecreator.common.BaseResponse;
import com.xuxiaojian.aipassagecreator.common.DeleteRequest;
import com.xuxiaojian.aipassagecreator.common.ResultUtils;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.manager.SseEmitterManager;
import com.xuxiaojian.aipassagecreator.model.dto.article.*;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStyleEnum;
import com.xuxiaojian.aipassagecreator.model.vo.ArticleVO;
import com.xuxiaojian.aipassagecreator.service.ArticleAsyncService;
import com.xuxiaojian.aipassagecreator.service.ArticleService;
import com.xuxiaojian.aipassagecreator.service.UserService;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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

        ThrowUtils.throwIf(!ArticleStyleEnum.isValid(request.getStyle()),ErrorCode.PARAMS_ERROR,"无效的文章风格");

        User loginUser = userService.getLoginUser(httpServletRequest);

        String taskId = articleService.createArticleTask(request.getTopic(),request.getStyle(), loginUser);

        //先这样做 后续会改
        Article article = articleService.getByTaskId(taskId);
        article.setEnabledImageMethods(GsonUtils.toJson(request.getEnabledImageMethods()));
        articleService.updateById(article);
        //异步执行生成文章
//        articleAsyncService.executeArticleGeneration(taskId,request.getTopic(),
//                request.getStyle(),
//                request.getEnabledImageMethods());

        //异步执行阶段1：异步生成标题方案
        articleAsyncService.executePhase1(taskId,request.getTopic(),request.getStyle());

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

    @PostMapping("/confirm_title")
    @Operation(summary = "确认标题并输入补充描述")
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getSelectedMainTitle() == null || request.getSelectedMainTitle().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "主标题不能为空");
        ThrowUtils.throwIf(request.getSelectedSubTitle() == null || request.getSelectedSubTitle().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "副标题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        articleService.confirmTitle(request.getTaskId(), request.getSelectedMainTitle(),request.getSelectedSubTitle()
                ,request.getUserDescription(),loginUser);

        //异步执行阶段2：生成大纲
        articleAsyncService.executePhase2(request.getTaskId());
        return ResultUtils.success(null);
    }

    @PostMapping("/confirm-outline")
    @Operation(summary = "确认大纲")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request,
                                             HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getOutline() == null || request.getOutline().isEmpty(),
                ErrorCode.PARAMS_ERROR, "大纲不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        // 确认大纲
        articleService.confirmOutline(
                request.getTaskId(),
                request.getOutline(),
                loginUser
        );

        //异步执行阶段3：生成正文 + 配图
        articleAsyncService.executePhase3(request.getTaskId());

        return ResultUtils.success(null);
    }

    @PostMapping("/ai-modify-outline")
    @Operation(summary = "AI 修改大纲")
    public BaseResponse<List<ArticleState.OutlineSection>> aiModifyOutline(
            @RequestBody ArticleAiModifyOutlineRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTaskId() == null || request.getTaskId().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(request.getModifySuggestion() == null || request.getModifySuggestion().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "修改建议不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);

        List<ArticleState.OutlineSection> modifiedOutline = articleService.aiModifyOutline(request.getTaskId(), request.getModifySuggestion(), loginUser);

        return ResultUtils.success(modifiedOutline);
    }
}


