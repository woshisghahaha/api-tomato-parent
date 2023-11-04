package com.tomato.project.job.cycle;

import com.tomato.apicommon.model.entity.InterfaceInfo;
import com.tomato.project.mapper.InterfaceInfoMapper;
import com.tomato.project.service.InterfaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时删除失效API
 *
 * @author Tomato
 */
// 取消下面注释开启任务
@Component
@Slf4j
public class DeleteExpireAPI {

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;
    @Resource
    private InterfaceInfoService interfaceInfoService;

    /**
     * 每10天执行一次
     */
    @Scheduled(cron = "0 0 0 */10 * ?")
    //@Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 查询近 10 天未更新的数据
        //Date thirtyDaysAgoDate = new Date(new Date().getTime() - 30 * 24 * 60 * 60 * 1000L);
        Date tenDaysAgoDate = new Date(new Date().getTime() - 10 * 24 * 60 * 60 * 1000L);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoMapper.listInterfaceInfoWithDelete(tenDaysAgoDate);
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            log.info("no Expire API");
            return;
        }
        final int pageSize = 500;
        int total = interfaceInfoList.size();
        log.info("DeleteExpireAPI start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("Delete from {} to {}", i, end);
            List<Long> ids = interfaceInfoList.stream().map(InterfaceInfo::getId).collect(Collectors.toList());
            interfaceInfoService.removeByIdsTranslator(ids.subList(i, end));
        }
        log.info("Delete end, total {}", total);
    }
}
