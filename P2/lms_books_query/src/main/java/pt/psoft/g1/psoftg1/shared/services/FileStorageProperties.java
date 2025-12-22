package pt.psoft.g1.psoftg1.shared.services;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * <p>
 * code based on https://github.com/callicoder/spring-boot-file-upload-download-rest-api-example
 *
 *
 */
@ConfigurationProperties(prefix = "file")
@Component
@Data
public class FileStorageProperties {
    @Getter
    private String uploadDir;

    @Getter
    private long photoMaxSize;

    public String getUploadDir() {
        return uploadDir;
    }

    public long getPhotoMaxSize() {
        return photoMaxSize;
    }
}
