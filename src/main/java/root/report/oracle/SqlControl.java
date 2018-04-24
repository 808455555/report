package root.report.oracle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.log4j.Logger;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultCDATA;
import org.dom4j.tree.DefaultComment;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import root.configure.AppConstants;
import root.report.common.RO;
import root.report.db.DbFactory;
import root.report.db.DbManager;
import root.report.query.SelectControl;
import root.report.util.XmlUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

@RestController
@RequestMapping("/reportServer/sql")
public class SqlControl extends RO{

    private static final Logger log = Logger.getLogger(SqlControl.class);
    @Autowired
    private AppConstants appConstant;
	private static SerializerFeature[] features = { SerializerFeature.WriteNullNumberAsZero,
            SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteMapNullValue,
            SerializerFeature.PrettyFormat, SerializerFeature.UseISO8601DateFormat,
            SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty };

	public static final String filePath = "/WEB-INF/classes/sql/mybatis.test.xml";// "/WEB-INF/sql/";
	public static final String headModel = "-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd";
	@Autowired
    private DbManager manager;
	
//	// 直接执行用户传入SQL
//	/**
//	 * id 唯一标识
//	 * sql select语句
//	 * @param pJson
//	 * @param request
//	 * @return
//	 */
	@RequestMapping(value = "/execIntroductionSQL", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String execIntroductionSQL(@RequestBody JSONObject pJson){
		List<Map<String, Object>> list = new ArrayList<>();
		try{
			String sql = pJson.getString("sql").toLowerCase();
			log.debug("测试SQL:"+sql);
			if(sql.contains("update")||sql.contains("insert")||sql.contains("drop")||sql.contains("delete")){
				return ExceptionMsg("SQL包含非法操作!");
			}
			String db = pJson.getString("db");
			Connection conn = new DbManager().getConnection(db);
			java.sql.Statement state = conn.createStatement();
			ResultSet rs = state.executeQuery(sql);
			ResultSetMetaData rsmd = rs.getMetaData();
			int cc = rsmd.getColumnCount();
			while (rs.next()) {
				Map<String, Object> retMap = new LinkedHashMap<String, Object>(cc);
				list.add(retMap);
				for (int i = 1; i <= cc; i++) {
					retMap.put(rsmd.getColumnLabel(i), rs.getObject(i));
				}
			}
		}catch(Exception e){
			Throwable cause = e;
			String message = null;
			while((message = cause.getMessage())==null){
				cause = cause.getCause();
			}
			ErrorMsg("3000", message);
		}
		return SuccessMsg("执行成功", list);
	}
	
	/**
	 * fileName 文件夹名称
	 * @param pJson
	 * @return 0001:创建成功！ 0002:重名 0003:异常
	 * @throws IOException
	 */
	@RequestMapping(value="/mkDir", produces = "text/plain; charset=utf-8")
	public String mkDir(@RequestBody JSONObject pJson) throws IOException
	{
		String resultMsg = "0001";//创建成功！
		try{
			String fileName = pJson.getString("fileName")+".xml";
			String category = pJson.getString("category");
			String filePath = GetSqlPath(category) + File.separator + fileName;
			File file = new File(filePath);
			if (file.exists()) { // 判断文件是否存在
				// 文件夹名重复，请重新命名
				resultMsg="0002";//重名
			} else {
				file.createNewFile();
			}
			
			//创建根元素
			Document doc = DocumentHelper.createDocument();
			Element mapper = DocumentHelper.createElement("mapper");
			mapper.addAttribute("namespace", pJson.getString("fileName"));
			doc.add(mapper);
			doc.addDocType("mapper", headModel, null);
			writeToXml(doc, file);
			
		} catch (Exception e) {
			e.printStackTrace();
			resultMsg="0003";//异常
		}
		return SuccessMsg("操作成功", resultMsg);
	}
	/**
	 * oldName 原文件夹名
	 * newName 新名称
	 * IOException 
	 * @return
	 */
	@RequestMapping(value="/reName", produces = "text/plain; charset=utf-8")
	public String reName(@RequestBody JSONObject pJson) throws IOException {
		String resultMsg = "0001";//重命名成功！
		try{
		String oldName = pJson.getString("oldName")+".xml";
		String newName = pJson.getString("newName")+".xml";
		String category = pJson.getString("category");
		String ServerPath = GetSqlPath(category);
		String newFile = ServerPath + "/" + newName;
		String oldFile = ServerPath + "/" + oldName;
		File file = new File(oldFile);
		if (file.exists()) { // 判断文件是否存在
			File newfile = new File(newFile);
			if (newfile.exists()) {
				// 文件名重复，请重新命名
				resultMsg="0002";//修改后名称已存在
			} else {
				boolean isOk = file.renameTo(newfile);
				Document doc = XmlUtil.parseXmlToDom(newfile);
				Element mapper = doc.getRootElement();
				mapper.addAttribute("namespace", newName);
				writeToXml(doc, newfile);
				List<String> dbs = new ArrayList<String>();
				for(Object ele:mapper.elements())
				{
					for(Object node:((Element)ele).content()) {
						if(((Node)node) instanceof DefaultComment){
							JSONObject obj = JSON.parseObject(((DefaultComment)node).getText());
							String db = obj.getString("db");
							if(!dbs.contains(db))
							{
								dbs.add(db);
								DbFactory.init(db);
							}
						}
					}
				}
				if(!isOk){
					resultMsg = "0005";//修改失败！
				}
			}
		} else {
			resultMsg="0003";//原文件夹或者文件不存在
		}
		}catch(Exception e){
			resultMsg="0004";//异常
			e.printStackTrace();
		}
		return SuccessMsg("操作成功", resultMsg);
	}
	
	/**
	 * 删除文件
	 * @return
	 */
	@RequestMapping(value="/deleteFile", produces = "text/plain; charset=utf-8")
	public String deleteFile(@RequestBody JSONObject pJson) throws IOException {
		String resultMsg = "0001";//删除成功！
		try {
			String delName = pJson.getString("localPath")+".xml";
			String category = pJson.getString("category");
			String filePath = GetSqlPath(category)+File.separator+delName;

			File file = new File(filePath);
			if (file.exists()) { // 判断文件是否存在
				// 文件夹名重复，请重新命名
				file.delete();
			} else {
				// 文件夹名不存在
				resultMsg="0002";
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultMsg="0003";
		}
		return SuccessMsg("操作成功", resultMsg);
	}
	
	private void writeToXml(Document doc, File file)
	{
		//写入XML文件
		OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        format.setTrimText(false);
        format.setIndent(false);
        XMLWriter writer = null;
        try
        {
			writer = new XMLWriter(new FileOutputStream(file),format);
			writer.write(doc);
			writer.flush();
			writer.close();
        }
        catch(Exception e)
        {
        	log.error("写入XML异常!"+file.getAbsolutePath());
        	e.printStackTrace();
        }
        finally
        {
        	if(writer!=null)
        	{
        		try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
	}
	
    /**
     * 新增用户定义报表SQL
     * 
     * @param pJson
     * @return String
     */
    @RequestMapping(value = "/saveUserSql", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String saveUserSql(@RequestBody String pJson)
    {
        JSONObject retObj = null;
        try
        {
            retObj = new JSONObject(true);
            JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
            String namespace = jsonObject.getString("namespace");
            String sqlType = jsonObject.getString("sqlType");
            JSONObject commonObj = jsonObject.getJSONObject("comment");
            String sqlId = jsonObject.getString("id");
            String cdata = jsonObject.getString("cdata");
            String category = jsonObject.getString("category");
            String userSqlPath = GetSqlPath(category) + File.separator + namespace + ".xml";
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            format.setTrimText(false);
            format.setIndent(false);
            XMLWriter writer = null;
            Document userDoc = XmlUtil.parseXmlToDom(userSqlPath);
            boolean checkResult = checkIsContainsSqlId(userDoc, sqlId);
            if(checkResult)
            {
                retObj.put("retCode", false);
                retObj.put("retMsg", "已经存在相同的报表ID");
                return retObj.toJSONString();
            }
            Element root = (Element)userDoc.selectSingleNode("/mapper");
            Element newSql = root.addElement("select");
            newSql.addAttribute("id", sqlId);
            if("SQL".equals(sqlType))
            {
                newSql.addAttribute("resultType", "Map");
            }
            else
            {
                newSql.addAttribute("statementType", "CALLABLE");
                //addResultMap(root);
            }
            newSql.addAttribute("parameterType", "Map");
            newSql.addComment(formatCommentJson(commonObj)+"\n");
            //newSql.addComment(JSONObject.toJSONString(jsonObject.getJSONObject("comment"), features)+"\n");
            //newSql.addCDATA(cdata);
            addSqlText(newSql,cdata);
            log.debug("新增SQL:"+newSql.asXML());
            writer = new XMLWriter(new FileOutputStream(userSqlPath),format);
            //删除空白行
            Element rootEle = userDoc.getRootElement();
            removeBlankNewLine(rootEle);
            writer.write(userDoc);
            writer.flush();
            writer.close();
            retObj.put("retCode", true);
            retObj.put("retMsg", "新增报表成功");
            //重置该DB连接
            DbFactory.init(commonObj.getString("db"));
        } 
        catch (Exception e)
        {
        	Throwable cause = e;
			String message = null;
			while((message = cause.getMessage())==null){
				cause = cause.getCause();
			}
			ErrorMsg("3000", message);
        }
        return SuccessMsg("操作成功", retObj);
    }
    
    private String formatCommentJson(JSONObject commentObj)
    {
        JSONObject obj = new JSONObject(true);
        if(commentObj.getString("db")!=null){
            obj.put("db", commentObj.getString("db"));
        }
        if(commentObj.getString("id")!=null){
            obj.put("id", commentObj.getString("id"));
        }
        if(commentObj.getString("name")!=null){
            obj.put("name", commentObj.getString("name"));
        }
        if(commentObj.getString("desc")!=null){
            obj.put("desc", commentObj.getString("desc"));
        }
        JSONArray inarray = commentObj.getJSONArray("in");
        if(inarray!=null)
        {
            JSONObject inobj = null;
            JSONArray in_array = new JSONArray();
            for (int i = 0; i < inarray.size(); i++)
            {
                inobj = new JSONObject(true);
                if(((JSONObject)inarray.get(i)).getString("id")!=null)
                {
                    inobj.put("id", ((JSONObject)inarray.get(i)).getString("id"));
                }
                if(((JSONObject)inarray.get(i)).getString("name")!=null)
                {
                    inobj.put("name", ((JSONObject)inarray.get(i)).getString("name"));
                }
                if(((JSONObject)inarray.get(i)).getString("datatype")!=null)
                {
                    inobj.put("datatype", ((JSONObject)inarray.get(i)).getString("datatype"));
                }
                if(((JSONObject)inarray.get(i)).getString("default")!=null)
                {
                    inobj.put("default", ((JSONObject)inarray.get(i)).getString("default"));
                }
                if(((JSONObject)inarray.get(i)).getString("lookup")!=null)
                {
                    inobj.put("lookup", ((JSONObject)inarray.get(i)).getString("lookup"));
                }
                if(((JSONObject)inarray.get(i)).getString("mut")!=null)
                {
                    inobj.put("mut", ((JSONObject)inarray.get(i)).getString("mut"));
                }
                if(((JSONObject)inarray.get(i)).getString("auth")!=null)
                {
                    inobj.put("auth", ((JSONObject)inarray.get(i)).getString("auth"));
                }
                in_array.add(inobj);
            }
            obj.put("in", in_array);
        }
        JSONArray outarray = commentObj.getJSONArray("out");
        if(outarray!=null)
        {
            JSONObject outobj = null;
            JSONArray out_array = new JSONArray();
            for (int i = 0; i < outarray.size(); i++)
            {
                outobj = new JSONObject(true);
                if(((JSONObject)outarray.get(i)).getString("id")!=null)
                {
                    outobj.put("id", ((JSONObject)outarray.get(i)).getString("id"));
                }
                if(((JSONObject)outarray.get(i)).getString("name")!=null)
                {
                    outobj.put("name", ((JSONObject)outarray.get(i)).getString("name"));
                }
                if(((JSONObject)outarray.get(i)).getString("datatype")!=null)
                {
                    outobj.put("datatype", ((JSONObject)outarray.get(i)).getString("datatype"));
                }
                if(((JSONObject)outarray.get(i)).getString("default")!=null)
                {
                    outobj.put("default", ((JSONObject)outarray.get(i)).getString("default"));
                }
                out_array.add(outobj);
            }
            obj.put("out", out_array);
        }
        return JSONObject.toJSONString(obj, features);
    }
    
    /**
     * 修改用户定义报表SQL
     * @return
     * @throws 
     */
    @RequestMapping(value = "/modifyUserSql", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String modifyUserSql(@RequestBody String pJson)
    {
        JSONObject retObj = null;
        try 
        {
            retObj = new JSONObject();
            JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
            String namespace = jsonObject.getString("namespace");
            String sqlType = jsonObject.getString("sqlType");
            JSONObject commonObj = jsonObject.getJSONObject("comment");
            String sqlId = jsonObject.getString("id");
            String cdata = jsonObject.getString("cdata");
            String category = jsonObject.getString("category");
            String userSqlPath = GetSqlPath(category) + File.separator + namespace + ".xml";
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            format.setTrimText(false);
            format.setIndent(false);
            XMLWriter writer = null;
            Document userDoc = XmlUtil.parseXmlToDom(userSqlPath);
            
            Element select = (Element)userDoc.selectSingleNode("//select[@id='"+sqlId+"']");
            List<Object> list = select.content();
            Object object = null;
            for (int i = list.size()-1; i >=0 ; i--) {
                object = list.get(i);
                if (object instanceof DefaultComment) {
                    select.remove((DefaultComment)object);
                }else if (object instanceof DefaultCDATA) {
                    select.remove((DefaultCDATA)object);
                }else if (object instanceof DefaultText) {
                    select.remove((DefaultText)object);
                }else if (object instanceof DefaultElement) {
                    select.remove((DefaultElement)object);
                } 
            }
            select.addComment(formatCommentJson(commonObj));
            addSqlText(select, cdata);
            log.debug("修改报表:"+select.asXML());
            writer = new XMLWriter(new FileOutputStream(userSqlPath), format);
            //删除空白行
            Element root = userDoc.getRootElement();
            removeBlankNewLine(root);
            writer.write(userDoc);
            writer.flush();
            writer.close();
            retObj.put("retCode", true);
            retObj.put("retMsg", "修改报表成功");
            
            DbFactory.init(commonObj.getString("db"));
        } 
        catch (Exception e)
        {
        	Throwable cause = e;
			String message = null;
			while((message = cause.getMessage())==null){
				cause = cause.getCause();
			}
			ErrorMsg("3000", message);
        }
        return SuccessMsg("操作成功", retObj);
    }
    /**
     * 添加Sql文本
     * @param select select节点
     * @param sqlText 查询Sql
     * @throws DocumentException 
     */
    private void addSqlText(Element select, String sqlText) throws DocumentException 
    { 
    	String text = replaceSepecialSymbol("<sql>"+sqlText+"\n</sql>");
    	Document doc = DocumentHelper.parseText(text);
    	//获取根节点    
    	Element root = doc.getRootElement();
    	List<Node> content = root.content();
    	for (int i = 0; i < content.size(); i++) {
    		Node node = content.get(i);
    		select.add((Node)node.clone());
		}
    }
    //替换大于小于号
    private String replaceSepecialSymbol(String xmlText){
    	StringBuilder text = new StringBuilder();
    	String sub_text = xmlText;
    	while(sub_text!=null&&!sub_text.equals("")){
    		int first_index = sub_text.indexOf(">");
    		text.append(sub_text.substring(0,first_index+1));
    		sub_text = sub_text.substring(first_index+1);
    		int second_index = getCharactorIndex(sub_text);
    		text.append(sub_text.substring(0,second_index).replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
	    	sub_text = sub_text.substring(second_index);
    	}
    	return text.toString();
    }
    //获取特定字符的下标
    private int getCharactorIndex(String str){
    	String[] charactor = new String[]{"<sql>","</sql>","<if","</if>","<choose","</choose>","<when","</when>",
    			"<otherwise","</otherwise>"};
    	int start_index = 0;
    	for(int i=0;i<charactor.length;i++){
    		int current_index = str.indexOf(charactor[i]);
    		if(current_index!=-1){
    			if(start_index==0){
    				start_index = current_index;
    			}else{
    	    		if( current_index<=start_index){
    	    			start_index = current_index;
    	    		}
        		}
    		}
    	}
    	return start_index;
    }
    
    private void removeBlankNewLine(Node node){
    	 List<Node> list = ((Element)node).content();
    	 boolean textOnly = true;
    	 if(node.getNodeType()==Node.ELEMENT_NODE){
    		 for(Node temp:list){
    			 if(temp.getNodeType()!=Node.TEXT_NODE){
    				 textOnly = false;
    				 break;
    			 }
    		 }
    	 }
    	 Node temp = null;
    	 int size = list.size();
         for(int i=0;i<size;i++){
        	 temp = list.get(i);
    		 int nodeType = temp.getNodeType();
	         switch (nodeType) {
	             case Node.ELEMENT_NODE:
	            	 removeBlankNewLine(temp);
	                 break;
	             case Node.CDATA_SECTION_NODE:
	            	 break;
	             case Node.COMMENT_NODE:
	            	 break;
	             case Node.TEXT_NODE:
	            	 Text text =  (Text)temp;
	            	 String value = text.getText();
	            	 //当前元素是否最后的文本
	            	 boolean isLastText = false;
	            	 if(i+1<size&&list.get(i+1).getNodeType()!=Node.TEXT_NODE){
	            		 isLastText = true; 
	            	 }
	            	 if(!value.trim().equals("")){
	            		if(value.startsWith("\n")){
	            			value = value.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "");
		            		value = "\n"+value;
	            		}else{
	            			value = value.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "");
	            		}
	            		//清空右边空白(和文本换行)
	            		if(!textOnly&&isLastText){
	            			value = value.substring(0,value.indexOf(value.trim().substring(0, 1))+value.trim().length());
	            		}
	            	 }else{
	            		 value = value.trim()+"\n"; 
	            	 }
	            	 text.setText(value);
	                 break;
	             default:break;
	         }
    	 }
    }
    
    //判断doc是否存在某个节点
    private boolean checkIsContainsSqlId(Document userDoc,String sqlId)
    {
        List<Element> list = userDoc.selectNodes("//select[@id='"+sqlId+"']");
        if(list==null||list.size()==0)
        {
            return false;
        }
        return true;
    }
    
    //移除某个节点
    private void moveSqlId(Document userDoc,String sqlId)
    {
        List<Element> list = userDoc.selectNodes("//select[@id='"+sqlId+"']");
        for (int i = 0; i < list.size(); i++)
        {
            list.get(i).getParent().remove(list.get(i));
        }
    }
   
    //删除报表
    @RequestMapping(value = "/moveUserSql", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String moveUserSql(@RequestBody String pJson)
    {
        JSONObject retObj = null;
        try 
        {
            retObj = new JSONObject(); 
            
            JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
            String namespace = jsonObject.getString("namespace");
            String sqlId = jsonObject.getString("id");
            String category = jsonObject.getString("category");
            String userSqlPath = GetSqlPath(category) + File.separator + namespace + ".xml";
            
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            format.setTrimText(false);
            format.setIndent(false);
            XMLWriter writer = null;
            Document userDoc = XmlUtil.parseXmlToDom(userSqlPath);
            
            //重置DB连接
            JSONObject newObj = new JSONObject();
            newObj.put("namespace", namespace);
            newObj.put("sqlid", sqlId);
            JSONObject selectObj = JSONObject.parseObject(new SelectControl().qrySelectSqlDetail(newObj.toJSONString()));
            DbFactory.init(selectObj.getJSONObject("comment").getString("db"));
            
            //删除该节点
            moveSqlId(userDoc,sqlId);
            
            log.debug("删除报表:命名空间【"+namespace+"】,报表ID【"+sqlId+"】");
            writer = new XMLWriter(new FileOutputStream(userSqlPath), format);
            //删除空白行
            Element root = userDoc.getRootElement();
            removeBlankNewLine(root);
            writer.write(userDoc);
            writer.flush();
            writer.close();
            retObj.put("retCode", true);
            retObj.put("retMsg", "删除报表成功");
        } 
        catch (Exception e)
        {
        	Throwable cause = e;
			String message = null;
			while((message = cause.getMessage())==null){
				cause = cause.getCause();
			}
			ErrorMsg("3000", message);
        }
        return SuccessMsg("操作成功", retObj);
    }
	
	// 根据SQL取参数 并转换为XML格式
	@RequestMapping(value = "/getInputOutputParas", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getInputOutputParas(@RequestBody String pJson) throws DocumentException 
	{
		JSONArray list = new JSONArray();
		try{
		    JSONObject jsonObject = (JSONObject) JSON.parse(pJson);
		    String content = jsonObject.getString("sql");
		    String sqlType = jsonObject.getString("sqlType");
		    
		    //去掉Mybatis的控制语句
		    String xmlText = replaceSepecialSymbol("<sql>"+content+"\n</sql>");
		    Document doc = DocumentHelper.parseText(xmlText);
		    
	    	StringBuilder sb = new StringBuilder();
	    	Element root = doc.getRootElement();
	    	String sql = getElementText(sb, root).toString().toLowerCase();
			log.debug("sql=====================" + sql);
			
			list = parseSqlOutputParams(sqlType,sql);
			list.addAll(parseSqlInputParams(sqlType, sql));
			JSONArray tempList = parseSqlInputParams(sqlType, root);
			for (int i = 0; i < tempList.size(); i++) 
			{
				if(!list.contains(tempList.get(i)))
				{
					list.add(tempList.get(i));
				}
			}
		}catch(Exception e){
			Throwable cause = e;
			String message = null;
			while((message = cause.getMessage())==null){
				cause = cause.getCause();
			}
			return ExceptionMsg(message);
		}
		return SuccessMsg("成功", list);
	}
	
	//递归获取元素文本内容
	private StringBuilder getElementText(StringBuilder sb, Node node)
	{
		if(node instanceof DefaultElement)
		{
			DefaultElement ele = (DefaultElement)node;
			List<Node> nodes = ele.content();
			for (int i = 0; i < nodes.size(); i++) {
				getElementText(sb, nodes.get(i));
			}
		}
		else if(node instanceof DefaultText||node instanceof DefaultCDATA)
		{
			sb.append(node.getText());
		}
		return sb;
	}
	//递归获取元素的属性test的所有参数
	private List<String> getElementAttributeText(List<String> list, Node node)
	{
		if(node instanceof DefaultElement)
		{
			DefaultElement ele = (DefaultElement)node;
			String attrValue = ele.attributeValue("test");
			if(attrValue!=null)
			{
				String[] attrs = attrValue.toLowerCase().split("\\s+[and|or]+\\s+");
				for(String attr:attrs)
				{
					if(!list.contains(attr))
					{
						list.add(attr.split("==|!=")[0].trim());
					}
				}
			}
			
			List<Node> nodes = ele.content();
			for (int i = 0; i < nodes.size(); i++) {
				getElementAttributeText(list, nodes.get(i));
			}
		}
		return list;
	}
	//从Mybatis控制语句中获取输入参数
	private JSONArray parseSqlInputParams(String sqlType,Element root)
	{
		JSONArray list = new JSONArray();
		if(!sqlType.toLowerCase().equals("sql"))
		{
			return list;
		}
		List<String> attrList = new ArrayList<String>();
		getElementAttributeText(attrList, root);
		JSONObject obj = null;
		for (String param:attrList) 
		{
			obj = new JSONObject();
			obj.put("id", param);
			obj.put("name", param);
			obj.put("datatype", "varchar");
			obj.put("type", "in");
			list.add(obj);
		}
	
		return list;
	}
	//根据输入Sql解析出输出参数
	private JSONArray parseSqlOutputParams(String sqlType,String sql) throws Exception
	{
		JSONArray list = new JSONArray();
		if(!sqlType.toLowerCase().equals("sql"))
		{
			return list;
		}
		String sqlFormat = sql.replace("#{", "").replace("}", "").replace("${", "");
		log.debug("sqlFormat=============" + sqlFormat);
		CCJSqlParserManager pm = new CCJSqlParserManager();
		
		Statement stmt = pm.parse(new StringReader(sqlFormat));
		if (stmt instanceof Select)
		{
		    PlainSelect pss = null;
		    SelectBody body = ((Select) stmt).getSelectBody();
		    if(body instanceof SetOperationList)
		    {
		        pss = (PlainSelect)((SetOperationList)body).getSelects().get(0);
		    }
		    else
		    {
		        pss = (PlainSelect) (((Select) stmt).getSelectBody());
		    }
			List<SelectItem> listps = pss.getSelectItems();
			Map<String,String> obj = null;
			SelectExpressionItem expresstionItem = null;
			Column column = null;
			Alias alias = null;
			String columnName = "";
			for (SelectItem item : listps)
			{
			    obj = new HashMap<String,String>();
			    expresstionItem = (SelectExpressionItem)item;
			    alias = expresstionItem.getAlias();
				if(alias==null)
				{
				    column =  (Column)(expresstionItem.getExpression());
				    columnName = column.getColumnName();
				}
				else
				{
				    columnName = alias.getName();
				}
				obj.put("id", columnName);
				obj.put("name", columnName);
				obj.put("datatype", "varchar");
				obj.put("type", "out");
				list.add(obj);
			}
		}
		return list;
	}
	//根据Sql解析输入参数
	private JSONArray parseSqlInputParams(String sqlType,String sql)
	{
		JSONArray list = new JSONArray();
		if (sqlType.toLowerCase().equals("sql"))
		{
			String[] condition = sql.split("}");
			int index = -1;
			Map<String,Boolean> paramMap = new HashMap<String,Boolean>();
			for (int i = 0; i < condition.length; i++)
			{
			    Map<String,String> obj = new HashMap<String,String>();
			    index = condition[i].indexOf("#{");
			    if(index==-1)
			    {
			        index = condition[i].indexOf("${");
			    }
			    String id = "";
				if (index != -1) {
				    id = condition[i].substring(index + 2);
				    if(paramMap.containsKey(id))
				    {
				        continue;
				    }
				    else
				    {
				        paramMap.put(id, true);
    				    obj.put("id", id);
    				    obj.put("name", id);
    				    obj.put("datatype", "varchar");
    				    obj.put("type", "in");
    					list.add(obj);
				    }
				}
			}
		} 
		else 
		{
			// String sql="{call CMCC_BALANCES_CHECK_TEST(#{p_period_year,
			// mode=IN,jdbcType=INTEGER},#{p_period_num,mode=IN,jdbcType=INTEGER}"
			// + ",#{p_balance_type,mode=OUT,jdbcType=INTEGER})}";
			String column = sql.substring(sql.indexOf("(") + 1,
					sql.indexOf(")"));
			String[] params = column.split("}");
			for (String string : params)
			{
				if (string.indexOf(",") == 0)
				{
					string = string.substring(3);
				}
				else
				{
					string = string.substring(2);
				}
				if(string.indexOf("#{")==-1)
				{
					continue;
				}
				String[] param = string.split(",");
				Map map = new HashMap();
				map.put("id", param[0].replace("#{","").trim());
				map.put("name", param[0].replace("#{","").trim());
				map.put("jdbctype", param[2].split("=")[1].trim());
				map.put("type", param[1].split("=")[1].trim());
				list.add(map);
			}
		}
		return list;
	}
	
    private String GetSqlPath(String category){
        if(category != null && category.equals("DataDictionary")){
            return  appConstant.getUserDictionaryPath();
        }else if (category != null && category.equals("function")){
        	return  appConstant.getUserFunctionPath();
        }else{
    		return appConstant.getUserSqlPath();
        }
    }

}
