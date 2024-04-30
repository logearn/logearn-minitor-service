package com.logearn.minitor.config;

import com.logearn.minitor.entitiy.AlarmMessage;
import com.logearn.minitor.util.FeiShuUtils;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Date;

@Import(RestTemplate.class)
public class FlyBookNotifier extends AbstractStatusChangeNotifier {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RestTemplate restTemplate;

    @Value("${spring.boot.admin.notify.flybook.webhook-url}")
    private String webhookUrl;


    public FlyBookNotifier(InstanceRepository repository, RestTemplate restTemplate) {
        super(repository);
        this.restTemplate = restTemplate;
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        if (StringUtil.isNullOrEmpty(webhookUrl)) {
            return Mono.error(new IllegalStateException("'webhookUrl' must not be null."));
        }
        return Mono
                .fromRunnable(() -> {
                    Object requestNotify = createMessage(event, instance);
                    String body = restTemplate.postForEntity(webhookUrl, requestNotify, String.class).getBody();
                    logger.info("instance name:{}, status:{}, do notify result: {}", instance.getRegistration().getName(), instance.getStatusInfo().getStatus(), body);
                });
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected Object createMessage(InstanceEvent event, Instance instance) {
        String message = null;
        if (instance.getStatusInfo().getStatus().equals("UP")) message = " -> 服务上线";
        else if (instance.getStatusInfo().getStatus().equals("OFFLINE")) message = " -> 服务宕机";
        else message = " -> 未知状态:" + instance.getStatusInfo().getStatus();
        AlarmMessage requestJson = AlarmMessage.builder()
                .messageSource("logearn 监控中心")
                .url("https://logearn.com/sb-admin")
                .confirmButtonUrl("https://logearn.com/sb-admin")
                .describe(instance.getRegistration().getName() + " 服务状态更改 " + getLastStatus(event.getInstance()) + " ➡️ " + instance.getStatusInfo().getStatus())
                .title(instance.getRegistration().getName() + message)
                .degree("PO")
                .occurrenceTime(new Date().toString()).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(FeiShuUtils.getWebHookMessage(requestJson), headers);
    }


}