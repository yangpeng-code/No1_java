package com.cl.dao;

import com.cl.entity.NotificationLogEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;

import org.apache.ibatis.annotations.Param;
import com.cl.entity.view.NotificationLogView;

/**
 * 通知日志
 * 数据层
 * @author 
 * @email 
 * @date 2025-03-27 15:44:15
 */
public interface NotificationLogDao extends BaseMapper<NotificationLogEntity> {
	
	List<NotificationLogView> selectListView(@Param("ew") Wrapper<NotificationLogEntity> wrapper);

	List<NotificationLogView> selectListView(Pagination page,@Param("ew") Wrapper<NotificationLogEntity> wrapper);
	
	NotificationLogView selectView(@Param("ew") Wrapper<NotificationLogEntity> wrapper);
	

}
