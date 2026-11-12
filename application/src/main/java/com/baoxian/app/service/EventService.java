package com.baoxian.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoxian.infra.entity.EventLog;
import com.baoxian.infra.mapper.EventLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 事件流：所有业务动作留痕，前端 1.5 秒增量拉取 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventLogMapper eventLogMapper;

    public void log(String topic, String bizNo, String title, String status, String detail) {
        EventLog e = new EventLog();
        e.setTopic(topic);
        e.setBizNo(bizNo);
        e.setTitle(title);
        e.setStatus(status == null ? "OK" : status);
        e.setDetail(detail);
        eventLogMapper.insert(e);
    }

    public List<EventLog> recent(int limit) {
        return eventLogMapper.selectList(new LambdaQueryWrapper<EventLog>()
                .orderByDesc(EventLog::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
    }

    public void clear() {
        eventLogMapper.delete(null);
    }
}
