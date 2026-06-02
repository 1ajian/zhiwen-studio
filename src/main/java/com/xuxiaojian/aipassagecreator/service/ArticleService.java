package com.xuxiaojian.aipassagecreator.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleQueryRequest;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.ArticlePhaseEnum;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStatusEnum;
import com.xuxiaojian.aipassagecreator.model.vo.ArticleVO;

import java.util.List;

/**
 * ClassName: ArticleService
 * Package: com.xuxiaojian.aipassagecreator.service
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-27 23:50
 * @Version 1.0
 */
public interface ArticleService extends IService<Article> {
    /**
     * 创建文章任务
     * @param topic
     * @param loginUser
     * @return
     */
    String createArticleTask(String topic,String style,User loginUser);

    /**
     * 通过任务Id获取文章信息
     * @param taskId
     * @return
     */
    Article getByTaskId(String taskId);

    /**
     * 更新文章状态信息
     * @param taskId
     * @param status
     * @param errorMessage
     */
    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    /**
     * 保存文章内容
     * @param taskId
     * @param state
     */
    void saveArticleContent(String taskId, ArticleState state);

    /**
     * 分页获取文章列表
     * @param request
     * @param loginUser
     * @return
     */
    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    /**
     * 删除文章
     * @param id
     * @param loginUser
     * @return
     */
    Boolean deleteArticle(Long id, User loginUser);

    /**
     * 获取文章并权限校验，判断用户是否有权限看此文章
     * @param taskId
     * @param loginUser
     */
    ArticleVO getArticleDetail(String taskId, User loginUser);

    /**
     * 创建文章任务并进行配额检查
     * @param topic
     * @param style
     * @param loginUser
     * @return
     */
    String createArticleTaskWithQuotaCheck(String topic, String style, User loginUser);

    /**
     * 确定标题（用户选择后）
     * @param taskId 任务ID
     * @param mainTitle 主标题
     * @param subTitle 副标题
     * @param userDescription 用户补充描述
     * @param loginUser 当前登录用户
     */
    void confirmTitle(String taskId,String mainTitle,String subTitle,String userDescription,User loginUser);

    /**
     * 确认大纲（用户编辑后）
     * @param taskId 任务ID
     * @param outline 用户编辑后大纲
     * @param loginUser 当前登录用户
     */
    void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline,User loginUser);

    /**
     * 更新阶段
     * @param taskId 任务ID
     * @param phase 阶段
     */
    void updatePhase(String taskId, ArticlePhaseEnum phase);

    /**
     * 保存标题方案
     * @param taskId 任务ID
     * @param titleOptions 标题方案列表
     */
    void saveTitleOptions(String taskId,List<ArticleState.TitleOption> titleOptions);

    /**
     * AI 修改大纲
     * @param taskId 任务ID
     * @param modifySuggestion 用户修改建议
     * @param loginUser 当前登录用户
     * @return
     */
    List<ArticleState.OutlineSection> aiModifyOutline(String taskId,String modifySuggestion,User loginUser);
}
