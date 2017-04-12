package com.flyhtml.payment.channel.alipay.core;

import java.util.*;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.flyhtml.payment.channel.alibaba.model.enums.AlipayField;
import com.flyhtml.payment.channel.alibaba.model.enums.SignType;
import com.flyhtml.payment.channel.alipay.AlipayConfig;

import me.hao0.common.security.MD5;
import me.hao0.common.util.Strings;

/**
 * @author xiaowei
 * @time 17-3-29 上午10:03
 * @describe 支付宝工具类
 */
public class Alipay {

    private static AlipayClient alipayClient;

    static {
        alipayClient = new DefaultAlipayClient(AlipayConfig.URL, AlipayConfig.APPID, AlipayConfig.RSA2_PRIVATE_KEY,
                                               AlipayConfig.FORMAT, AlipayConfig.CHARSET,
                                               AlipayConfig.ALIPAY_RSA2_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
    }

    /***
     * @param subject 商品标题
     * @param body 商品描述
     * @param returnUrl 订单号
     * @param amount 订单总金额
     * @return
     */
    public static String createOrder(String subject, String body, String orderNo, String amount, String returnUrl,
                                     String notifyUrl) {
        try {
            AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();// 创建API对应的request
            // 在公共参数中设置回跳和通知地址
            alipayRequest.setReturnUrl(returnUrl);
            alipayRequest.setNotifyUrl(notifyUrl);
            // 封装请求支付信息
            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setSubject(subject);
            model.setBody(body);
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(amount);
            model.setTimeoutExpress(AlipayConfig.TIMEOUT_EXPRESS);
            model.setProductCode("QUICK_WAP_PAY");
            alipayRequest.setBizModel(model);
            String form = alipayClient.pageExecute(alipayRequest).getBody();
            return form;
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 对签名参数过滤处理，出去值为"", null, sign, signType
     *
     * @param signingParams 签名参数
     * @return 过滤后的签名参数
     */
    public static Map<String, String> filterSigningParams(Map<String, String> signingParams) {

        Map<String, String> validParams = new HashMap<>();

        for (Map.Entry<String, String> kv : signingParams.entrySet()) {
            if (Strings.isNullOrEmpty(kv.getValue()) || AlipayField.SIGN.field().equals(kv.getKey())
                || AlipayField.SIGN_TYPE.field().equals(kv.getKey())) {
                continue;
            }
            validParams.put(kv.getKey(), kv.getValue());
        }

        return validParams;
    }

    /**
     * 把请求参数中的key/value组装成用与号连接的请求字符串，按key的字母升序排序
     *
     * @param params 支付参数
     * @return key/value组装成用与号连接的请求字符串，按key的字母升序排序
     */
    public static String buildSignString(Map<String, String> params) {
        return buildSignString(params, "");
    }

    /**
     * 把请求参数中的key/value组装成用与号连接的请求字符串，按key的字母升序排序
     *
     * @param params 支付参数
     * @param wrapChar 值的包装字符，如APP支付需要加"
     * @return key/value组装成用与号连接的请求字符串，按key的字母升序排序
     */
    public static String buildSignString(Map<String, String> params, String wrapChar) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder payString = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {
                // 拼接时，不包括最后一个&字符
                payString.append(key).append("=").append(wrapChar).append(value).append(wrapChar);
            } else {
                payString.append(key).append("=").append(wrapChar).append(value).append(wrapChar).append("&");
            }
        }

        return payString.toString();
    }

    protected void putIfNotEmpty(Map<String, String> map, AlipayField field, String paramValue) {
        if (!Strings.isNullOrEmpty(paramValue)) {
            map.put(field.field(), paramValue);
        }
    }
}