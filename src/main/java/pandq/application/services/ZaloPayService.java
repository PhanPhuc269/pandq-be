package pandq.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pandq.adapter.web.api.dtos.ZaloPayDTO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class ZaloPayService {

    @Value("${ZALOPAY_APP_ID:2554}")
    private int appId;

    @Value("${ZALOPAY_KEY1:sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn}")
    private String key1;

    @Value("${ZALOPAY_KEY2:trMrHtvjo6myautxDUiAcYsVtaeQ8nhf}")
    private String key2;

    private static final String SANDBOX_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String QUERY_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/query";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a ZaloPay payment order
     */
    public ZaloPayDTO.CreateOrderResponse createOrder(ZaloPayDTO.CreateOrderRequest request) {
        try {
            String appTransId = generateAppTransId();
            long appTime = System.currentTimeMillis();
            String appUser = "user123";
            
            // embed_data and item as JSON strings
            String embedData = "{}";
            String item = "[]";
            
            // Description
            String description = "PandQ - Thanh toan don hang #" + appTransId;
            
            // Create MAC signature
            // Format: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
            String macData = appId + "|" + appTransId + "|" + appUser + "|" + request.getAmount() + "|" + appTime + "|" + embedData + "|" + item;
            
            log.info("Using appId: {}", appId);
            log.info("MAC data string: {}", macData);
            String mac = hmacSHA256(key1, macData);
            log.info("Generated MAC: {}", mac);
            
            // Build form-urlencoded request body
            StringBuilder formBody = new StringBuilder();
            formBody.append("app_id=").append(appId);
            formBody.append("&app_user=").append(URLEncoder.encode(appUser, StandardCharsets.UTF_8));
            formBody.append("&app_trans_id=").append(URLEncoder.encode(appTransId, StandardCharsets.UTF_8));
            formBody.append("&app_time=").append(appTime);
            formBody.append("&amount=").append(request.getAmount());
            formBody.append("&description=").append(URLEncoder.encode(description, StandardCharsets.UTF_8));
            formBody.append("&embed_data=").append(URLEncoder.encode(embedData, StandardCharsets.UTF_8));
            formBody.append("&item=").append(URLEncoder.encode(item, StandardCharsets.UTF_8));
            formBody.append("&mac=").append(mac);
            
            log.info("Request body: {}", formBody);
            
            // Send request to ZaloPay using form-urlencoded
            String responseBody = sendFormRequest(SANDBOX_ENDPOINT, formBody.toString());
            log.info("ZaloPay CreateOrder response: {}", responseBody);
            
            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            ZaloPayDTO.CreateOrderResponse response = new ZaloPayDTO.CreateOrderResponse();
            response.setReturnCode((Integer) responseMap.get("return_code"));
            response.setReturnMessage((String) responseMap.get("return_message"));
            response.setZpTransToken((String) responseMap.get("zp_trans_token"));
            response.setOrderUrl((String) responseMap.get("order_url"));
            response.setAppTransId(appTransId);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error creating ZaloPay order", e);
            ZaloPayDTO.CreateOrderResponse response = new ZaloPayDTO.CreateOrderResponse();
            response.setReturnCode(-1);
            response.setReturnMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Query payment status
     */
    public ZaloPayDTO.QueryStatusResponse queryStatus(String appTransId) {
        try {
            String macData = appId + "|" + appTransId + "|" + key1;
            String mac = hmacSHA256(key1, macData);
            
            StringBuilder formBody = new StringBuilder();
            formBody.append("app_id=").append(appId);
            formBody.append("&app_trans_id=").append(URLEncoder.encode(appTransId, StandardCharsets.UTF_8));
            formBody.append("&mac=").append(mac);
            
            String responseBody = sendFormRequest(QUERY_ENDPOINT, formBody.toString());
            log.info("ZaloPay Query response: {}", responseBody);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            ZaloPayDTO.QueryStatusResponse response = new ZaloPayDTO.QueryStatusResponse();
            response.setReturnCode((Integer) responseMap.get("return_code"));
            response.setReturnMessage((String) responseMap.get("return_message"));
            response.setIsProcessing(responseMap.get("is_processing") != null && (Boolean) responseMap.get("is_processing"));
            response.setAmount(responseMap.get("amount") != null ? ((Number) responseMap.get("amount")).longValue() : 0L);
            response.setZpTransId((String) responseMap.get("zp_trans_id"));
            
            return response;
            
        } catch (Exception e) {
            log.error("Error querying ZaloPay status", e);
            ZaloPayDTO.QueryStatusResponse response = new ZaloPayDTO.QueryStatusResponse();
            response.setReturnCode(-1);
            response.setReturnMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Verify callback MAC using key2
     */
    public boolean verifyCallback(String data, String mac) {
        try {
            String computedMac = hmacSHA256(key2, data);
            return computedMac.equals(mac);
        } catch (Exception e) {
            log.error("Error verifying callback", e);
            return false;
        }
    }

    private String generateAppTransId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        String random = String.valueOf(System.currentTimeMillis() % 1000000);
        return date + "_" + random;
    }

    private String hmacSHA256(String key, String data) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String sendFormRequest(String url, String formBody) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));
            
            return httpClient.execute(httpPost, response -> {
                return EntityUtils.toString(response.getEntity());
            });
        }
    }
}
