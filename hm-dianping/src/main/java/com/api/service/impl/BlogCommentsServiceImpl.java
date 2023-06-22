package com.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.api.entity.BlogComments;
import com.api.mapper.BlogCommentsMapper;
import com.api.service.IBlogCommentsService;
import org.springframework.stereotype.Service;


@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
