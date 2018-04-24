package root.report.file;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import root.configure.AppConstants;
import root.report.util.FileUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@RestController
@RequestMapping("/reportServer/report")
public class ReportControl {
	public static final String setupPath = "/file/";// "/WEB-INF/classes/report/";
	public static final String filePath = "/WEB-INF/classes/iReport/file/report/";
	@Autowired
	private AppConstants appConstant;
	// 递归使用
	private void showAllFiles(File dir, JSONArray aNode) {
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++) {

			if (fs[i].isHidden()) {
				continue;
			}
			JSONObject tNode = new JSONObject(true);
			tNode.put("name", fs[i].getName());
			tNode.put("path", fs[i].getPath());
			aNode.add(tNode);

			if (fs[i].isDirectory()) {

				JSONArray nNode = new JSONArray();

				tNode.put("children", nNode);
				showAllFiles(fs[i], nNode);
			}

		}

	}

	/**
	 * request需要传递 userCode：用户名 返回值filePath：文件路径树json格式
	 */
	
	@RequestMapping(value = "/getDirectory/{userCode}", produces = "text/plain; charset=utf-8")
	public @ResponseBody String getDirectory(@PathVariable("userCode") String userCode) {

		String serverPath = appConstant.getReportPath();

		File file = new File(serverPath+File.separator+userCode);
		JSONArray rootNode = new JSONArray();

		showAllFiles(file, rootNode);

		return rootNode.toJSONString();
	}

	/**
	 * request需要传递 userCode：用户名 filePath：文件路径
	 */


	
	@RequestMapping(value="/upload/{userCode}", produces = "text/plain; charset=utf-8")
	public String upload(@PathVariable("userCode") String userCode,HttpServletRequest request) {

		try {
			// 创建一个通用的多部分解析器
			CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
					request.getSession().getServletContext());
			// 判断 request 是否有文件上传,即多部分请求
			if (multipartResolver.isMultipart(request)) {
				// 转换成多部分request
				MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
				// 取得request中的所有文件名
				Iterator<String> iter = multiRequest.getFileNames();

				while (iter.hasNext()) {
					// 记录上传过程起始时的时间，用来计算上传时间
					int pre = (int) System.currentTimeMillis();
					// 取得上传文件
					MultipartFile file = multiRequest.getFile(iter.next());
					if (file != null) {
						// 取得当前上传文件的文件名称
						String myFileName = file.getOriginalFilename();
						// 如果名称不为“”,说明该文件存在，否则说明该文件不存在
						if (myFileName.trim() != "") {
							System.out.println(myFileName);
							// 重命名上传后的文件名配置路径+用户+用户路径+文件名
							String fileName = file.getOriginalFilename();
							// 定义上传路径
							// String userCode =
							// multiRequest.getParameter("userCode");
							String filePath = multiRequest.getParameter("filePath");
							String ServerPath = appConstant.getReportPath();

							String path = ServerPath +File.separator +userCode+File.separator+filePath;
							// 保存文件
							File localFile = new File(path);

							file.transferTo(localFile);
						}
					}

					// 记录上传该文件后的时间
					int finaltime = (int) System.currentTimeMillis();
					System.out.println(finaltime - pre);
				}

			}
			return "success";
			
		} catch (Exception e) {
			return  e.getMessage();
		}
		
	}

	// @RequestMapping("/downloadExcel/{userCode}/{filePath}")

	// public ResponseEntity<byte[]> downloadExcel(@PathVariable("userCode")
	// String userCode,
	// @PathVariable("filePath") String filePath) throws IOException {
	//
	@RequestMapping(value="/download/{userCode}", produces = "text/plain; charset=utf-8")
	public ResponseEntity<byte[]> download(@PathVariable("userCode") String userCode,HttpServletRequest req) throws IOException {

		String ServerPath = appConstant.getReportPath();

		String path = ServerPath +File.separator+userCode+File.separator+req.getParameter("filePath");

		File file = new File(path);
		HttpHeaders headers = new HttpHeaders();
		String fileName = new String(path.getBytes("UTF-8"), "iso-8859-1");// 为了解决中文名称乱码问题
		headers.setContentDispositionFormData("attachment", fileName);
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file), headers, HttpStatus.CREATED);
	}

	/**
	 * 
	 * fileDir 文件夹及其路径
	 * 
	 * filePath 文件默认路径 /WEB-INF/classes/iReport/file/template/
	 * 
	 * @param request
	 * @param pJson
	 * @return 0001:创建成功！ 0002:重名 0003:异常
	 * @throws IOException
	 */
	@RequestMapping(value="/mkDir/{userCode}", produces = "text/plain; charset=utf-8")
	public String mkDir(@PathVariable("userCode") String userCode,HttpServletRequest request, @RequestBody String pJson) throws IOException {
		String resultMsg = "0001";// 创建成功！
		try {
			// pJson = new String(pJson.getBytes("iso-8859-1"), "UTF-8");
			JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
			String fileDir = jsonObject.getString("fileDir");
			// String filePath = request.getSession().getServletContext()
			// .getRealPath(this.filePath);

			String ServerPath = appConstant.getReportPath();

			String filePath = ServerPath + File.separator +userCode+File.separator + fileDir;
			// if(!StringUtils.isEmpty(fileDir)){
			// filePath = filePath + fileDir+File.separator;
			// }

			File file = new File(filePath);
			if (file.exists()) { // 判断文件是否存在
				// 文件夹名重复，请重新命名
				resultMsg = "0002";// 重名
			} else {
				file.mkdirs();

			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMsg = "0003";// 异常
		}
		return resultMsg;

	}

	/**
	 * oldName 原文件夹或者文件名称 几路径 wangjian or wangjian/test or wangjian/test.txt
	 * newName 新名称 filePath 文件默认路径 /WEB-INF/classes/iReport/file/template/
	 * 
	 * @return
	 */
	
	@RequestMapping(value="/reName/{userCode}", produces = "text/plain; charset=utf-8")
	public String reName(@PathVariable("userCode") String userCode,HttpServletRequest request, @RequestBody String pJson) throws IOException {
		String resultMsg = "0001";// 重命名成功！
		try {
			// pJson = new String(pJson.getBytes("iso-8859-1"), "UTF-8");
			JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
			String oldName = jsonObject.getString("oldName");
			String newName = jsonObject.getString("newName");
			// String filePath = request.getSession().getServletContext()
			// .getRealPath(this.filePath);
			// String newFile = filePath + newName;
			//
			String ServerPath = appConstant.getReportPath();

			String newFile = ServerPath + File.separator +userCode+File.separator + newName;

			String oldFile = ServerPath + File.separator +userCode+File.separator + oldName;

			File file = new File(oldFile);
			if (file.exists()) { // 判断文件是否存在
				File newfile = new File(newFile);
				if (newfile.exists()) {
					// 文件名重复，请重新命名
					resultMsg = "0002";// 修改后名称已存在
				} else {
					boolean isOk = file.renameTo(newfile);
					if (!isOk) {
						resultMsg = "0005";// 修改失败！
					}
				}

			} else {
				resultMsg = "0003";// 原文件夹或者文件不存在
			}
		} catch (Exception e) {
			resultMsg = "0004";// 异常
			e.printStackTrace();
		}
		return resultMsg;
	}

	/**
	 * 
	 * filePath 文件默认路径 /WEB-INF/classes/iReport/file/template/ delName 文件夹或者文件名称
	 * 
	 * @return
	 */
	@RequestMapping(value="/deleteFile/{userCode}", produces = "text/plain; charset=utf-8")
	public String deleteFile(@PathVariable("userCode") String userCode,HttpServletRequest request, @RequestBody String pJson) throws IOException {
		String resultMsg = "0001";// 重命名成功！
		try {
			// pJson = new String(pJson.getBytes("iso-8859-1"), "UTF-8");
			JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
			String delName = jsonObject.getString("localPath");
			// String filePath = request.getSession().getServletContext()
			// .getRealPath(this.filePath);
			String ServerPath = appConstant.getReportPath();

			String filePath = ServerPath + File.separator +userCode+File.separator + delName;

			File file = new File(filePath);
			if (file.exists()) { // 判断文件是否存在
				// 文件夹名重复，请重新命名
				FileUtil.deleteDir(file);
			} else {
				// 文件夹名不存在
				resultMsg = "0002";
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMsg = "0003";
		}
		return resultMsg;
	}

}
