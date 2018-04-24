package root.report.query;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import root.report.util.XmlUtil;

public class SqlTemplate {
	
	String template = null;
	Document doc=null;
	Element select=null;
	JSONObject comment=null;
	
	
	private String namespace;
	private String id;
	private String db;
	private String selectType;
	private JSONArray in;
	private JSONArray out;
	private String sql;
	
	
	public  SqlTemplate(String templateFileName,String selectId) {
		try {
			//解析xml文件
			doc = XmlUtil.parseXmlToDom(templateFileName);
			//查找到Select节点
			select = (Element) doc.selectSingleNode("//select[@id='" + selectId + "']");
			//查找到说明comment节点
			String aJsonString = "";
			for (int j = 0; j < select.nodeCount(); j++) {
				Node node1 = select.node(j);
				if (node1.getNodeTypeName().equals("Comment")) {
					aJsonString = node1.getStringValue();
					break;
				}
			}
			comment=(JSONObject) JSONObject.parse(aJsonString);
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		this.id=selectId;
	}
	
	public String getNamespace() {
		if (namespace==null)
		{
			Element root = doc.getRootElement();
		    namespace= root.attributeValue("namespace");
		}
		return namespace;
	}
	public String getId() {
		return id;
	}
	public String getDb() {
		
		return  comment.getString("db");
		
	}
	public String getSelectType() {
		
		if (selectType==null)
		{
			String statementType = select.attributeValue("statementType");
			if (statementType == null) {
				selectType= "sql";
			} else if (statementType.equals("CALLABLE")) {
				selectType= "proc";
			}
		}
		
		return selectType;
	}
	public JSONArray getIn() {
		
       if (in==null)
    	 in= comment.getJSONArray("in");
		
		return in;
	}
	public JSONArray getOut() {

	   if(out==null)
		  out=comment.getJSONArray("out");
		
		return out;
	}


}
