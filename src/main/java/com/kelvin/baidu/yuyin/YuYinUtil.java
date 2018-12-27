package com.kelvin.baidu.yuyin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YuYinUtil {

	private static final Logger logger = LogManager.getLogger(YuYinUtil.class);

	public static final String YU_YIN_HE_CHENG_URL = "http://tsn.baidu.com/text2audio";

	/**
	 * 语音合成
	 * 
	 * @param token 百度token
	 * @param text  发音文本
	 * @param vol   音量 0-15
	 * @param per   发音人 0-普通女声、1-普通男声、3-度逍遥、4度丫丫
	 * @param spd   语速 0-15
	 * @param aue   格式 3为mp3格式(默认)； 4为pcm-16k；5为pcm-8k；6为wav（内容同pcm-16k）;
	 *              注意aue=4或者6是语音识别要求的格式，但是音频内容不是语音识别要求的自然人发音，所以识别效果会受影响。
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("deprecation")
	public void generateYuYin(String token, String text, String vol, String per, String spd, String pit, String aue,
			String outputFile) {

		logger.debug("调用语音合成接口");
		logger.debug("text:" + text);
		logger.debug("vol:" + vol);
		logger.debug("per:" + per);
		logger.debug("spd:" + spd);
		logger.debug("pit:" + pit);
		logger.debug("aue:" + aue);

		CloseableHttpClient httpClient = HttpClients.createMinimal();

		// 百度语音要求tex字段进行两次url encode，post实体中会进行一次encode
		String encodeText;
		try {
			encodeText = URLEncoder.encode(text, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
//		encodeText = URLEncoder.encode(encodeText, "utf-8");

//		String url = YU_YIN_HE_CHENG_URL + "?lan=zh&ctp=1&cuid=abcdxxx&tok=" + token + "&tex=" + encodeText
//				+ "&vol=9&per=0&spd=5&pit=5&aue=3";
//		url = URLEncoder.encode(url, "utf-8");

//		HttpGet get = new HttpGet(url);

		HttpPost post = new HttpPost(YU_YIN_HE_CHENG_URL);

		List<NameValuePair> params = new ArrayList<>();
		// 语言 目前只支持zh固定值
		params.add(new BasicNameValuePair("lan", "zh"));
		// 客户端类型选择 web端写1
		params.add(new BasicNameValuePair("ctp", "1"));
		// cuid 用户唯一标识，用来计算UV值。建议填写能区分用户的机器 MAC 地址或 IMEI 码，长度为60字符以内
		params.add(new BasicNameValuePair("cuid", "kelvinylon"));
		// token
		params.add(new BasicNameValuePair("tok", token));
		// text 语音文本
		params.add(new BasicNameValuePair("tex", encodeText));
		// 以下是选填项
		// vol 音量 0-15
		if (vol != null) {
			params.add(new BasicNameValuePair("vol", vol));
		}

		// per 发音人 0、1、3、4
		if (per != null) {
			params.add(new BasicNameValuePair("per", per));
		}

		// spd 语速 0-15
		if (spd != null) {
			params.add(new BasicNameValuePair("spd", spd));
		}
		
		// pit 语调
		if(pit != null) {
			params.add(new BasicNameValuePair("pit", pit));
		}

		// aue 3为mp3格式(默认)； 4为pcm-16k；5为pcm-8k；6为wav（内容同pcm-16k）;
		// 注意aue=4或者6是语音识别要求的格式，但是音频内容不是语音识别要求的自然人发音，所以识别效果会受影响。
		if (aue != null) {
			params.add(new BasicNameValuePair("aue", aue));
		}

		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(params);
		} catch (UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
		post.setEntity(entity);

		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000)
				.setSocketTimeout(20000).build();
//		get.setConfig(config);
		post.setConfig(config);
		try {
			CloseableHttpResponse resp = httpClient.execute(post);
			HttpEntity respEntity = resp.getEntity();
			String contentType = respEntity.getContentType().getValue();
			System.out.println(contentType);
			if (contentType.equals("application/json")) {
				throw new RuntimeException("错误：contentType为json，此请求的contentType应为音频文件");
			}
			InputStream contentIn = respEntity.getContent();
			File outFile = new File(outputFile);
			if(!outFile.getParentFile().exists()) {
				boolean b = outFile.getParentFile().mkdirs();
				if(!b) {
					throw new RuntimeException("创建父文件夹失败");
				}
			}
			
			FileOutputStream fout = new FileOutputStream(outputFile);
			try {
				IOUtils.copy(contentIn, fout);
			} finally {
				IOUtils.closeQuietly(contentIn);
				IOUtils.closeQuietly(fout);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void generateYuYin(String token, String text, String outputFile) {
		generateYuYin(token, text, null, null, null, null, null, outputFile);
	}

	/**
	 * 语音识别成文字
	 * 
	 * @param in
	 * @param token
	 * @return
	 */
	public String reconizeYuYin(InputStream in, String token) {
		return null;
	}

}
