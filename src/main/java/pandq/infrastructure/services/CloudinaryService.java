package pandq.infrastructure.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for uploading images to Cloudinary.
 * Handles image upload and returns the public URL.
 */
@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.upload-preset:}")
    private String uploadPreset;

    private Cloudinary cloudinary;

    /**
     * Initialize Cloudinary instance (lazy initialization).
     */
    private Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
        }
        return cloudinary;
    }

    /**
     * Upload an image file to Cloudinary.
     *
     * @param file The multipart file to upload
     * @param folder The folder path in Cloudinary (e.g., "pandq/chat-images")
     * @return The public URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            // Use unsigned upload with upload preset (no API secret needed)
            return uploadImageUnsigned(file, folder);

        } catch (Exception e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to upload image to Cloudinary", e);
        }
    }

    /**
     * Upload image using unsigned upload (for Android client-side upload).
     * This requires upload preset to be configured.
     *
     * @param file The multipart file to upload
     * @param folder The folder path in Cloudinary
     * @return The public URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImageUnsigned(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (uploadPreset == null || uploadPreset.isEmpty()) {
            throw new IllegalStateException("Cloudinary upload preset is not configured");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = getCloudinary().uploader().unsignedUpload(
                    file.getBytes(),
                    uploadPreset,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            );

            String publicUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded image to Cloudinary (unsigned): {}", publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("Error uploading image to Cloudinary (unsigned)", e);
            throw new IOException("Failed to upload image to Cloudinary", e);
        }
    }

    /**
     * Delete an image from Cloudinary.
     *
     * @param publicId The public ID of the image to delete
     * @return true if deletion was successful
     */
    public boolean deleteImage(String publicId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = getCloudinary().api().deleteResources(
                    java.util.Collections.singletonList(publicId),
                    ObjectUtils.asMap()
            );
            log.info("Successfully deleted image from Cloudinary: {}", publicId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    /**
     * Get the Cloudinary configuration.
     *
     * @return Map with cloud_name, api_key, and upload_preset
     */
    public Map<String, String> getCloudinaryConfig() {
        return ObjectUtils.asMap(
                "cloud_name", cloudName,
                "upload_preset", uploadPreset != null ? uploadPreset : ""
        );
    }
}
