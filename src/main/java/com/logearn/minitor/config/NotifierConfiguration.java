package com.logearn.minitor.config;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NotifierConfiguration {
    @Bean
    public FlyBookNotifier flyBookNotifier(InstanceRepository repository) {
        return new FlyBookNotifier(repository, new RestTemplate());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "spring.boot.admin.notify.jvm", name = "enabled", havingValue = "true")
    @ConfigurationProperties("spring.boot.admin.notify.jvm")
    @Lazy(false)
    public JvmAlarmNotifier jvmAlarm(InstanceRepository repository) {
        return new JvmAlarmNotifier(repository);
    }

}