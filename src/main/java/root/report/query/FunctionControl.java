package root.report.query;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.apache.log4j.Logger;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultCDATA;
import org.dom4j.tree.DefaultComment;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import root.configure.AppConstants;
import root.report.common.RO;
import root.report.db.DbFactory;
import root.report.util.XmlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/reportServer/function")
public class FunctionControl extends RO{

	private static Logger log = Logger.getLogger(FunctionControl.class);
    @Autowired
    private AppConstants appConstant;
	@RequestMapping(value = "/getFunctionClass", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getFunctionClass() {
		String usersqlPath = appConstant.getUserFunctionPath();
		File file = new File(usersqlPath);
		File[] fileList = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".xml")) {
					return true;
				}
				return false;
			}
		});
		// 构造返回json
		List<Map> list = new ArrayList<Map>();

		for (int i = 0; i < fileList.length; i++) {

			JSONObject authNode = new JSONObject(true);
			String filename = fileList[i].getName();
			String name = filename.substring(0, filename.lastIndexOf("."));
			authNode.put("name", name);
			authNode.put("value", name);
            // 根据名称查找对应的模板文件
            String sqlPath = appConstant.getUserFunctionPath() + File.separator + name + ".xml";

            try {
                SAXReader sax = new SAXReader();
                sax.setValidation(false);
                sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);// 这一行是必须要有的
                // 获得dom4j的文档对象
                Document document = sax.read(new FileInputStream(sqlPath));
                Element root = document.getRootElement();
                // 得到database节点
                List<Element> selects = root.selectNodes("//select");

                // 构造返回json
                List<Map> childlist = new ArrayList<Map>();

                for (int j = 0; j < selects.size(); j++) {

                    Element element = selects.get(j);

                    //取出id
                    Map<String, String> childmap = new HashMap<String, String>();
                    childmap.put("name", element.attributeValue("id"));
                    childmap.put("value", name+"/"+element.attributeValue("id"));
                    childlist.add(childmap);
                }
                authNode.put("children", childlist);
            } catch (Exception e) {
                e.printStackTrace();
            }
			list.add(authNode);

		}
		return JSON.toJSONString(list);

	}

	// 取所有报表基本信息
	@RequestMapping(value = "/getFunctionName/{FunctionClass}", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getFunctionName(@PathVariable("FunctionClass") String selectClassName) {
		String result = "";
		// 根据名称查找对应的模板文件
		String usersqlPath = appConstant.getUserFunctionPath() + File.separator + selectClassName + ".xml";
		;

		try {
			SAXReader sax = new SAXReader();
			sax.setValidation(false);
			sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);// 这一行是必须要有的
			// 获得dom4j的文档对象
			Document document = sax.read(new FileInputStream(usersqlPath));
			Element root = document.getRootElement();
			// 得到database节点
			List<Element> selects = root.selectNodes("//select");

			// 构造返回json
			List<Map> list = new ArrayList<Map>();

			for (int i = 0; i < selects.size(); i++) {

				Element element = selects.get(i);

				// 取出id
				Map<String, String> map = new HashMap<String, String>();
				map.put("name", element.attributeValue("id"));
				// 取出数据查询类型
				String statementType = element.attributeValue("statementType");
				if (statementType == null) {
					map.put("type", "sql");
				} else if (statementType.equals("CALLABLE")) {
					map.put("type", "proc");
				}
				// 取出db和描述信息
				String aJsonString = "";
				for (int j = 0; j < element.nodeCount(); j++) {
					Node node1 = element.node(j);
					if (node1.getNodeTypeName().equals("Comment")) {
						aJsonString = node1.getStringValue();
						break;
					}
				}
				JSONObject jsonObject = (JSONObject) JSON.parse(aJsonString);
				map.put("db", jsonObject.getString("db"));
				map.put("desc", jsonObject.getString("desc"));

				list.add(map);
			}
			result = JSON.toJSONString(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	// 根据SQLID 取入参 出参信息
	@RequestMapping(value = "/getFunctionParam/{FunctionClassId}/{FunctionID}", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getFunctionParam(@PathVariable("FunctionClassId") String FunctionClassId,
			@PathVariable("FunctionID") String FunctionID) {
		String result = "";
		try {
			// 执行函数
			String usersqlPath = appConstant.getUserFunctionPath() + File.separator + FunctionClassId
					+ ".xml";
			SqlTemplate template = new SqlTemplate(usersqlPath, FunctionID);
			// 输入参数放入map中
			return template.comment.toJSONString();

		} catch (Exception ex) {
			ex.printStackTrace();

		}
		return result;
	}
	
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
            String userSqlPath = GetSqlPath(category,namespace);
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
                newSql.addAttribute("resultType", "BigDecimal");
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
            writer = new XMLWriter(new FileOutputStream(userSqlPath), format);
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
            log.error("新增报表失败.");
            e.printStackTrace();
            retObj.put("retCode", false);
            retObj.put("retMsg", "新增报表失败");
        }
        return retObj.toJSONString();
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
                if(((JSONObject)inarray.get(i)).getString("auth")!=null)
                {
                    inobj.put("auth", ((JSONObject)inarray.get(i)).getString("auth"));
                }
                if(((JSONObject)inarray.get(i)).getString("in_formula")!=null)
                {
                    inobj.put("in_formula", ((JSONObject)inarray.get(i)).getString("in_formula"));
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
            String userSqlPath = GetSqlPath(category,namespace);
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
            //select.addCDATA(cdata);
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
            log.error("修改报表失败.");
            e.printStackTrace();
            retObj.put("retCode", false);
            retObj.put("retMsg", "修改报表失败");
        }
        return retObj.toJSONString();
    }
	
	private void addSqlText(Element select, String sqlText) throws DocumentException 
    { 
    	String xmlText = "<sql>"+sqlText+"</sql>";
    	Document doc = DocumentHelper.parseText(xmlText);
    	//获取根节点    
    	Element root = doc.getRootElement();
    	List<Node> content = root.content();
    	for (int i = 0; i < content.size(); i++) {
    		Node node = content.get(i);
    		select.add((Node)node.clone());
		}
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
   	 for(Node temp:list){
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
	            	 if(!value.trim().equals("")){
	            		//清空右边空白
	            		value = value.substring(0,value.indexOf(value.trim().substring(0, 1))+value.trim().length());
	            		if(textOnly){
	            			value+="\n";
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

	@RequestMapping(value = "/execFunction/{FunctionClassName}/{FunctionID}", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String execFunction(@PathVariable("FunctionClassName") String FunctionClassName,
			@PathVariable("FunctionID") String FunctionID, @RequestBody String pJson) {

		System.out.println("开始执行查询:" + "selectClassName:" + FunctionClassName + "," + "selectID:" + FunctionID + ","
				+ "pJson:" + pJson + ",");
		long t1 = System.nanoTime();

		Object aResult = null;

		try {

			// 检查函数名是否存在
			
			//检查参数

			// 执行函数
			String usersqlPath = appConstant.getUserFunctionPath() + File.separator + FunctionClassName
					+ ".xml";
			SqlTemplate template = new SqlTemplate(usersqlPath, FunctionID);
			// 输入参数放入map中
			JSONArray inTemplate = template.getIn();
			JSONArray inValue = JSONArray.parseArray(pJson);

			Map<String,Object> map = new LinkedHashMap<String,Object>();
			Map<String,Boolean> dataParam = new HashMap<String,Boolean>();
			if (inTemplate != null) {
				for (int i = 0; i < inTemplate.size(); i++) {
					JSONObject aJsonObject = (JSONObject) inTemplate.get(i);
					String id = aJsonObject.getString("id");
					map.put(id, inValue.getString(i));
					Boolean inFormula = aJsonObject.getBoolean("in_formula");
					dataParam.put(id, inFormula);
				}
			}
			Map<String,Object> funcParamMap = new HashMap<String,Object>();
			List<FuncMetaData> list = new ArrayList<FuncMetaData>();
			acquireFuncMetaData(list,map,funcParamMap,dataParam);
			if(list.size()!=0){
				aResult = excuteFunc(list,0,funcParamMap,template);
			}else{
				if(template.getSelectType().equals("sql")) {
					String db = template.getDb();
					String namespace = template.getNamespace();
					String funcId = template.getId();
					aResult = DbFactory.Open(db).selectOne(namespace + "." + funcId, map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			aResult=e.getMessage();
		}

		long t2 = System.nanoTime();
		System.out.println("结束执行查询:" + "FunctionClassName:" + FunctionClassName + "," + "selectID:" + FunctionID + ","
				+ "pJson:" + pJson + ",\n" + "time:" + String.format("%.4fs", (t2 - t1) * 1e-9));
		return JSON.toJSONString(aResult, features);

	}
	
	private BigDecimal excuteFunc(List<FuncMetaData> list,int index,Map<String,Object> paramMap,SqlTemplate template) throws Exception{
		BigDecimal sum = null;
		int size = list.size();
		FuncMetaData meta = list.get(index);
		String[] paramVal = meta.getParamVal();
		String id = meta.getId();
		String expression = meta.getFuncExpression();
		for(String s:paramVal){
			paramMap.put(id, s);
			if(index<size-1){
				sum = excuteFunc(list,index+1,paramMap,template);
			}else{
				if(template.getSelectType().equals("sql")) {
					String db = template.getDb();
					String namespace = template.getNamespace();
					String funcId = template.getId();
					sum = DbFactory.Open(db).selectOne(namespace + "." + funcId, paramMap);
				}
			}
			expression = expression.replace(s, sum.toString());
		}
		Object result = null;
		try{
			Expression exp = AviatorEvaluator.compile(expression);
			result = exp.execute();
		}catch(Exception e){
			throw new Exception("参数表达式不合法");
		}
		return new BigDecimal(result.toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
	}
	//获取函数的元数据
	private void acquireFuncMetaData(List<FuncMetaData> list,Map<String,Object> map,Map<String,Object> funcParamMap,Map<String,Boolean> dateParam){
		Set<String> keys = map.keySet();
		for (String key:keys) {
			String value = (String) map.get(key);
			Boolean inFormula = dateParam.get(key);
			if(inFormula!=null&&inFormula){
				FuncMetaData meta = new FuncMetaData();
				meta.setId(key);
				meta.setFuncExpression(value);
				String[] arr = value.split("\\+|\\-|\\*|\\/|\\(|\\)");
				List<String> tempList = new ArrayList<String>();
				for(String temp:arr){
					if(temp!=null&&!temp.trim().equals("")){
						tempList.add(temp.trim());
					}
				}
				String[] paramVal = new String[tempList.size()];
				tempList.toArray(paramVal);
				meta.setParamVal(paramVal);
				list.add(meta);
			}else{
				funcParamMap.put(key, map.get(key));
			}
		}
	}
	
	private static SerializerFeature[] features = { SerializerFeature.WriteNullNumberAsZero,
			SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteMapNullValue,
			SerializerFeature.PrettyFormat, SerializerFeature.UseISO8601DateFormat,
			SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty };

	@RequestMapping(value = "/qryFunctionDetail", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String qryFunctionDetail(@RequestBody String pJson) {
		JSONObject obj = new JSONObject();
		try {
			JSONObject pObj = (JSONObject) JSON.parse(pJson);
			String namespace = pObj.getString("namespace");
			String sqlid = pObj.getString("sqlid");
			String category = pObj.getString("category");
			String sqlPath = GetSqlPath(category, namespace);
			Document doc = XmlUtil.parseXmlToDom(sqlPath);
			Element select = (Element) doc.selectSingleNode("/mapper/select[@id='" + sqlid + "']");
			obj.put("namespace", namespace);
			List<Object> list = select.content();
			Object object = null;
			DefaultComment selContent = null;
			DefaultCDATA selCdata = null;
			String text = "";
			for (int i = 0; i < list.size(); i++) {
				object = list.get(i);
				if (object instanceof DefaultComment) {
					selContent = (DefaultComment) object;
					obj.put("comment", JSON.parse(selContent.getText()));
				}else{
					text+=((Node)object).asXML();
				}
			}
			obj.put("cdata", text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj.toJSONString();
	}

	private String GetSqlPath(String category, String namespace) {
		if (category != null && category.equals("DataDictionary")) {
			return appConstant.getUserDictionaryPath() + File.separator + namespace + ".xml";
		} else if (category != null && category.equals("function")) {
			return appConstant.getUserFunctionPath() + File.separator + namespace + ".xml";
		} else {
			return appConstant.getUserSqlPath() + File.separator + namespace + ".xml";
		}
	}
	
	@RequestMapping(value = "/getAllFunctionClass", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getAllFunctionClass()
    {
        String usersqlPath = appConstant.getUserFunctionPath();
        File file = new File(usersqlPath);
        File[] fileList = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        });
        // 构造返回json
        List<Map> list = new ArrayList<Map>();

        for (int i = 0; i < fileList.length; i++) {

            Map<String, String> map = new HashMap<String, String>();
            String filename = fileList[i].getName();
            String name = filename.substring(0, filename.lastIndexOf("."));
            map.put("name", name);
            list.add(map);

        }
        return SuccessMsg("查询成功",list);  //return JSON.toJSONString(list);

    }
	@RequestMapping(value = "/getFunctionAuthList/{userName}", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String getFunctionAuthList(@PathVariable("userName") String userName) {
        try{
            Map<String,String> map = new HashMap<String,String>();
            map.put("userName",userName);
            int isAdmin =  DbFactory.Open(DbFactory.FORM).selectOne("user.isAdmin",userName);
            if(isAdmin == 1){
                return this.getAllFunctionClass();
            }else{
                List<Map> functionAuthList = DbFactory.Open(DbFactory.FORM).selectList("rule.getFunctionAuthList",map);
                return SuccessMsg("查询成功", functionAuthList);
            }
             
            
        }catch(Exception ex)
        {
            ex.printStackTrace();
            return ErrorMsg("3000", ex.getMessage());
        }
        
    }
    @RequestMapping(value = "/getFunctionAuthListByClass/{userName}/{className}", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String getFunctionAuthListByClass(@PathVariable("userName") String userName,@PathVariable("className") String className) {
        
        String result = "";
         try {
        Map<String,String> cmap = new HashMap<String,String>();
        cmap.put("userName",userName);
        cmap.put("className",className);
        int isAdmin =  DbFactory.Open(DbFactory.FORM).selectOne("user.isAdmin",userName);
        List<Map> functionAuthList = DbFactory.Open(DbFactory.FORM).selectList("rule.getFunctionAuthListByClass",cmap);
        for (Map functionAuth : functionAuthList) {
            String[] str = functionAuth.get("name").toString().split("/");
            functionAuth.put("name", str[str.length-1]);
        }
     // 根据名称查找对应的模板文件
        String usersqlPath = appConstant.getUserFunctionPath() + File.separator + className + ".xml";

       
            SAXReader sax = new SAXReader();
            sax.setValidation(false);
            sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);// 这一行是必须要有的
            // 获得dom4j的文档对象
            Document document = sax.read(new FileInputStream(usersqlPath));
            Element root = document.getRootElement();
            // 得到database节点
            List<Element> selects = root.selectNodes("//select");

            // 构造返回json
            List<Map> list = new ArrayList<Map>();

            for (int i = 0; i < selects.size(); i++) {

                Element element = selects.get(i);

                //取出id
                Map<String, String> map = new HashMap<String, String>();
                map.put("name", element.attributeValue("id"));
                //取出数据查询类型
                String statementType = element.attributeValue("statementType");
                if (statementType == null) {
                    map.put("type", "sql");
                } else if (statementType.equals("CALLABLE")) {
                    map.put("type", "proc");
                }
               //取出db和描述信息
                String aJsonString = "";
                for (int j = 0; j < element.nodeCount(); j++) {
                    Node node1 = element.node(j);
                    if (node1.getNodeTypeName().equals("Comment")) {
                        aJsonString = node1.getStringValue();
                        break;
                    }
                }
                JSONObject jsonObject = (JSONObject) JSON.parse(aJsonString);
                map.put("db", jsonObject.getString("db"));
                map.put("desc", jsonObject.getString("desc"));

                list.add(map);
            }
            if(isAdmin == 1){
                return SuccessMsg("查询成功",list);
            }else{
                List<Map> authMap = new ArrayList<Map>();
                for (Map auth : functionAuthList) {
                    for (Map l : list) {
                        if(l.get("name").equals(auth.get("name"))){
                            authMap.add(l);
                        }
                    }
                }
                return SuccessMsg("查询成功",authMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorMsg("3000", e.getMessage());
        }
      
    }

}
