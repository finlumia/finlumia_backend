package br.com.finlumia.docs.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class VideoConversionExecutorConfig {

    // core=1/max=2: a VPS tambem roda Postgres e os demais modulos no mesmo host.
    // ffmpeg -preset veryfast em 720p satura ~1 core por job, entao 2 jobs
    // simultaneos limita o pior caso de contencao de CPU numa rajada de uploads.
    @Bean(name = "videoConversionExecutor")
    public ThreadPoolTaskExecutor videoConversionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("video-conv-");
        // AbortPolicy (nao CallerRunsPolicy): rodar ffmpeg na thread da requisicao
        // HTTP anularia o proposito de nao pesar a aplicacao principal.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
