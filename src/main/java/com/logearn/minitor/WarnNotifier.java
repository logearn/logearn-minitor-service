package com.logearn.minitor;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class WarnNotifier extends AbstractStatusChangeNotifier {
    public WarnNotifier(InstanceRepository repository) {
        super(repository);
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        // 服务名
        String serviceName = instance.getRegistration().getName();
        // 服务url
        String serviceUrl = instance.getRegistration().getServiceUrl();
        // 服务状态
        String status = instance.getStatusInfo().getStatus();
        // 详情
        Map<String, Object> details = instance.getStatusInfo().getDetails();
        // 当前服务掉线时间
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = simpleDateFormat.format(date);
        // 拼接短信内容
        StringBuilder str = new StringBuilder();
        str.append("服务名：【" + serviceName + "】 \r\n");
        str.append("服务状态：【"+ status +"】 \r\n");
        str.append("地址：【" + serviceUrl + "】\r\n");
        str.append("时间：" + format +"\r\n");

        return Mono.fromRunnable(()->{
            // 这里写你服务发生改变时，要提醒的方法
            // 如服务掉线了，就发送短信告知
        });
    }
}
