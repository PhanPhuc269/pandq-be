package pandq.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pandq.adapter.web.api.dtos.SepayDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SePay Payment Service
 * Uses VietQR standard for bank transfer payments
 */
@Slf4j
@Service
public class SepayService {

    @Value("${SEPAY_API_TOKEN:}")
    private String apiToken;

    @Value("${SEPAY_BANK_ACCOUNT:}")
    private String bankAccount;

    @Value("${SEPAY_BANK_CODE:TPB}")
    private String bankCode;

    @Value("${SEPAY_ACCOUNT_NAME:}")
    private String accountName;

    // VietQR API for generating QR codes
    private static final String VIETQR_API = "https://img.vietqr.io/image";
    
    // SePay API base URL (for transaction queries)
    private static final String SEPAY_API = "https://my.sepay.vn/userapi";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Store pending transactions (in production, use database)
    private final Map<String, PendingTransaction> pendingTransactions = new HashMap<>();

    /**
     * Generate VietQR code for payment
     * Returns a QR code URL that user can scan with any banking app
     */
    public SepayDTO.CreateQRResponse createQRCode(SepayDTO.CreateQRRequest request) {
        try {
            // Generate unique payment code with PQ prefix (matches SePay config)
            String paymentCode = generatePaymentCode();
            
            // Payment content is just the code (PQ + numbers)
            // SePay will auto-detect codes starting with "PQ" prefix
            String content = paymentCode;
            
            // Build VietQR URL
            // Format: https://img.vietqr.io/image/BANK_CODE-ACCOUNT_NUMBER-TEMPLATE.png?amount=AMOUNT&addInfo=CONTENT&accountName=NAME
            String qrUrl = String.format(
                "%s/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                VIETQR_API,
                bankCode,
                bankAccount,
                request.getAmount(),
                URLEncoder.encode(content, StandardCharsets.UTF_8),
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
            );
            
            log.info("Generated VietQR URL: {}", qrUrl);
            log.info("Payment code: {}", paymentCode);
            
            // Store pending transaction for webhook matching
            PendingTransaction pending = new PendingTransaction();
            pending.paymentCode = paymentCode;
            pending.amount = request.getAmount();
            pending.orderId = request.getOrderId();
            pending.createdAt = System.currentTimeMillis();
            pendingTransactions.put(paymentCode, pending);
            
            // Build response
            SepayDTO.CreateQRResponse response = new SepayDTO.CreateQRResponse();
            response.setReturnCode(1);
            response.setReturnMessage("Success");
            response.setQrDataUrl(qrUrl);
            response.setQrCode(content);
            response.setTransactionId(paymentCode);
            response.setBankAccount(bankAccount);
            response.setBankCode(bankCode);
            response.setAccountName(accountName);
            response.setAmount(request.getAmount());
            response.setContent(content);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error creating SePay QR code", e);
            SepayDTO.CreateQRResponse response = new SepayDTO.CreateQRResponse();
            response.setReturnCode(0);
            response.setReturnMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Handle webhook from SePay when payment is received
     * Response format must be {"success": true} as per SePay requirements
     */
    public SepayDTO.WebhookResponse handleWebhook(SepayDTO.WebhookRequest request) {
        SepayDTO.WebhookResponse response = new SepayDTO.WebhookResponse();
        
        try {
            log.info("SePay webhook received: content={}, amount={}, code={}", 
                request.getContent(), request.getTransferAmount(), request.getCode());
            
            // Only process incoming transfers
            if (!"in".equals(request.getTransferType())) {
                response.setSuccess(true);
                response.setMessage("Ignored outgoing transfer");
                return response;
            }
            
            // Extract payment code from content - look for PQ prefix
            String paymentCode = request.getCode();
            if (paymentCode == null && request.getContent() != null) {
                // Try to extract from content - look for PQ prefix followed by numbers
                String content = request.getContent().toUpperCase();
                int pqIndex = content.indexOf("PQ");
                if (pqIndex >= 0) {
                    // Extract PQ followed by digits
                    StringBuilder code = new StringBuilder("PQ");
                    for (int i = pqIndex + 2; i < content.length() && Character.isDigit(content.charAt(i)); i++) {
                        code.append(content.charAt(i));
                    }
                    if (code.length() >= 12) { // PQ + at least 10 digits
                        paymentCode = code.toString();
                    }
                }
            }
            
            if (paymentCode != null) {
                PendingTransaction pending = pendingTransactions.get(paymentCode);
                if (pending != null) {
                    // Verify amount matches
                    if (pending.amount.equals(request.getTransferAmount())) {
                        log.info("Payment confirmed for code: {}, amount: {}", paymentCode, request.getTransferAmount());
                        pending.isPaid = true;
                        pending.paidAt = System.currentTimeMillis();
                        
                        // TODO: Update order status in database
                        // orderService.markAsPaid(pending.orderId);
                        
                        response.setSuccess(true);
                        response.setMessage("Payment confirmed");
                        return response;
                    } else {
                        log.warn("Amount mismatch for code: {}. Expected: {}, Got: {}", 
                            paymentCode, pending.amount, request.getTransferAmount());
                    }
                }
            }
            
            response.setSuccess(true);
            response.setMessage("Webhook processed");
            return response;
            
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            response.setSuccess(false);
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Check if a payment has been received for a transaction
     */
    public SepayDTO.TransactionQueryResponse checkPaymentStatus(String transactionId) {
        SepayDTO.TransactionQueryResponse response = new SepayDTO.TransactionQueryResponse();
        
        PendingTransaction pending = pendingTransactions.get(transactionId);
        if (pending == null) {
            response.setReturnCode(0);
            response.setReturnMessage("Transaction not found");
            response.setIsPaid(false);
            return response;
        }
        
        response.setReturnCode(1);
        response.setReturnMessage("Success");
        response.setIsPaid(pending.isPaid);
        response.setAmount(pending.amount);
        
        return response;
    }

    /**
     * Generate unique payment code for tracking
     * Format: PQ + 10 digit number (to match SePay config: prefix PQ, 10-12 digits)
     * Example: PQ2312230001
     */
    private String generatePaymentCode() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
        String timestamp = sdf.format(new Date()); // 10 digits: yyMMddHHmm
        return "PQ" + timestamp;
    }

    /**
     * Internal class to track pending transactions
     */
    private static class PendingTransaction {
        String paymentCode;
        Long amount;
        String orderId;
        long createdAt;
        boolean isPaid = false;
        long paidAt;
    }
}
