package org.mundell.mediaserver.service.eztv.downloader.configs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.mundell.dal.common.services.ConfigurationService;
import org.mundell.mediaserver.service.eztv.downloader.constraints.Constraints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
@ComponentScan(basePackages = "org.mundell")
public class MediaServerConfiguration {

    private final ConfigurationService configurationService;

    public String getSeriesDownloadLocation() {
        return configurationService.get(Constraints.SERIES_DOWNLOAD_LOCATION).getValue();
    }

    public String getSeriesLocation() {
        return configurationService.get(Constraints.SERIES_LOCATION).getValue();
    }

    @Bean
    public ObjectMapper configJackson() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
