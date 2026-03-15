package com.cl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import com.cl.service.NotificationService;
import com.cl.entity.JiuzhentongzhiEntity;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.cl.service.JiuzhentongzhiService;
import java.util.Date;
import java.util.List;

/**
 * 调度配置
 * 用于重试失败的通知
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    private JiuzhentongzhiService jiuzhentongzhiService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每5分钟检查并重试失败的通知
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void retryFailedNotifications() {
        // 查询失败且重试次数小于3的通知
        EntityWrapper<JiuzhentongzhiEntity> ew = new EntityWrapper<>();
        ew.eq("send_status", "failed").lt("retry_count", 3);
        List<JiuzhentongzhiEntity> failedLogs = jiuzhentongzhiService.selectList(ew);

        for (JiuzhentongzhiEntity log : failedLogs) {
            // 重试发送
            boolean success = attemptRetrySend(log);
            if (success) {
                log.setSendStatus("sent");
                log.setSendTime(new Date());
            } else {
                log.setRetryCount(log.getRetryCount() + 1);
            }
            jiuzhentongzhiService.updateById(log);
        }
    }

    private boolean attemptRetrySend(JiuzhentongzhiEntity log) {
        // 重试发送逻辑
        return notificationService.retrySend(log);
    }
}
