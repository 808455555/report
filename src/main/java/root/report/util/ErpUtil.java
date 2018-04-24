package root.report.util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

public class ErpUtil {
	// DES加密的私钥，必须是8位长的字符串
	// #des3加密key VI
	private static final byte[] DESkey = "11111111".getBytes();// 设置密钥

	private static final byte[] DESIV = "12345678".getBytes();// 设置向量

	static AlgorithmParameterSpec iv = null;// 加密算法的参数接口，IvParameterSpec是它的一个实现

	private static Key key = null;

	public ErpUtil() {
		try {
			DESKeySpec keySpec = new DESKeySpec(DESkey);// 设置密钥参数

			iv = new IvParameterSpec(DESIV);// 设置向量

			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");// 获得密钥工厂

			key = keyFactory.generateSecret(keySpec);// 得到密钥对象
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public ErpUtil(String desKey, String desIV) {
		try {
			DESKeySpec keySpec = new DESKeySpec(desKey.getBytes());// 设置密钥参数

			iv = new IvParameterSpec(desIV.getBytes());// 设置向量

			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");// 获得密钥工厂

			key = keyFactory.generateSecret(keySpec);// 得到密钥对象
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String GetPeriodName(String period) {

		String[] p = period.split("-");

		String pString = null;
		switch (p[1]) {
		case "1":
			pString = "JAN";
			break;
		case "2":
			pString = "FEB";
			break;

		case "3":
			pString = "MAR";
			break;

		case "4":
			pString = "APR";
			break;

		case "5":
			pString = "MAY";
			break;
		case "6":
			pString = "JUN";
			break;

		case "7":
			pString = "JUL";
			break;

		case "8":
			pString = "AUG";
			break;

		case "9":
			pString = "SEP";
			break;
		case "10":
			pString = "OCT";
			break;

		case "11":
			pString = "NOV";
			break;

		case "12":
			pString = "DEC";
			break;

		default:
			break;
		}
		return pString + "-" + p[0].substring(2, 4);

	}

	public String encode(String data) throws Exception {

		Cipher enCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");// 得到加密对象Cipher

		enCipher.init(Cipher.ENCRYPT_MODE, key, iv);// 设置工作模式为加密模式，给出密钥和向量

		byte[] pasByte = enCipher.doFinal(data.getBytes("utf-8"));

		BASE64Encoder base64Encoder = new BASE64Encoder();

		return base64Encoder.encode(pasByte);

	}

	public String decode(String data) throws Exception {

		Cipher deCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

		deCipher.init(Cipher.DECRYPT_MODE, key, iv);

		BASE64Decoder base64Decoder = new BASE64Decoder();

		byte[] pasByte = deCipher.doFinal(base64Decoder.decodeBuffer(data));

		return new String(pasByte, "UTF-8");

	}

	// 测试

	public static void main(String[] args) throws Exception {

		ErpUtil tools = new ErpUtil();

		System.out.println("加密:" + tools.encode("apps".toUpperCase()));

		System.out.println("解密:" + tools.decode("FpzGCwACXWs="));

	}
}
