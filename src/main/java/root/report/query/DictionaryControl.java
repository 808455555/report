package root.report.query;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultCDATA;
import org.dom4j.tree.DefaultComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;
import root.configure.AppConstants;
import root.report.common.RO;
import root.report.db.DbFactory;
import root.report.util.XmlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/reportServer/dictionary")
public class DictionaryControl extends RO {

	private static final Logger log = Logger.getLogger(SelectControl.class);

	public static final String headModel = "-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd";
	
	//数据库连接URL，通过使用TCP/IP的服务器模式（远程连接），当前连接的是内存里面的gacl数据库
    private  final String JDBC_URL = "jdbc:h2:tcp://localhost/mem:gacl";
    //连接数据库时使用的用户名
    private  final String USER = "sa";
    //连接数据库时使用的密码
    private  final String PASSWORD = "1234";
    //连接H2数据库时使用的驱动类，org.h2.Driver这个类是由H2数据库自己提供的，在H2数据库的jar包中可以找到
    private  final String DRIVER_CLASS="org.h2.Driver";
	@Autowired
	private AppConstants appConstant;
	@RequestMapping(value = "/h2", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String h2() throws ClassNotFoundException, SQLException {
            // 加载H2数据库驱动
            Class.forName(DRIVER_CLASS);
            // 根据连接URL，用户名，密码获取数据库连接
            Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            //如果存在USER_INFO表就先删除USER_INFO表
            stmt.execute("DROP TABLE IF EXISTS USER_INFO");
            //创建USER_INFO表
            stmt.execute("CREATE TABLE USER_INFO(id VARCHAR(36) PRIMARY KEY,name VARCHAR(100),sex VARCHAR(4))");
            //新增
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','大日如来','男')");
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','青龙','男')");
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','白虎','男')");
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','朱雀','女')");
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','玄武','男')");
            stmt.executeUpdate("INSERT INTO USER_INFO VALUES('" + UUID.randomUUID()+ "','苍狼','男')");
            //删除
            stmt.executeUpdate("DELETE FROM USER_INFO WHERE name='大日如来'");
            //修改
            stmt.executeUpdate("UPDATE USER_INFO SET name='孤傲苍狼' WHERE name='苍狼'");
            //查询
            ResultSet rs = stmt.executeQuery("SELECT * FROM USER_INFO");
            //遍历结果集
            while (rs.next()) {
                System.out.println(rs.getString("id") + "," + rs.getString("name")+ "," + rs.getString("sex"));
            }
            //释放资源
            stmt.close();
            //关闭连接
            conn.close();
		
		return "ok";

	}
	
	
	@RequestMapping(value = "/getDictionaryClass", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getDictionaryClass() {
		log.info("调用getDictionaryClass.");
		String usersqlPath = appConstant.getUserDictionaryPath();
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
		// ���췵��json
		List<Map> list = new ArrayList<Map>();

		for (int i = 0; i < fileList.length; i++) {

			Map<String, String> map = new HashMap<String, String>();
			String filename = fileList[i].getName();
			String name = filename.substring(0, filename.lastIndexOf("."));
			map.put("name", name);
			list.add(map);

		}
		return JSON.toJSONString(list);

	}

	@RequestMapping(value = "/getDictionaryName/{DictionaryClass}", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getDictionaryName(@PathVariable("DictionaryClass") String selectClassName) {
		String result = "";
		String usersqlPath = appConstant.getUserDictionaryPath() + File.separator + selectClassName + ".xml";
		;

		try {
			SAXReader sax = new SAXReader();
			sax.setValidation(false);
			sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);// ��һ���Ǳ���Ҫ�е�
			Document document = sax.read(new FileInputStream(usersqlPath));
			Element root = document.getRootElement();
			List<Element> selects = root.selectNodes("//select");

			List<Map> list = new ArrayList<Map>();

			for (int i = 0; i < selects.size(); i++) {

				Element element = selects.get(i);

				Map<String, String> map = new HashMap<String, String>();
				map.put("name", element.attributeValue("id"));
				String statementType = element.attributeValue("statementType");
				if (statementType == null) {
					map.put("type", "sql");
				} else if (statementType.equals("CALLABLE")) {
					map.put("type", "proc");
				}
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

	@RequestMapping(value = "/getDictionaryParam/{DictionaryClassId}/{DictionaryID}", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getDictionaryParam(@PathVariable("DictionaryClassId") String DictionaryClassId,
			@PathVariable("DictionaryID") String DictionaryID) {
		String result = "";
		String usersqlPath =appConstant.getUserDictionaryPath() + File.separator + DictionaryID + ".xml";
		;

		try {
			SAXReader sax = new SAXReader();
			sax.setValidation(false);
			sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);// ��һ���Ǳ���Ҫ�е�
			Document document = sax.read(new FileInputStream(usersqlPath));
			Element root = document.getRootElement();
			// List<Element> selects = root.selectNodes("//select");
			Element aSelect = (Element) root.selectSingleNode("//select[@id='" + DictionaryID + "']");

			String aJsonString = "";
			for (int j = 0; j < aSelect.nodeCount(); j++) {
				Node node1 = aSelect.node(j);
				if (node1.getNodeTypeName().equals("Comment")) {
					aJsonString = node1.getStringValue();
					break;
				}
			}

			JSONObject jsonObject = (JSONObject) JSON.parse(aJsonString);
			return jsonObject.toJSONString();
		} catch (Exception ex) {
			ex.printStackTrace();

		}
		return result;
	}
	
	@RequestMapping(value = "/execlDictionary/{DictionaryClassName}/{DictionaryID}", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String execlDictionary(@PathVariable("DictionaryClassName") String DictionaryClassName,
            @PathVariable("DictionaryID") String DictionaryID) {

        System.out.println("调用:" + "selectClassName:" + DictionaryClassName + "," + "selectID:" + DictionaryID);
        long t1 = System.nanoTime();

        List<Map> aResult = null;

        try {
            
            String usersqlPath = appConstant.getUserDictionaryPath()
                                  + File.separator + DictionaryClassName + ".xml";
            SqlTemplate template=new SqlTemplate(usersqlPath,DictionaryID);
            JSONArray inTemplate = template.getIn();
            
            
            Map map = new HashMap();
            if(inTemplate!=null)
            {
                for (int i = 0; i < inTemplate.size(); i++) {
                    JSONObject aJsonObject = (JSONObject) inTemplate.get(i);
                    map.put(aJsonObject.getString("id"), "");
                }
            }
            
            if (template.getSelectType().equals("sql")) {
                
                String db=template.getDb();
                String namespace=template.getNamespace();
                String id=template.getId();
                aResult = DbFactory.Open(db).selectList( namespace+"." +id, map);
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        long t2 = System.nanoTime();
        System.out.println("结束调用:" + "DictionaryClassName:" + DictionaryClassName + "," + "selectID:" + DictionaryID + ","
                + ",\n" + "time:" + String.format("%.4fs", (t2 - t1) * 1e-9));
        return JSON.toJSONString(aResult);

    }
	
	@RequestMapping(value = "/execlAppDictionary/{DictionaryClassName}/{DictionaryID}", produces = "text/plain;charset=UTF-8")
    public @ResponseBody String execlAppDictionary(@PathVariable("DictionaryClassName") String DictionaryClassName,
            @PathVariable("DictionaryID") String DictionaryID) {

        System.out.println("开始执行查询:" + "selectClassName:" + DictionaryClassName + "," + "selectID:" + DictionaryID);
        long t1 = System.nanoTime();

        List<Map> aResult = null;

        try {
            
            //检查函数参数个数
            
            //执行函数
            String usersqlPath = appConstant.getUserDictionaryPath()
                                  + File.separator + DictionaryClassName + ".xml";
            SqlTemplate template=new SqlTemplate(usersqlPath,DictionaryID);
            //输入参数放入map中
            JSONArray inTemplate = template.getIn();
            
            
            Map map = new HashMap();
            if(inTemplate!=null)
            {
                for (int i = 0; i < inTemplate.size(); i++) {
                    JSONObject aJsonObject = (JSONObject) inTemplate.get(i);
                    map.put(aJsonObject.getString("id"), "");
                }
            }
            
            if (template.getSelectType().equals("sql")) {
                
                String db=template.getDb();
                String namespace=template.getNamespace();
                String id=template.getId();
                aResult = DbFactory.Open(db).selectList( namespace+"." +id, map);
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        long t2 = System.nanoTime();
        System.out.println("结束执行查询:" + "DictionaryClassName:" + DictionaryClassName + "," + "selectID:" + DictionaryID + ","
                + ",\n" + "time:" + String.format("%.4fs", (t2 - t1) * 1e-9));
        return SuccessMsg("查询成功",aResult);  //return JSON.toJSONString(aResult);

    }
	
	
	private static SerializerFeature[] features = { SerializerFeature.WriteNullNumberAsZero,
			SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteMapNullValue,
			SerializerFeature.PrettyFormat, SerializerFeature.UseISO8601DateFormat,
			SerializerFeature.WriteDateUseDateFormat, SerializerFeature.WriteNullListAsEmpty };

	

	@RequestMapping(value = "/qryDictionaryDetail", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String qryDictionaryDetail(@RequestBody String pJson) {
		JSONObject obj = new JSONObject();
		try {
			JSONObject pObj = (JSONObject) JSON.parse(pJson);
			String namespace = pObj.getString("namespace");
			String sqlid = pObj.getString("sqlid");
			String sqlPath = appConstant.getUserDictionaryPath() + File.separator + namespace + ".xml";
			Document doc = XmlUtil.parseXmlToDom(sqlPath);
			Element select = (Element) doc.selectSingleNode("/mapper/select[@id='" + sqlid + "']");
			obj.put("namespace", namespace);
			List<Object> list = select.content();
			Object object = null;
			DefaultComment selContent = null;
			DefaultCDATA selCdata = null;
			for (int i = 0; i < list.size(); i++) {
				object = list.get(i);
				if (object instanceof DefaultComment) {
					selContent = (DefaultComment) object;
					obj.put("comment", JSON.parse(selContent.getText()));
				} else if (object instanceof DefaultCDATA) {
					selCdata = (DefaultCDATA) object;
					obj.put("cdata", selCdata.getText());
				}
			}
		} catch (Exception e) {
			log.error("查询数据字典异常");
			e.printStackTrace();
		}
		return obj.toJSONString();
	}
	
	@RequestMapping(value = "/execDictionarySql", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String execDictionarySql(@RequestBody String pJson) throws SAXException, DocumentException {
		JSONObject pObj = (JSONObject) JSON.parse(pJson);
		JSONObject obj = new JSONObject();
		String namespace = pObj.getString("namespace");
		String sqlid = pObj.getString("sqlid");
		long t1 = System.nanoTime();
		String sqlPath = appConstant.getUserDictionaryPath() + File.separator + namespace + ".xml";
		Document doc = XmlUtil.parseXmlToDom(sqlPath);
		Element select = (Element) doc.selectSingleNode("/mapper/select[@id='" + sqlid + "']");
		List<Object> list = select.content();
		Object object = null;
		DefaultComment selContent = null;
		DefaultCDATA selCdata = null;
		for (int i = 0; i < list.size(); i++) {
			object = list.get(i);
			if (object instanceof DefaultComment) {
				selContent = (DefaultComment) object;
				obj.put("comment", JSON.parse(selContent.getText()));
			} else if (object instanceof DefaultCDATA) {
				selCdata = (DefaultCDATA) object;
				obj.put("cdata", selCdata.getText());
			}
		}
		String sql = obj.getString("cdata");
		String comment = obj.getString("comment");
		JSONObject commentObj = (JSONObject) JSONObject.parse(comment);
		String db = commentObj.getString("db");
		Double aResult = null;
		List<Map> rList = new ArrayList<Map>();
		try {
			Map map = new HashMap();
			map.put("sqltest", sql);
			rList = DbFactory.Open(db).selectList("table.execIntroductionSQL", map);
			return JSON.toJSONString(rList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(rList);

	}
}
