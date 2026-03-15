package com.cl.service;

import org.springframework.stereotype.Service;
import com.cl.entity.JiuzhentongzhiEntity;
import com.cl.entity.YonghuEntity;
import com.cl.entity.YishengyuyueEntity;
import com.cl.service.JiuzhentongzhiService;
import com.cl.service.YonghuService;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

/**
 * 通知服务
 * @author 
 * @email 
 * @date 2025-03-27 15:44:15
 */
@Service
public class NotificationService {

    @Autowired
    private JiuzhentongzhiService jiuzhentongzhiService;

    @Autowired
    private YonghuService yonghuService;

    private static final int MAX_RETRIES = 3;

    /**
     * 发送通知
     * @param notificationId
     */
    @Async
    @Transactional
    public void sendNotification(Long notificationId) {
        JiuzhentongzhiEntity notification = jiuzhentongzhiService.selectById(notificationId);
        if (notification == null) {
            return;
        }

        // 检查用户是否接收通知
        YonghuEntity user = yonghuService.selectOne(new EntityWrapper<YonghuEntity>().eq("zhanghao", notification.getZhanghao()));
        if (user == null || !"是".equals(user.getIsReceiveNotification())) {
            // 不发送
            notification.setSendStatus("not_sent");
            jiuzhentongzhiService.updateById(notification);
            return;
        }

        int retryCount = notification.getRetryCount() != null ? notification.getRetryCount() : 0;
        boolean success = false;

        while (retryCount < MAX_RETRIES && !success) {
            try {
                // 模拟发送通知（这里可以集成邮件或短信API）
                success = sendToUser(notification);
                if (success) {
                    notification.setSendStatus("sent");
                    notification.setSendTime(new Date());
                } else {
                    retryCount++;
                    notification.setRetryCount(retryCount);
                }
            } catch (Exception e) {
                retryCount++;
                notification.setRetryCount(retryCount);
                notification.setErrorMessage(e.getMessage());
            }
        }

        if (!success) {
            notification.setSendStatus("failed");
        }

        jiuzhentongzhiService.updateById(notification);
    }

    /**
     * 模拟发送通知到用户
     * @param notification
     * @return
     */
    private boolean sendToUser(JiuzhentongzhiEntity notification) {
        // 这里实现实际的发送逻辑，例如调用邮件服务或短信API
        // 例如：使用JavaMail发送邮件，或调用第三方短信服务
        // 暂时返回true模拟成功
        System.out.println("Sending notification to " + notification.getShouji() + ": " + notification.getTongzhibeizhu());
        return true; // 模拟成功
    }

    /**
     * 发送预约提醒（一次性发送所有后续提醒）
     * @param appointment
     */
    @Async
    @Transactional
    public void sendAppointmentReminders(YishengyuyueEntity appointment) {
        // 检查用户是否接收通知
        YonghuEntity user = yonghuService.selectOne(new EntityWrapper<YonghuEntity>().eq("zhanghao", appointment.getZhanghao()));
        if (user == null || !"是".equals(user.getIsReceiveNotification())) {
            return; // 不发送
        }

        // 定义所有提醒内容
        List<String> reminders = Arrays.asList(
            "预约成功！您的预约ID: " + appointment.getId() + "，时间: " + appointment.getYuyueshijian(),
            "就诊提醒：请提前15分钟到达医院，带好身份证和医保卡。",
            "注意事项：如有发热症状，请提前联系医院。"
        );

        // 为每个提醒创建通知记录并尝试发送
        for (String content : reminders) {
            JiuzhentongzhiEntity log = new JiuzhentongzhiEntity();
            log.setTongzhibianhao("REMINDER_" + appointment.getId() + "_" + System.currentTimeMillis());
            log.setZhanghao(appointment.getZhanghao());
            log.setShouji(appointment.getShouji());
            log.setTongzhibeizhu(content);
            log.setSendStatus("retrying");
            log.setRetryCount(0);
            log.setAddtime(new Date());
            jiuzhentongzhiService.insert(log);

            boolean success = attemptSendReminder(log, content, appointment.getShouji());
            if (success) {
                log.setSendStatus("sent");
                log.setSendTime(new Date());
            } else {
                log.setSendStatus("failed");
            }
            jiuzhentongzhiService.updateById(log);
        }
    }

    private boolean attemptSendReminder(JiuzhentongzhiEntity log, String content, String phone) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                // 模拟发送通知（邮件或短信）
                System.out.println("Sending reminder to " + phone + ": " + content);
                // 这里可以集成邮件：mailSender.send(message);
                // 或短信API
                return true; // 模拟成功
            } catch (Exception e) {
                log.setRetryCount(i + 1);
                log.setErrorMessage(e.getMessage());
                jiuzhentongzhiService.updateById(log);
                try {
                    Thread.sleep((long) (5000 * Math.pow(2, i))); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }

    /**
     * 重试发送通知
     * @param log
     * @return
     */
    public boolean retrySend(JiuzhentongzhiEntity log) {
        return attemptSendReminder(log, log.getTongzhibeizhu(), log.getShouji());
    }

    /**
     * 发送预约提交通知
     * @param appointment
     */
    @Async
    @Transactional
    public void sendAppointmentSubmittedNotification(YishengyuyueEntity appointment) {
        // 检查用户是否接收通知
        YonghuEntity user = yonghuService.selectOne(new EntityWrapper<YonghuEntity>().eq("zhanghao", appointment.getZhanghao()));
        if (user == null || !"是".equals(user.getIsReceiveNotification())) {
            return; // 不发送
        }

        // 创建通知记录
        JiuzhentongzhiEntity log = new JiuzhentongzhiEntity();
        log.setTongzhibianhao("SUBMIT_" + appointment.getId() + "_" + System.currentTimeMillis());
        log.setZhanghao(appointment.getZhanghao());
        log.setShouji(appointment.getShouji());
        log.setTongzhibeizhu("您的预约已提交成功！预约ID: " + appointment.getId() + "，时间: " + appointment.getYuyueshijian() + "。请等待管理员审核。");
        log.setSendStatus("retrying");
        log.setRetryCount(0);
        log.setAddtime(new Date());
        jiuzhentongzhiService.insert(log);

        boolean success = attemptSendReminder(log, log.getTongzhibeizhu(), appointment.getShouji());
        if (success) {
            log.setSendStatus("sent");
            log.setSendTime(new Date());
        } else {
            log.setSendStatus("failed");
        }
        jiuzhentongzhiService.updateById(log);
    }
}
