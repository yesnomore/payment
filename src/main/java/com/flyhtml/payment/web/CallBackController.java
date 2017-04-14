package com.flyhtml.payment.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.flyhtml.payment.channel.wechatpay.WechatPayConfig;
import com.flyhtml.payment.channel.wechatpay.core.Wepay;
import com.flyhtml.payment.channel.wechatpay.core.WepayBuilder;
import com.google.common.base.Throwables;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alipay.api.AlipayApiException;
import com.flyhtml.payment.channel.alipay.core.AlipayUtil;
import com.flyhtml.payment.channel.alipay.enums.Validate;
import com.flyhtml.payment.channel.alipay.model.Notify;
import com.flyhtml.payment.common.util.Maps;
import com.flyhtml.payment.common.util.BeanUtils;
import com.flyhtml.payment.db.model.Pay;
import com.flyhtml.payment.db.model.PayHooks;
import com.flyhtml.payment.db.model.PayNotify;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.hao0.common.date.Dates;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xiaowei
 * @time 17-3-31 上午11:07
 * @describe 回调控制层
 */
@RestController
@RequestMapping("/notify")
public class CallBackController extends BaseController {

    @RequestMapping("/alipay/{id}")
    public String alipay(@PathVariable("id") String id) throws AlipayApiException, IOException {
        // 验签
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> paramMap = new HashMap<>();
        for (String key : parameterMap.keySet()) {
            paramMap.put(key, parameterMap.get(key)[0]);
        }
        Boolean signCheck = AlipayUtil.signCheck(paramMap);
        if (!signCheck) {
            return Validate.INVALID_SIGNATURE.getName();
        }
        // 验证订单准确性
        Pay pay = payService.selectById(id);
        Notify notify = BeanUtils.toObject(paramMap, Notify.class, true);
        Validate validate = AlipayUtil.notifyCheck(notify, pay);
        if (!validate.equals(Validate.SUCCESS)) {
            return validate.getName();
        }
        // 插入通知对象
        PayNotify payNotify = new PayNotify();
        payNotify.setNotifyParam(new Gson().toJson(parameterMap));
        payNotify.setNotifyUrl(request.getRequestURI());
        payNotify.setResponseData(validate.getName());
        payNotifyService.insertSelective(payNotify);
        // 更新支付对象为已支付状态
        Pay upPay = new Pay();
        upPay.setId(pay.getId());
        upPay.setIsPay(true);
        upPay.setPayTime(Dates.toDate(notify.getGmtPayment()));
        upPay.setChannelNo(notify.getTradeNo());
        payService.update(upPay);
        // 回调
        logger.debug("start payhooks....");
        String extra = pay.getExtra();
        Map<String, String> extraMap = new Gson().fromJson(extra, new TypeToken<Map<String, String>>() {
        }.getType());
        PayHooks hooks = new PayHooks();
        hooks.setId(pay.getId());
        hooks.setHooksUrl(extraMap.get("notifyUrl"));
        hooks.setHooksParam(new Gson().toJson(pay));
        hooks.setHooksTime(new Date());
        payHooksService.insertSelective(hooks);
        return validate.getName();
    }

    @RequestMapping("/wechat/{id}")
    public String wechat(@PathVariable("id") String id) throws IOException {
        // 验签
        Wepay wepay = WepayBuilder.newBuilder(WechatPayConfig.appid, WechatPayConfig.appTestKey,
                                              WechatPayConfig.mch_id).build();
        String notifyXml = getPostRequestBody(request);
        if (notifyXml.isEmpty()) {
            return wepay.notifies().notOk("body为空");
        }

        Map<String, Object> notifyParams = Maps.toMap(notifyXml);
        if (wepay.notifies().verifySign(notifyParams)) {

            // TODO business logic

            logger.info("verify sign success: {}", notifyParams);

            return wepay.notifies().ok();
        } else {

            logger.error("verify sign failed: {}", notifyParams);
            return wepay.notifies().notOk("签名失败");
        }
    }

    public static String getPostRequestBody(HttpServletRequest request) {
        if (request.getMethod().equals("POST")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = request.getReader()) {
                char[] charBuffer = new char[128];
                int bytesRead;
                while ((bytesRead = br.read(charBuffer)) != -1) {
                    sb.append(charBuffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.warn("failed to read request body, cause: {}", Throwables.getStackTraceAsString(e));
            }
            return sb.toString();
        }
        return "";
    }

    @RequestMapping("/pay/success")
    public String html() {
        return "支付成功!";
    }
}
