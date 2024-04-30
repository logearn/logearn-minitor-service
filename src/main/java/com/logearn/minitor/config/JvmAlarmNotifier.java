package com.logearn.minitor.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.logearn.minitor.entitiy.AlarmMessage;
import com.logearn.minitor.util.FeiShuUtils;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JvmAlarmNotifier {

    @Value("${spring.boot.admin.notify.flybook.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private Scheduler scheduler;

    private Disposable subscription;

    private InstanceRepository repository;


    /**
     * jvm 阈值
     */
    private double threshold = 0.90;

    /**
     * 累计告警次数
     */
    private int alarmCountThreshold = 3;

    /**
     * 检测频率,秒
     */
    private long interval = 30;


    /**
     * 排除实例
     */
    private String excludeInstances = "";

    /**
     * 开关
     */
    private boolean enabled = true;

    /**
     * 提醒模版
     */
    private final String ALARM_TPL = "%s 服务 ｜检测间隔时间: %s | JVM 使用超阈值: %s,累计: %s次; " +
            "当前最大内存: %s , 已使用: %s ;" +
            "当前线程数: %s , JVM 进程 cpu 使用率: %s , 系统 cpu 使用率: %s";

    /**
     * 超过阈值次数
     */
    private final Map<String, Integer> instanceCount = new HashMap<>();

    public JvmAlarmNotifier(InstanceRepository repository) {
        this.repository = repository;
    }

    private void checkFn(Long aLong) {
        if (!enabled) {
            return;
        }
        log.debug("check jvm for all instances");
        repository.findAll().filter(instance -> !excludeInstances.contains(instance.getRegistration().getName())).map(instance -> {
            checkJVM(instance);
            return Mono.just(0d);
        }).subscribe();

    }

    private void checkJVM(Instance instance) {
        String instanceName = instance.getRegistration().getName();

        //最大堆空间
        BigDecimal jvmMax = getJvmValue(instance, "jvm.memory.max?tag=area:heap").divide(new BigDecimal(1024*1024d), 12, RoundingMode.DOWN);
        //已使用堆空间
        BigDecimal jvmUsed = getJvmValue(instance, "jvm.memory.used?tag=area:heap").divide(new BigDecimal(1024*1024d), 12, RoundingMode.DOWN);

        BigDecimal processCpuUsage = getJvmValue(instance, "process.cpu.usage") ;
        //已使用堆空间
        BigDecimal systemCpuUsage = getJvmValue(instance, "system.cpu.usage");
        boolean outMemory = jvmMax.compareTo(BigDecimal.ZERO) != 0 && jvmUsed.divide(jvmMax, 12, RoundingMode.DOWN).compareTo(new BigDecimal(threshold)) > 0;
        boolean outCpu = processCpuUsage.compareTo(new BigDecimal(threshold)) >= 0;
        if ( (outMemory || outCpu)
                && instanceCount.computeIfAbsent(instanceName, key -> 0) > alarmCountThreshold) {
            //当前活跃线程数
            BigDecimal threads = getJvmValue(instance, "jvm.threads.live");
            String content = String.format(ALARM_TPL, instanceName, interval, (threshold * 100) + "%", alarmCountThreshold,
                    jvmMax.toPlainString(), jvmUsed.toPlainString(),
                    threads.toPlainString(), processCpuUsage.multiply(new BigDecimal("100")).toPlainString(), systemCpuUsage.multiply(new BigDecimal("100")).toPlainString());
            AlarmMessage requestJson = AlarmMessage.builder()
                    .messageSource("logearn 监控中心")
                    .url("https://logearn.com/sb-admin")
                    .confirmButtonUrl("https://logearn.com/sb-admin")
                    .describe(content)
                    .title(instance.getRegistration().getName()+" - JVM 告警")
                    .degree("PO")
                    .occurrenceTime(new Date().toString()).build();
            Mono.fromRunnable(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String body = restTemplate.postForEntity(webhookUrl, new HttpEntity<>(FeiShuUtils.getWebHookMessage(requestJson), headers), String.class).getBody();
                log.info("instance name:{}, status:{}, do notify result: {}", instance.getRegistration().getName(), instance.getStatusInfo().getStatus(), body);
            }).block();
            //重新计算
            instanceCount.remove(instanceName);
        }
        //更新累计超过阈值次数
        if (outMemory || outCpu) {
            instanceCount.computeIfPresent(instanceName, (key, value) -> value + 1);
        }
    }

    private BigDecimal getJvmValue(Instance instance, String tags) {
        try {
            String reqUrl = instance.getRegistration().getManagementUrl() + "/metrics/" + tags;
            log.debug("check jvm {}, uri {}", instance.getRegistration().getName(), reqUrl);
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(reqUrl, String.class);
            String body = responseEntity.getBody();
            JSONObject bodyObject = JSON.parseObject(body);
            JSONArray measurementsArray = bodyObject.getJSONArray("measurements");
            if (measurementsArray != null && !measurementsArray.isEmpty()) {
                return new BigDecimal(""+measurementsArray.getJSONObject(0).get("value").toString());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public String getExcludeInstances() {
        return excludeInstances;
    }

    public void setExcludeInstances(String excludeInstances) {
        this.excludeInstances = excludeInstances;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getAlarmCountThreshold() {
        return alarmCountThreshold;
    }

    public void setAlarmCountThreshold(int alarmCountThreshold) {
        this.alarmCountThreshold = alarmCountThreshold;
    }

    private void start() {
        this.scheduler = Schedulers.newSingle("jvm-check");
        this.subscription = Flux.interval(Duration.ofSeconds(this.interval)).subscribeOn(this.scheduler).subscribe(this::checkFn);
    }

    private void stop() {
        if (this.subscription != null) {
            this.subscription.dispose();
            this.subscription = null;
        }
        if (this.scheduler != null) {
            this.scheduler.dispose();
            this.scheduler = null;
        }
    }
}
