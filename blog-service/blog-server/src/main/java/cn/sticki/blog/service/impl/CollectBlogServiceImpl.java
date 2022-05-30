package cn.sticki.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.sticki.blog.mapper.BlogBasicMapper;
import cn.sticki.blog.mapper.BlogGeneralMapper;
import cn.sticki.blog.mapper.CollectBlogMapper;
import cn.sticki.blog.pojo.BlogBasic;
import cn.sticki.blog.pojo.BlogListVO;
import cn.sticki.blog.pojo.CollectBlog;
import cn.sticki.blog.service.CollectBlogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CollectBlogServiceImpl extends ServiceImpl<CollectBlogMapper, CollectBlog> implements CollectBlogService {

	@Resource
	private BlogGeneralMapper blogGeneralMapper;

	@Resource
	private CollectBlogMapper collectBlogMapper;

	@Resource
	private BlogBasicMapper blogBasicMapper;

	@Override
	public boolean collectBlog(Integer userId, Integer blogId) {
		LambdaQueryWrapper<CollectBlog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CollectBlog::getUserId, userId);
		wrapper.eq(CollectBlog::getBlogId, blogId);
		CollectBlog selectOne = collectBlogMapper.selectOne(wrapper);
		if (selectOne != null) {
			// 内容已经存在
			collectBlogMapper.deleteById(selectOne);
			blogGeneralMapper.decreaseCollectionNum(blogId);
			return false;
		} else {
			CollectBlog collectBlog = new CollectBlog();
			collectBlog.setBlogId(blogId);
			collectBlog.setUserId(userId);
			collectBlog.setCreateTime(new Timestamp(System.currentTimeMillis()));
			collectBlogMapper.insert(collectBlog);
			blogGeneralMapper.increaseCollectionNum(blogId);
			return true;
		}
	}

	@Override
	public Long getCollectNum(Integer blogId) {
		LambdaQueryWrapper<CollectBlog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CollectBlog::getBlogId, blogId);
		return collectBlogMapper.selectCount(wrapper);
	}

	@Override
	public BlogListVO getCollectBlogList(@NotNull Integer userId, int page, int pageSize) {
		// 先查收藏表，获取收藏的博客id
		LambdaQueryWrapper<CollectBlog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CollectBlog::getUserId, userId);
		IPage<CollectBlog> iPage = new Page<>(page, pageSize);
		collectBlogMapper.selectPage(iPage, wrapper);
		BlogListVO blogListVO = BeanUtil.copyProperties(iPage, BlogListVO.class);
		List<CollectBlog> collectBlogList = iPage.getRecords();
		// 若为空，则直接返回
		if (collectBlogList.isEmpty()) {
			return blogListVO;
		}
		ArrayList<Integer> blogIdList = new ArrayList<>();
		for (CollectBlog blog : collectBlogList) {
			blogIdList.add(blog.getBlogId());
		}
		// 查询blog表，把之前获取的博客id列表传入，获取blog数据
		LambdaQueryWrapper<BlogBasic> blogWrapper = new LambdaQueryWrapper<>();
		blogWrapper.in(BlogBasic::getId, blogIdList);
		List<BlogBasic> blogBasicList = blogBasicMapper.selectList(blogWrapper);

		blogListVO.setRecords(blogBasicList);
		return blogListVO;
	}

}