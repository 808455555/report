package pccw.wj.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class RO {
	

	private static SerializerFeature[] features = { SerializerFeature.WriteNullNumberAsZero,
			SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteMapNullValue,
			SerializerFeature.PrettyFormat, SerializerFeature.UseISO8601DateFormat,
			SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty };
	
	public  String SuccessMsg(String message,Object data)
	{
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("resultCode", "1000");
		jsonObject.put("message", message);
		jsonObject.put("data", data);
		return JSON.toJSONString(jsonObject,features);
	}
	public  String ErrorMsg(String resultCode,String message)
	{
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("resultCode", resultCode);
		jsonObject.put("message", message);
		return JSON.toJSONString(jsonObject,features);
	}
	public  String ExceptionMsg(String message)
	{
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("resultCode", "3000");
		jsonObject.put("message", message);
		return JSON.toJSONString(jsonObject,features);
	}
	
	

}
