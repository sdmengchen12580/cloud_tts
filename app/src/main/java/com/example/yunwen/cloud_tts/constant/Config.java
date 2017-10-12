package com.example.yunwen.cloud_tts.constant;


public class Config {
	//云知声
	public static final String appKey = "4qqq4qrxw6mvccxttl7fffuhh23atwubogdvcmyg";
	public static final String secret = "0f34fb0811bb9b884cde6317e7c8bad8";

	//主机地址（可变更）
	public static final String HOSTNAME = "http://robot.infinitus.com.cn/";
	//设置客户clientId
	public static final String CLIENDID = "5255525611";
	//设置客户sourceId
	public static final int SOURCEID = 9;
	//设置机器人问题类型
	public static final String QUESTION = "aq";
	//设置机器人appid
	public static final String APPID = "dERCzeW8x7FcuPeetU";
	//设置机器人sercet
	public static final String SECRET = "mGgZHdQ0OYF1CAFD5292";
	//设置机器人常在线类型
	public static final String ROBAT_ONLIONING = "kl";
	//设置机器人常用信息
	public static final String ROBAT_INFORMATION = "p";
	//设置机器人获取流程答案接口
	public static final String ROBAT_GWTFLOW = "getflw";
	//设置转人工
	public static final String ROBAT_TOPERSION = "needperson";
	//设置机器人离线类型
	public static final String ROBAT_NOONLION = "offline";

	//发送
	public static final int SENDTXT = 0x1;
	public static final int SENDIMAGE = 0x2;
	public static final int SENDVIDEO = 0x3;
	public static final int SENDAUDIO = 0x4;
	public static final int SENDMORE = 0x5;
	//接受
	public static final int FROMIMAGE = 0x6;
	public static final int FROMVIDEO = 0x7;
	public static final int FROMAUDIO = 0x8;
	public static final int FROMMORE = 0x9;
	public static final int FROMTXT = 0x10;
	public static final int FROMTXTMORE = 0x11;
}
