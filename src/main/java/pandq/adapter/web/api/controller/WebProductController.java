package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pandq.adapter.web.api.dtos.ProductDTO;
import pandq.application.services.ProductService;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class WebProductController {

    private final ProductService productService;

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getProductDetailHtml(@PathVariable UUID id) {
        ProductDTO.Response product = productService.getProductById(id);
        
        String priceFormatted = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(product.getPrice());
        String imageUrl = product.getThumbnailUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = "https://via.placeholder.com/400x400?text=No+Image";
        }
        String description = product.getDescription() != null ? product.getDescription() : "Không có mô tả";
        String rating = String.format("%.1f", product.getAverageRating() != null ? product.getAverageRating() : 0.0);
        int reviewCount = product.getReviewCount() != null ? product.getReviewCount() : 0;

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>%s | PandQ Shop</title>
                <meta property="og:title" content="%s" />
                <meta property="og:description" content="%s" />
                <meta property="og:image" content="%s" />
                
                <!-- Font Inter -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

                <style>
                    :root {
                        --primary: #ec3713;
                        --text-main: #18181B;
                        --text-sec: #71717A;
                        --bg: #FFFFFF;
                        --divider: #E4E4E7;
                    }
                    body { font-family: 'Inter', sans-serif; margin: 0; padding: 0; background-color: #f4f4f5; color: var(--text-main); -webkit-font-smoothing: antialiased; padding-bottom: 80px; }
                    .container { max-width: 480px; margin: 0 auto; background: white; min-height: 100vh; position: relative; box-shadow: 0 0 20px rgba(0,0,0,0.05); }
                    
                    /* Header */
                    .header { position: sticky; top: 0; z-index: 10; background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); padding: 12px 16px; border-bottom: 1px solid var(--divider); display: flex; align-items: center; justify-content: center; }
                    .header-logo { font-weight: 700; color: var(--primary); font-size: 18px; }

                    /* Product Image */
                    .image-container { width: 100%%; aspect-ratio: 4/5; background: #f4f4f5; position: relative; overflow: hidden; }
                    img { width: 100%%; height: 100%%; object-fit: cover; }
                    
                    /* Content */
                    .content { padding: 20px; }
                    
                    h1 { font-size: 20px; font-weight: 700; margin: 0 0 8px 0; line-height: 1.4; color: var(--text-main); }
                    
                    .price-row { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; margin-bottom: 12px; }
                    .price { font-size: 24px; font-weight: 700; color: var(--primary); }
                    .status-badge { 
                        font-size: 12px; color: #16a34a; background: #dcfce7; 
                        padding: 4px 10px; border-radius: 99px; font-weight: 600; 
                        border: 1px solid rgba(34, 197, 94, 0.2);
                    }

                    /* Rating */
                    .rating-row { display: flex; align-items: center; margin-bottom: 24px; font-size: 14px; }
                    .star { color: #EAB308; margin-right: 4px; }
                    .rating-val { font-weight: 700; margin-right: 4px; }
                    .review-count { color: var(--text-sec); }
                    
                    /* Divider */
                    .hr { height: 8px; background: #f4f4f5; margin: 0 -20px 24px -20px; }

                    /* Description */
                    .section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
                    .description { font-size: 15px; line-height: 1.6; color: var(--text-sec); white-space: pre-wrap; }

                    /* Sticky Bottom Bar */
                    .bottom-bar {
                        position: fixed; bottom: 0; left: 0; right: 0;
                        background: white; padding: 12px 20px;
                        box-shadow: 0 -4px 12px rgba(0,0,0,0.05);
                        display: flex; justify-content: center;
                        border-top: 1px solid var(--divider);
                        max-width: 480px; margin: 0 auto;
                    }
                    .cta-btn { 
                        display: block; width: 100%%; padding: 16px; 
                        background: var(--primary); color: white; 
                        text-align: center; text-decoration: none; 
                        font-weight: 600; border-radius: 12px; font-size: 16px;
                        box-shadow: 0 4px 6px -1px rgba(236, 55, 19, 0.3);
                        transition: transform 0.2s;
                    }
                    .cta-btn:active { transform: scale(0.98); }
                    
                    /* Link Hint */
                    .app-hint { text-align: center; font-size: 12px; color: var(--text-sec); margin-bottom: 8px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <span class="header-logo">PandQ Shop</span>
                    </div>

                    <div class="image-container">
                        <img src="%s" alt="%s">
                    </div>
                    
                    <div class="content">
                        <h1>%s</h1>
                        
                        <div class="price-row">
                            <div class="price">%s đ</div>
                            <span class="status-badge">Còn hàng</span>
                        </div>
                        
                        <div class="rating-row">
                            <span class="star">★</span>
                            <span class="rating-val">%s</span>
                            <span class="review-count">(%d đánh giá)</span>
                        </div>

                        <div class="hr"></div>

                        <div class="section-title">Mô tả sản phẩm</div>
                        <div class="description">%s</div>
                    </div>
                    
                    <div class="bottom-bar">
                        <div style="width: 100%%">
                            <div class="app-hint">Trải nghiệm tốt nhất trên ứng dụng PandQ</div>
                            <a href="https://pandq.com/products/%s" class="cta-btn">Mở trong ứng dụng</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                product.getName(),
                product.getName(), description.substring(0, Math.min(description.length(), 100)), imageUrl,
                imageUrl, product.getName(),
                product.getName(),
                priceFormatted,
                rating, reviewCount,
                description,
                id
            );
    }
}
