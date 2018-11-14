package com.test;

import org.junit.jupiter.api.Test;

import com.kelvin.baidu.yuyin.YuYinUtil;
import com.kelvinylon.AuthService;
import com.kelvinylon.DailyTimeFullException;

class MyTest {

	@Test
	void test() throws DailyTimeFullException {
		String token = AuthService.getInstance().nextAuth((byte) 0);
		new YuYinUtil().generateYuYin(token, "宝宝你在干啥呀", "9", "4", null, null, "c:\\bbb.mp3");
	}

}
