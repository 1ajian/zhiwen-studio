package com.xuxiaojian.aipassagecreator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xuxiaojian.aipassagecreator.constant.UserConstant;
import com.xuxiaojian.aipassagecreator.exception.ErrorCode;
import com.xuxiaojian.aipassagecreator.exception.ThrowUtils;
import com.xuxiaojian.aipassagecreator.mapper.ArticleMapper;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleQueryRequest;
import com.xuxiaojian.aipassagecreator.model.dto.article.ArticleState;
import com.xuxiaojian.aipassagecreator.model.entity.Article;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import com.xuxiaojian.aipassagecreator.model.enums.ArticleStatusEnum;
import com.xuxiaojian.aipassagecreator.model.vo.ArticleVO;
import com.xuxiaojian.aipassagecreator.service.ArticleService;
import com.xuxiaojian.aipassagecreator.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.xuxiaojian.aipassagecreator.constant.UserConstant.ADMIN_ROLE;

/**
 * ClassName: ArticleServiceImpl
 * Package: com.xuxiaojian.aipassagecreator.service.impl
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-28 22:33
 * @Version 1.0
 */
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {


    @Override
    public String createArticleTask(String topic, User loginUser) {
        //生成任务ID
        String taskId = IdUtil.simpleUUID();

        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(loginUser.getId());
        article.setTopic(topic);
        article.setStatus(ArticleStatusEnum.PENDING.getValue());
        article.setCreateTime(LocalDateTime.now());

        this.save(article);

        log.info("文章任务已创建,taskId={},userId={}",taskId,loginUser.getId());
        return taskId;
    }

    @Override
    public Article getByTaskId(String taskId) {
        return this.getOne(QueryWrapper.create().eq("taskId",taskId));
    }

    @Override
    public void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在,taskId = {}",taskId);
            return;
        }

        article.setStatus(status.getValue());
        article.setErrorMessage(errorMessage);
        this.updateById(article);

        log.info("文章状态已更新,taskId={},status={}",taskId,status.getValue());
    }

    @Override
    public void saveArticleContent(String taskId, ArticleState state) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在,taskId = {}",taskId);
            return;
        }

        article.setMainTitle(state.getTitle().getMainTitle());
        article.setSubTitle(state.getTitle().getSubTitle());
        article.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
        article.setContent(state.getContent());
        article.setFullContent(state.getFullContent());

        if (CollUtil.isNotEmpty(state.getImages())) {
            ArticleState.ImageResult cover = state.getImages().stream()
                    .filter(img -> img.getPosition() != null && img.getPosition() == 1)
                    .findFirst()
                    .orElse(null);
            if (cover != null && StrUtil.isNotBlank(cover.getUrl())) {
                article.setCoverImage(cover.getUrl());
            }
        }

        article.setImages(GsonUtils.toJson(state.getImages()));
        article.setCompletedTime(LocalDateTime.now());

        this.updateById(article);
        log.info("文章保存成功,taskId={}",taskId);
    }

    @Override
    public Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser) {
        Long current = request.getPageNum();
        Long pageSize = request.getPageSize();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        if (!ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryWrapper.eq("userId",loginUser.getId());
        } else if (request.getUserId() != null) {
            queryWrapper.eq("userId",request.getUserId());
        }

        if (request.getStatus() != null && request.getStatus().trim().isEmpty()) {
            queryWrapper.eq("status",request.getStatus());
        }

        Page<Article> articlePage = this.page(new Page<>(current, pageSize), queryWrapper);
        return convertToVOPage(articlePage);
    }

    @Override
    public Boolean deleteArticle(Long id, User loginUser) {
        Article article = this.getById(id);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);

        //校验权限:只能删除自己的文章（管理员除外）
        checkArticlePermission(article,loginUser);

        //删除
        return this.removeById(id);
    }

    @Override
    public ArticleVO getArticleDetail(String taskId, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null,ErrorCode.NOT_FOUND_ERROR,"文章不存在");
        checkArticlePermission(article,loginUser);
        return ArticleVO.objToVo(article);
    }

    /**
     * 检查文章权限
     * @param article
     * @param loginUser
     */
    private void checkArticlePermission(Article article, User loginUser) {
        //非管理员
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            Long userId = article.getUserId();
            ThrowUtils.throwIf(!userId.equals(loginUser.getId()),ErrorCode.OPERATION_ERROR,"无法删除非本人创建的文章");
        }

    }

    /**
     * 普通文章对象转换为VO对象
     * @param articlePage
     * @return
     */
    private Page<ArticleVO> convertToVOPage(Page<Article> articlePage) {
        Page<ArticleVO> articleVOPage = new Page<>();
        articleVOPage.setPageNumber(articlePage.getPageNumber());
        articleVOPage.setPageSize(articlePage.getPageSize());
        articleVOPage.setTotalRow(articlePage.getTotalRow());

        List<ArticleVO> articleVOList = articlePage.getRecords().stream()
                .map(ArticleVO::objToVo)
                .collect(Collectors.toList());

        articleVOPage.setRecords(articleVOList);
        return articleVOPage;
    }
}
